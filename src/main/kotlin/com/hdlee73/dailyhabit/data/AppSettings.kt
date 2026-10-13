package com.hdlee73.dailyhabit.data

import org.json.JSONObject
import java.io.File

/**
 * 사용자 설정. 앱 데이터 폴더의 settings.json에 저장한다.
 * 안드로이드의 AppSettings와 같은 항목에 윈도우 전용 항목(트레이·자동 실행·캘린더 주소)을 더했다.
 */
class AppSettings private constructor() {
    private val file: File = File(AppDirs.root, "settings.json")
    private var json: JSONObject = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrDefault(JSONObject())

    @Synchronized
    private fun save() {
        runCatching { file.writeText(json.toString(2), Charsets.UTF_8) }
    }

    private fun put(key: String, v: Any?) {
        json.put(key, v)
        save()
    }

    var dailyEnabled: Boolean
        get() = json.optBoolean(KEY_DAILY_ENABLED, true)
        set(v) = put(KEY_DAILY_ENABLED, v)

    /** 아침 알림 시각 (자정부터 분) */
    var dailyMinute: Int
        get() = json.optInt(KEY_DAILY_MINUTE, 9 * 60)
        set(v) = put(KEY_DAILY_MINUTE, v)

    /** 아침 알림을 마지막으로 보낸 날 (epochDay) */
    var dailySentEpochDay: Long
        get() = json.optLong(KEY_DAILY_SENT, 0L)
        set(v) = put(KEY_DAILY_SENT, v)

    /** 천주교인용 기본 루틴(말씀 읽기·기도하기)을 이미 만들었는지 */
    var faithRoutinesSeeded: Boolean
        get() = json.optBoolean(KEY_FAITH_SEEDED, false)
        set(v) = put(KEY_FAITH_SEEDED, v)

    /** 루틴을 제목과 체크만 보이는 작은 카드로 보기 */
    var routineCompact: Boolean
        get() = json.optBoolean(KEY_ROUTINE_COMPACT, true)
        set(v) = put(KEY_ROUTINE_COMPACT, v)

    /** 그 해에 읽고 싶은 책 권수 (0이면 목표 없음) */
    fun readingGoal(year: Int): Int = json.optInt("reading_goal_$year", 0)

    fun setReadingGoal(year: Int, count: Int) = put("reading_goal_$year", count)

    /** 앱을 열 때 새 버전이 있는지 확인 */
    var autoUpdateCheck: Boolean
        get() = json.optBoolean(KEY_AUTO_UPDATE, true)
        set(v) = put(KEY_AUTO_UPDATE, v)

    var lastUpdateCheck: Long
        get() = json.optLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(v) = put(KEY_LAST_UPDATE_CHECK, v)

    var lastBackup: Long
        get() = json.optLong(KEY_LAST_BACKUP, 0L)
        set(v) = put(KEY_LAST_BACKUP, v)

    /** 장보기에 최근 넣은 품목 이름 (최근 것이 앞) */
    var shoppingHistory: List<String>
        get() = json.optString(KEY_SHOPPING_HISTORY, "").split('\n').filter { it.isNotBlank() }
        set(v) = put(KEY_SHOPPING_HISTORY, v.joinToString("\n"))

    // ── 윈도우 전용

    /** 창을 닫아도 알림 영역(트레이)에서 계속 실행해 알림을 보낸다 */
    var keepInTray: Boolean
        get() = json.optBoolean(KEY_KEEP_IN_TRAY, true)
        set(v) = put(KEY_KEEP_IN_TRAY, v)

    /** 구글 캘린더 '비공개 주소(iCal 형식)'. 여러 개면 줄바꿈으로 구분 */
    var calendarUrls: List<String>
        get() = json.optString(KEY_CALENDAR_URLS, "").split('\n').map { it.trim() }.filter { it.isNotBlank() }
        set(v) = put(KEY_CALENDAR_URLS, v.joinToString("\n"))

    companion object {
        private const val KEY_FAITH_SEEDED = "faith_routines_seeded"
        private const val KEY_ROUTINE_COMPACT = "routine_compact"
        private const val KEY_AUTO_UPDATE = "auto_update_check"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_SHOPPING_HISTORY = "shopping_history"
        private const val KEY_DAILY_ENABLED = "daily_enabled"
        private const val KEY_DAILY_MINUTE = "daily_minute"
        private const val KEY_DAILY_SENT = "daily_sent_epoch_day"
        private const val KEY_KEEP_IN_TRAY = "keep_in_tray"
        private const val KEY_CALENDAR_URLS = "calendar_urls"

        val default: AppSettings by lazy { AppSettings() }
    }
}
