package com.neocalc.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import uniffi.neocalc_backend.UpdateCheckResult
import uniffi.neocalc_backend.DownloadResult
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
        val checksum: String?,
        val releaseNotes: String = ""
    ) : UpdateStatus()
    data class Downloading(val progress: Int, val version: String) : UpdateStatus()
    data class Downloaded(val apkFile: File, val version: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
    object ChecksumFailed : UpdateStatus()
}

/**
 * Update manager that delegates to Rust backend for network operations.
 * The actual HTTP requests and SHA-256 verification happen in Rust.
 */
object UpdateManager {
    private const val TAG = "UpdateManager"

    /**
     * Check for updates using Rust backend.
     * Automatically detects device ABI for architecture-specific APK downloads.
     */
    suspend fun checkForUpdates(currentVersion: String): UpdateStatus {
        return withContext(Dispatchers.IO) {
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
     * Download APK using Rust backend with checksum verification.
     * Progress reporting is not available with the current blocking Rust implementation.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        expectedChecksum: String?,
        onProgress: (Int) -> Unit
    ): UpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                // Prepare output directory
                val updatesDir = File(context.cacheDir, "updates")
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }

                // Clean old APKs
                updatesDir.listFiles()?.forEach { it.delete() }

                val outputPath = File(updatesDir, "neocalc-$version.apk").absolutePath
                
                // Notify UI that download is starting (progress will be indeterminate)
                withContext(Dispatchers.Main) {
                    onProgress(0)
                }

                Log.d(TAG, "Starting download: $downloadUrl -> $outputPath")
                
                when (val result = rustDownloadApk(downloadUrl, outputPath, expectedChecksum)) {
                    is DownloadResult.Success -> {
                        Log.d(TAG, "Download complete: ${result.filePath}")
                        withContext(Dispatchers.Main) {
                            onProgress(100)
                        }
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
