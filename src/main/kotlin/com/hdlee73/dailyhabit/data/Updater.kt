package com.hdlee73.dailyhabit.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hdlee73.dailyhabit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
)

/** GitHub 릴리스에서 내 판본(천주교인용/일반용)의 최신 설치 파일(.msi)을 찾아 내려받고 설치한다 */
object Updater {
    const val REPO = "hdlee73/DailyHabit_win"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"

    /** 새 버전을 찾았을 때 메뉴의 NEW 표시에 쓰는 마지막 확인 결과 */
    var available by mutableStateOf<UpdateInfo?>(null)

    val hasUpdate: Boolean get() = available?.let { isNewer(it.version, BuildConfig.VERSION_NAME) } == true

    /** "1.3.0", "1.3.0-dev.5" → 숫자 부분과 개발 빌드 여부 */
    private fun parse(v: String): Pair<List<Int>, Boolean> {
        val base = v.removePrefix("v").substringBefore('-')
        val nums = base.split('.').map { it.toIntOrNull() ?: 0 }
        return nums to v.contains("-dev")
    }

    fun isNewer(latest: String, current: String): Boolean {
        val (a, _) = parse(latest)
        val (b, currentIsDev) = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        // 같은 숫자면 개발 빌드보다 정식 릴리스가 새 것
        return currentIsDev
    }

    private fun open(url: String): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "DailyHabit-Windows")
        }

    /** 내 판본의 가장 높은 정식 릴리스. 네트워크 오류는 예외로 던진다 */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = open("https://api.github.com/repos/$REPO/releases?per_page=30")
        try {
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) throw java.io.IOException("서버 응답 ${conn.responseCode}")
            val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            var best: UpdateInfo? = null
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                if (r.optBoolean("draft") || r.optBoolean("prerelease")) continue
                val tag = r.optString("tag_name")
                if (tag.endsWith("-catholic") != BuildConfig.CATHOLIC) continue
                val version = tag.removePrefix("v").removeSuffix("-catholic")
                val assets = r.optJSONArray("assets") ?: continue
                val msi = pickInstaller(assets)
                if (msi == null) continue
                if (best == null || isNewer(version, best.version)) {
                    best = UpdateInfo(
                        version = version,
                        notes = r.optString("body").trim(),
                        apkUrl = msi.getString("browser_download_url"),
                        apkSize = msi.optLong("size"),
                        pageUrl = r.optString("html_url", RELEASES_PAGE),
                    )
                }
            }
            best
        } finally {
            conn.disconnect()
        }
    }

    /** 앱을 열 때와 12시간마다 확인한다 */
    suspend fun autoCheck() {
        val settings = AppSettings.default
        if (!settings.autoUpdateCheck) return
        runCatching { fetchLatest() }.onSuccess {
            settings.lastUpdateCheck = System.currentTimeMillis()
            available = it
        }
    }

    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(System.getProperty("java.io.tmpdir"), "DailyHabit-update").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "${if (BuildConfig.CATHOLIC) "DailyHabit-Catholic" else "DailyHabit"}-${info.version}.msi")
        val conn = open(info.apkUrl)
        try {
            if (conn.responseCode != 200) throw java.io.IOException("다운로드 실패 (${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            out
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 설치 파일을 실행한다. 설치하는 동안 파일을 바꿀 수 있도록 앱은 바로 종료되고,
     * 설치가 끝나면 앱을 다시 연다. 실행하지 못하면 false.
     */
    /** 이 PC의 CPU(x64 / ARM64)에 맞는 설치 파일(.msi)을 고른다. ARM64용이 없으면 x64용(에뮬레이션으로 실행됨) */
    internal fun pickInstaller(assets: org.json.JSONArray, arm64: Boolean = isArm64()): JSONObject? {
        val msis = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".msi", ignoreCase = true) }
        fun JSONObject.isArm() = optString("name").contains("arm64", ignoreCase = true)
        return (if (arm64) msis.firstOrNull { it.isArm() } else null) ?: msis.firstOrNull { !it.isArm() }
    }

    fun isArm64(): Boolean = System.getProperty("os.arch").orEmpty().lowercase().let { it == "aarch64" || it == "arm64" }

    fun install(msi: File): Boolean = runCatching {
        fun q(v: String) = "'" + v.replace("'", "''") + "'"
        val exe = System.getProperty("jpackage.app-path").orEmpty()
        val script = buildString {
            append("Start-Process -FilePath 'msiexec.exe' -Wait -ArgumentList ('/i \"' + ${q(msi.absolutePath)} + '\" /passive /norestart')\n")
            if (exe.isNotBlank()) append("if (Test-Path ${q(exe)}) { Start-Process -FilePath ${q(exe)} }\n")
        }
        // 따옴표·한글 경로 문제를 피하려고 스크립트를 인코딩해서 넘긴다
        val encoded = java.util.Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        ProcessBuilder("powershell.exe", "-NoProfile", "-WindowStyle", "Hidden", "-EncodedCommand", encoded).start()
        true
    }.getOrDefault(false)
}
