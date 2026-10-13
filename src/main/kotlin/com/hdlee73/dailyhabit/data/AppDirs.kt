package com.hdlee73.dailyhabit.data

import com.hdlee73.dailyhabit.BuildConfig
import java.io.File

/**
 * 앱 데이터 폴더 (%APPDATA%\DailyHabit…). 판본마다 따로 둔다.
 * 천주교인용은 v1.0.0이 쓰던 폴더를 그대로 이어서 쓴다.
 */
object AppDirs {
    val root: File by lazy {
        // 테스트에서는 실제 기록을 건드리지 않도록 따로 지정한 폴더를 쓴다
        System.getProperty("dailyhabit.dataDir")?.let { return@lazy File(it).apply { mkdirs() } }
        val base = System.getenv("APPDATA") ?: System.getProperty("user.home")
        File(base, if (BuildConfig.CATHOLIC) "DailyHabit" else "DailyHabit-General").apply { mkdirs() }
    }
}
