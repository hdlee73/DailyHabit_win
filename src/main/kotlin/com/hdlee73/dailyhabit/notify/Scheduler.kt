package com.hdlee73.dailyhabit.notify

import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.CalendarRepository
import com.hdlee73.dailyhabit.data.GospelRepository
import com.hdlee73.dailyhabit.data.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 안드로이드의 AlarmManager·WorkManager 대신, 앱이 켜져 있는 동안(창을 닫아도 트레이에서) 30초마다
 * 아침 알림·루틴 알림·할 일 마감 알림·새 버전 확인(12시간마다)을 챙긴다.
 */
object Scheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fired = mutableSetOf<String>()
    private var lastUpdateNotice = 0L
    private var started = false

    /** 놓친 알림을 너무 늦게 보내지 않도록, 예약 시각에서 이만큼 지난 것까지만 보낸다 */
    private const val GRACE_MINUTES = 10L

    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (isActive) {
                runCatching { tick() }
                delay(30_000)
            }
        }
    }

    private suspend fun tick() {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val settings = AppSettings.default
        val db = AppDatabase.get()

        // 아침 알림: 하루 한 번. PC를 늦게 켰다면 켜자마자 보낸다
        val dailyAt = today.atTime(settings.dailyMinute / 60, settings.dailyMinute % 60)
        if (settings.dailyEnabled && !now.isBefore(dailyAt) && settings.dailySentEpochDay != today.toEpochDay()) {
            settings.dailySentEpochDay = today.toEpochDay()
            sendDaily()
        }

        // 루틴 알림: 오늘 할 루틴이고 아직 체크하지 않았을 때
        db.routineDao.active().filter { it.reminderEnabled && it.activeOn(today.dayOfWeek) }.forEach { r ->
            val at = today.atTime(r.reminderMinute / 60, r.reminderMinute % 60)
            val key = "r${r.id}@$today"
            if (due(now, at) && fired.add(key) && db.routineDao.isChecked(r.id, today.toEpochDay()) == 0) {
                Notifier.showRoutine(r)
            }
        }

        // 할 일 마감 알림
        db.todoDao.withReminders().forEach { t ->
            val at = LocalDate.ofEpochDay(t.dueEpochDay!!).atTime(t.dueMinute!! / 60, t.dueMinute % 60)
            if (due(now, at) && fired.add("t${t.id}@$at")) Notifier.showTodo(t)
        }

        // 새 버전: 12시간마다 확인하고, 있으면 설치할 때까지 알린다
        if (settings.autoUpdateCheck && System.currentTimeMillis() - lastUpdateNotice > 12 * 3600_000L) {
            lastUpdateNotice = System.currentTimeMillis()
            Updater.autoCheck()
            Updater.available?.takeIf { Updater.hasUpdate }?.let { Notifier.showUpdate(it.version) }
        }
    }

    private fun due(now: LocalDateTime, at: LocalDateTime) = !now.isBefore(at) && now.isBefore(at.plusMinutes(GRACE_MINUTES))

    /** 아침 알림을 지금 보낸다 (설정의 '지금 보내 보기'에서도 사용) */
    fun sendDailyNow() {
        scope.launch { sendDaily() }
    }

    private suspend fun sendDaily() {
        val today = LocalDate.now()
        val gospel = if (BuildConfig.CATHOLIC) GospelRepository().get(today).getOrNull() else null
        val calendar = CalendarRepository()
        val events = runCatching { calendar.groupByDay(calendar.events(today, 1), today, 1)[today].orEmpty() }
            .getOrDefault(emptyList())
        val routines = runCatching {
            AppDatabase.get().routineDao.active().filter { it.activeOn(today.dayOfWeek) }
        }.getOrDefault(emptyList())
        Notifier.showDaily(today, gospel, events, calendar.hasPermission(), routines)
    }
}
