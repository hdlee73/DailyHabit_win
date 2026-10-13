package com.hdlee73.dailyhabit.notify

import com.hdlee73.dailyhabit.BuildConfig

/** Windows에 로그인하면 트레이에서 조용히 시작 (HKCU\…\Run 등록). 알림을 놓치지 않기 위한 안드로이드 '부팅 후 알림 재예약'의 대신 */
object Autostart {
    private const val KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private val name = if (BuildConfig.CATHOLIC) "DailyHabit-Catholic" else "DailyHabit"

    /** 설치된 앱(.exe)으로 실행 중일 때만 등록할 수 있다 */
    val exePath: String? get() = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }

    val available: Boolean get() = exePath != null && System.getProperty("os.name").orEmpty().startsWith("Windows")

    fun isEnabled(): Boolean = runCatching {
        val p = ProcessBuilder("reg", "query", KEY, "/v", name).redirectErrorStream(true).start()
        p.inputStream.readBytes()
        p.waitFor() == 0
    }.getOrDefault(false)

    fun setEnabled(on: Boolean): Boolean = runCatching {
        val exe = exePath ?: return false
        val cmd = if (on) listOf("reg", "add", KEY, "/v", name, "/t", "REG_SZ", "/d", "\"$exe\" --tray", "/f")
        else listOf("reg", "delete", KEY, "/v", name, "/f")
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        p.inputStream.readBytes()
        p.waitFor() == 0
    }.getOrDefault(false)
}
