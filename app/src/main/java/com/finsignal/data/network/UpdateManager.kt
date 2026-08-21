package com.finsignal.data.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val tagName: String,
    val releaseUrl: String,
    val releaseNotes: String
)

@Singleton
class UpdateManager @Inject constructor() {

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/nazimunaeem/FinSignal/releases/latest"
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name")
                val releaseUrl = json.getString("html_url")
                val body = json.optString("body", "")

                if (isNewerVersion(latestTag, currentVersion)) {
                    return@withContext UpdateInfo(latestTag, releaseUrl, body)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val latest = latestTag.removePrefix("v").substringBefore("-")
        val current = currentVersion.removePrefix("v").substringBefore("-")
        
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}
