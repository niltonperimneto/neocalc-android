package com.neocalc.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import uniffi.neocalc_backend.UpdateCheckResult
import uniffi.neocalc_backend.DownloadResult
import uniffi.neocalc_backend.UpdateProgressListener
import uniffi.neocalc_backend.checkForUpdates as rustCheckForUpdates
import uniffi.neocalc_backend.downloadApk as rustDownloadApk

/**
 * Update status sealed class for UI consumption.
 * Maps from Rust update results to Kotlin-friendly types.
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(
        val version: String,
        val downloadUrl: String,
        val checksum: String,
        val releaseNotes: String = ""
    ) : UpdateStatus()
    /** [progress] is 0-100, or -1 when the total size is unknown. */
    data class Downloading(val progress: Int, val version: String) : UpdateStatus()
    data class Downloaded(val apkFile: File, val version: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
    object ChecksumFailed : UpdateStatus()
}

/**
 * Update manager that delegates to Rust backend for network operations.
 * HTTP transport, signature/checksum verification, retries, and download
 * resume all happen in Rust; this layer adds coroutine dispatch, progress
 * mapping, and local file management.
 */
object UpdateManager {
    private const val TAG = "UpdateManager"

    /** Refuse to start a download with less free space than this. */
    private const val MIN_FREE_SPACE_BYTES = 64L * 1024 * 1024

    /** Serializes update operations so taps can't start concurrent checks/downloads. */
    private val updateMutex = Mutex()

    /**
     * Check for updates using Rust backend.
     * Automatically detects device ABI for architecture-specific APK downloads.
     */
    suspend fun checkForUpdates(currentVersion: String): UpdateStatus = updateMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                // Get primary device ABI for architecture-specific APK selection
                val deviceAbi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: ""

                when (val result = rustCheckForUpdates(currentVersion, deviceAbi)) {
                    is UpdateCheckResult.Available -> {
                        Log.d(TAG, "Update available: ${result.version} (ABI: $deviceAbi)")
                        UpdateStatus.Available(
                            version = result.version,
                            downloadUrl = result.downloadUrl,
                            checksum = result.checksum,
                            releaseNotes = result.releaseNotes
                        )
                    }
                    is UpdateCheckResult.UpToDate -> {
                        Log.d(TAG, "App is up to date")
                        UpdateStatus.UpToDate
                    }
                    is UpdateCheckResult.Error -> {
                        Log.e(TAG, "Update check error: ${result.message}")
                        UpdateStatus.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during update check", e)
                UpdateStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Download APK using Rust backend with mandatory checksum verification.
     * Interrupted downloads are resumed on retry; progress is reported from
     * the Rust download loop.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        expectedChecksum: String,
        onProgress: (Int) -> Unit
    ): UpdateStatus = updateMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val updatesDir = File(context.cacheDir, "updates")
                updatesDir.mkdirs()
                val outputFile = File(updatesDir, "neocalc-$version.apk")
                val partName = "${outputFile.name}.part"

                // A file that made it to its final name already passed
                // checksum verification; don't download it again.
                if (outputFile.isFile && outputFile.length() > 0) {
                    Log.d(TAG, "Reusing verified download: ${outputFile.path}")
                    return@withContext UpdateStatus.Downloaded(outputFile, version)
                }

                // Clean stale files from other versions, but keep this
                // version's partial download so Rust can resume it.
                updatesDir.listFiles()?.forEach { file ->
                    if (file.name != outputFile.name && file.name != partName) {
                        file.delete()
                    }
                }

                if (updatesDir.usableSpace < MIN_FREE_SPACE_BYTES) {
                    return@withContext UpdateStatus.Error("Not enough free space to download the update")
                }

                val mainHandler = Handler(Looper.getMainLooper())
                val listener = object : UpdateProgressListener {
                    // Called from the Rust download thread.
                    private var lastPercent = Int.MIN_VALUE
                    override fun onProgress(bytesDownloaded: ULong, totalBytes: ULong?) {
                        val percent = if (totalBytes != null && totalBytes > 0uL) {
                            (bytesDownloaded * 100uL / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1 // indeterminate
                        }
                        if (percent != lastPercent) {
                            lastPercent = percent
                            mainHandler.post { onProgress(percent) }
                        }
                    }
                }

                Log.d(TAG, "Starting download: $downloadUrl -> ${outputFile.path}")

                when (val result = rustDownloadApk(
                    downloadUrl,
                    outputFile.absolutePath,
                    expectedChecksum,
                    listener
                )) {
                    is DownloadResult.Success -> {
                        Log.d(TAG, "Download complete: ${result.filePath}")
                        UpdateStatus.Downloaded(File(result.filePath), version)
                    }
                    is DownloadResult.ChecksumFailed -> {
                        Log.e(TAG, "Checksum verification failed!")
                        Log.e(TAG, "Expected: ${result.expected}")
                        Log.e(TAG, "Actual: ${result.actual}")
                        UpdateStatus.ChecksumFailed
                    }
                    is DownloadResult.Error -> {
                        Log.e(TAG, "Download error: ${result.message}")
                        UpdateStatus.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during download", e)
                UpdateStatus.Error(e.message ?: "Download failed")
            }
        }
    }

    /**
     * Install the downloaded APK using system installer.
     * Uses FileProvider for secure file sharing (Android 7.0+).
     */
    fun installApk(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
