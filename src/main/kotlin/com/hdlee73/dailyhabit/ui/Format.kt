package com.hdlee73.dailyhabit.ui

import com.hdlee73.dailyhabit.data.CalendarEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

fun formatKoreanDate(date: LocalDate): String =
    "${date.monthValue}월 ${date.dayOfMonth}일 (${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"

fun relativeDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "오늘"
    today.plusDays(1) -> "내일"
    today.plusDays(2) -> "모레"
    else -> ""
}

/** 특정 날짜 기준으로 일정 시간을 표시 */
fun formatEventTime(e: CalendarEvent, day: LocalDate): String {
    if (e.allDay) return "종일"
    val startsToday = e.start.toLocalDate() == day
    val endsToday = e.end.toLocalDate() == day
    return when {
        startsToday && endsToday -> "${e.start.format(TIME)}–${e.end.format(TIME)}"
        startsToday -> "${e.start.format(TIME)}~"
        endsToday -> "~${e.end.format(TIME)}"
        else -> "종일"
    }
}

/** 자정부터 분 → "오전 9:00" */
fun formatMinute(minute: Int): String {
    val h = minute / 60
    val m = minute % 60
    val ampm = if (h < 12) "오전" else "오후"
    val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
    return "$ampm $h12:${"%02d".format(m)}"
}

/** 할 일 마감 표시 */
fun formatDue(day: LocalDate, minute: Int?, today: LocalDate = LocalDate.now()): String {
    val rel = relativeDayLabel(day, today)
    val dateText = when {
        rel.isNotEmpty() -> rel
        day.year == today.year -> "${day.monthValue}/${day.dayOfMonth} (${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"
        else -> "${day.year}. ${day.monthValue}. ${day.dayOfMonth}."
    }
    return if (minute != null) "$dateText ${formatMinute(minute)}" else dateText
}

val WEEKDAY_SHORT = listOf("월", "화", "수", "목", "금", "토", "일")
