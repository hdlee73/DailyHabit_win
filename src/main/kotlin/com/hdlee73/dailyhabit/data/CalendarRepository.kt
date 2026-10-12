package com.hdlee73.dailyhabit.data

import java.time.LocalDate
import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val location: String = "",
    val calendarName: String = "로컬 일정",
    val color: Int = 0,
)

/** 데스크톱 환경에서는 Todo 항목 중 마감일이 있는 것들을 일정 캘린더 이벤트로 매핑해 제공 */
class CalendarRepository {
    suspend fun events(from: LocalDate, days: Int): List<CalendarEvent> {
        val to = from.plusDays(days.toLong())
        val todos = AppDatabase.get().todoDao.all()
        return todos.filter { it.dueEpochDay != null }.mapNotNull { todo ->
            val due = LocalDate.ofEpochDay(todo.dueEpochDay!!)
            if (!due.isBefore(from) && due.isBefore(to)) {
                val start = if (todo.dueMinute != null) {
                    due.atTime(todo.dueMinute / 60, todo.dueMinute % 60)
                } else {
                    due.atStartOfDay()
                }
                CalendarEvent(
                    id = todo.id,
                    title = todo.title,
                    start = start,
                    end = start.plusHours(1),
                    allDay = todo.dueMinute == null,
                    location = todo.category,
                    calendarName = if (todo.done) "완료된 할 일" else "할 일",
                    color = if (todo.done) 0xFF888888.toInt() else 0xFFC6613F.toInt(),
                )
            } else null
        }
    }

    fun groupByDay(events: List<CalendarEvent>, from: LocalDate, days: Int): Map<LocalDate, List<CalendarEvent>> {
        val map = (0 until days).associate { from.plusDays(it.toLong()) to mutableListOf<CalendarEvent>() }
        events.forEach { e ->
            val day = e.start.toLocalDate()
            map[day]?.add(e)
        }
        return map
    }
}
