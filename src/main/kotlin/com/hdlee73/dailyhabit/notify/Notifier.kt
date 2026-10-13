package com.hdlee73.dailyhabit.notify

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.data.CalendarEvent
import com.hdlee73.dailyhabit.data.Gospel
import com.hdlee73.dailyhabit.data.Routine
import com.hdlee73.dailyhabit.data.Todo
import com.hdlee73.dailyhabit.ui.Section
import com.hdlee73.dailyhabit.ui.formatEventTime
import com.hdlee73.dailyhabit.ui.formatKoreanDate
import com.hdlee73.dailyhabit.ui.formatMinute
import java.time.LocalDate

/**
 * 윈도우 알림(작업 표시줄 오른쪽 알림 영역의 토스트).
 * 안드로이드 알림과 같은 내용을 보내고, 알림을 누르거나 트레이 아이콘으로 창을 열면 해당 화면이 열린다.
 */
object Notifier {
    var tray: TrayState? = null

    /** 마지막 알림이 가리키는 화면. 창을 다시 열 때 이 화면으로 간다 */
    var pendingSection by mutableStateOf<Section?>(null)

    private fun post(title: String, body: String, section: Section) {
        val t = tray ?: return
        pendingSection = section
        t.sendNotification(Notification(title, body, Notification.Type.Info))
    }

    fun showUpdate(version: String) =
        post("새 버전 v${version}이 나왔어요", "앱 정보에서 업데이트하세요. 설치할 때까지 계속 알려드려요.", Section.ABOUT)

    fun showRoutine(routine: Routine) =
        post("${routine.emoji} ${routine.title}", "오늘의 루틴을 할 시간이에요 · ${formatMinute(routine.reminderMinute)}", Section.ROUTINE)

    fun showTodo(todo: Todo) {
        val text = buildString {
            append("마감 시각이에요")
            todo.dueMinute?.let { append(" · ").append(formatMinute(it)) }
            if (todo.memo.isNotBlank()) append("\n").append(todo.memo)
        }
        post(todo.title, text, Section.TODO)
    }

    fun showDaily(
        date: LocalDate,
        gospel: Gospel?,
        events: List<CalendarEvent>,
        calendarAllowed: Boolean,
        routines: List<Routine> = emptyList(),
    ) {
        val text = buildString {
            if (BuildConfig.CATHOLIC) {
                // 복음 본문 전체 대신 요약: 매일미사의 주제 문장, 없으면 본문 첫머리
                append("✠ 오늘의 복음")
                if (gospel != null) {
                    append(" · ").append(gospel.reference).append('\n')
                    val digest = gospel.title.ifBlank {
                        val body = gospel.body.replace('\n', ' ')
                        if (body.length > 90) body.take(90) + "…" else body
                    }
                    append(digest)
                } else {
                    append("\n복음을 불러오지 못했어요. 앱에서 다시 시도해 주세요.")
                }
                append("\n\n")
            }
            append("📅 오늘 일정\n")
            when {
                !calendarAllowed -> append("설정에서 구글 캘린더를 연결해 주세요.")
                events.isEmpty() -> append("오늘은 등록된 일정이 없어요.")
                else -> events.forEach { append("• ").append(formatEventTime(it, date)).append("  ").append(it.title).append('\n') }
            }
            if (!BuildConfig.CATHOLIC) {
                append("\n\n🔁 오늘 루틴\n")
                if (routines.isEmpty()) append("오늘 예정된 루틴이 없어요.")
                else append(routines.joinToString("  ") { "${it.emoji} ${it.title}" })
            }
        }.trim()
        post("좋은 아침이에요 · ${formatKoreanDate(date)}", text, Section.HOME)
    }
}
