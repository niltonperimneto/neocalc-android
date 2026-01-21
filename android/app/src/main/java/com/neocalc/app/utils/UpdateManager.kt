package com.neocalc.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(val version: String, val apkDownloadUrl: String) : UpdateStatus()
    data class Downloading(val progress: Int, val version: String) : UpdateStatus()
    data class Downloaded(val apkFile: File, val version: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object UpdateManager {
    private const val GITHUB_REPO = "niltonperimneto/neocalc-android"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdates(currentVersion: String): UpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // GitHub API user-agent requirement
                connection.setRequestProperty("User-Agent", "NeoCalc-Android-App")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode != 200) {
                    return@withContext UpdateStatus.Error("GitHub API code: ${connection.responseCode}")
                }

                val stream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val latestTag = json.getString("tag_name")
                
                // Find APK download URL from release assets
                val assets = json.getJSONArray("assets")
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                // Simple string comparison for now, assuming strict vYYYY.MM-R format
                if (latestTag != currentVersion && latestTag != "v$currentVersion") {
                    if (apkUrl != null) {
                        UpdateStatus.Available(latestTag, apkUrl)
                    } else {
                        // Fallback to html_url if no APK found
                        UpdateStatus.Error("No APK found in release")
                    }
                } else {
                    UpdateStatus.UpToDate
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateStatus.Error(e.message ?: "Unknown network error")
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        onProgress: (Int) -> Unit
    ): UpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "NeoCalc-Android-App")

                if (connection.responseCode != 200) {
                    return@withContext UpdateStatus.Error("Download failed: ${connection.responseCode}")
                }

                val contentLength = connection.contentLength
                val updatesDir = File(context.cacheDir, "updates")
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }

                // Clean old APKs
                updatesDir.listFiles()?.forEach { it.delete() }

                val apkFile = File(updatesDir, "neocalc-$version.apk")
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                UpdateStatus.Downloaded(apkFile, version)
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateStatus.Error("Download error: ${e.message}")
            }
        }
    }

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
