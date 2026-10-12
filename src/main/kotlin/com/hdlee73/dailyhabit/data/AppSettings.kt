package com.hdlee73.dailyhabit.data

import org.json.JSONObject
import java.io.File

/**
 * 데스크톱 사용자 설정.
 * %APPDATA%\DailyHabit\settings.json 에 저장됩니다.
 */
class AppSettings {
    private val file: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "DailyHabit").apply { mkdirs() }
        File(dir, "settings.json")
    }

    private var json: JSONObject = JSONObject()

    init {
        load()
    }

    private fun load() {
        if (file.exists()) {
            runCatching {
                json = JSONObject(file.readText(Charsets.UTF_8))
            }
        }
    }

    private fun save() {
        runCatching {
            file.writeText(json.toString(2), Charsets.UTF_8)
        }
    }

    /** 천주교인용 모드 여부 (기본값: true) */
    var isCatholic: Boolean
        get() = json.optBoolean(KEY_IS_CATHOLIC, true)
        set(v) { json.put(KEY_IS_CATHOLIC, v); save() }

    var dailyEnabled: Boolean
        get() = json.optBoolean(KEY_DAILY_ENABLED, true)
        set(v) { json.put(KEY_DAILY_ENABLED, v); save() }

    var dailyMinute: Int
        get() = json.optInt(KEY_DAILY_MINUTE, 9 * 60)
        set(v) { json.put(KEY_DAILY_MINUTE, v); save() }

    var faithRoutinesSeeded: Boolean
        get() = json.optBoolean(KEY_FAITH_SEEDED, false)
        set(v) { json.put(KEY_FAITH_SEEDED, v); save() }

    var routineCompact: Boolean
        get() = json.optBoolean(KEY_ROUTINE_COMPACT, true)
        set(v) { json.put(KEY_ROUTINE_COMPACT, v); save() }

    fun readingGoal(year: Int): Int = json.optInt("reading_goal_$year", 0)

    fun setReadingGoal(year: Int, count: Int) {
        json.put("reading_goal_$year", count)
        save()
    }

    var autoUpdateCheck: Boolean
        get() = json.optBoolean(KEY_AUTO_UPDATE, true)
        set(v) { json.put(KEY_AUTO_UPDATE, v); save() }

    var lastUpdateCheck: Long
        get() = json.optLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(v) { json.put(KEY_LAST_UPDATE_CHECK, v); save() }

    var lastBackup: Long
        get() = json.optLong(KEY_LAST_BACKUP, 0L)
        set(v) { json.put(KEY_LAST_BACKUP, v); save() }

    var shoppingHistory: List<String>
        get() = json.optString(KEY_SHOPPING_HISTORY, "").split('\n').filter { it.isNotBlank() }
        set(v) { json.put(KEY_SHOPPING_HISTORY, v.joinToString("\n")); save() }

    companion object {
        private const val KEY_IS_CATHOLIC = "is_catholic"
        private const val KEY_FAITH_SEEDED = "faith_routines_seeded"
        private const val KEY_ROUTINE_COMPACT = "routine_compact"
        private const val KEY_AUTO_UPDATE = "auto_update_check"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_SHOPPING_HISTORY = "shopping_history"
        private const val KEY_DAILY_ENABLED = "daily_enabled"
        private const val KEY_DAILY_MINUTE = "daily_minute"

        val default = AppSettings()
    }
}
