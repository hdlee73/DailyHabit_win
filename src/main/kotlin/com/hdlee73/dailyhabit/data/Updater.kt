package com.hdlee73.dailyhabit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

data class UpdateInfo(
    val version: String,
    val notes: String,
    val downloadUrl: String,
    val pageUrl: String,
)

object Updater {
    const val REPO = "hdlee73/DailyHabit_win"
    const val CURRENT_VERSION = "1.0.0"

    private fun parse(v: String): List<Int> =
        v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    fun isNewer(latest: String, current: String): Boolean {
        val a = parse(latest)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URI("https://api.github.com/repos/$REPO/releases?per_page=10").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "DailyHabit-Win")
            if (conn.responseCode != 200) return@runCatching null
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            if (arr.length() == 0) return@runCatching null
            val first = arr.getJSONObject(0)
            val tagName = first.optString("tag_name", "1.0.0")
            UpdateInfo(
                version = tagName.removePrefix("v"),
                notes = first.optString("body", "").trim(),
                downloadUrl = first.optString("html_url", ""),
                pageUrl = first.optString("html_url", ""),
            )
        }.getOrNull()
    }
}
