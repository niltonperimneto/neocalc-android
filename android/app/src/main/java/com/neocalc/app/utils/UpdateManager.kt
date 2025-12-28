package com.neocalc.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(val version: String, val downloadUrl: String) : UpdateStatus()
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
                val htmlUrl = json.getString("html_url") // Link to release page

                // Simple string comparison for now, assuming strict vYYYY.MM-R format
                if (latestTag != currentVersion && latestTag != "v$currentVersion") {
                    UpdateStatus.Available(latestTag, htmlUrl)
                } else {
                    UpdateStatus.UpToDate
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateStatus.Error(e.message ?: "Unknown network error")
            }
        }
    }
}
