package com.hdlee73.dailyhabit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class CalendarEvent(
    val id: Long,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val location: String,
    val calendarName: String,
    val color: Int,
    /** 브라우저에서 열 주소 (구글 캘린더 일정 링크가 있으면) */
    val url: String = "",
)

/**
 * 구글 캘린더의 '비공개 주소(iCal 형식)'를 읽어 일정을 보여준다.
 * 안드로이드는 휴대폰에 동기화된 캘린더를 읽지만, 윈도우에는 그런 공용 캘린더가 없어서
 * 로그인 없이 쓸 수 있는 iCal 주소 방식으로 대신한다. 받은 내용은 저장해 두어 오프라인에서도 보인다.
 */
class CalendarRepository {
    private val settings = AppSettings.default
    private val cacheDir = File(AppDirs.root, "calendar").apply { mkdirs() }

    /** 안드로이드의 '캘린더 권한'에 해당: 캘린더 주소를 넣었는지 */
    fun hasPermission(): Boolean = settings.calendarUrls.isNotEmpty()

    suspend fun events(from: LocalDate, days: Int): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val urls = settings.calendarUrls
        if (urls.isEmpty()) return@withContext emptyList()
        val to = from.plusDays(days.toLong())
        urls.flatMapIndexed { index, url ->
            val text = load(url) ?: return@flatMapIndexed emptyList()
            runCatching { Ics.parse(text, index, from, to) }.getOrDefault(emptyList())
        }.sortedWith(compareBy({ it.start.toLocalDate() }, { !it.allDay }, { it.start }))
    }

    /** 새로 받아 오고, 실패하면 마지막으로 받은 내용을 쓴다 */
    private fun load(url: String): String? {
        val cache = File(cacheDir, "cal_${url.hashCode().toUInt()}.ics")
        val fresh = runCatching {
            val u = url.trim().replaceFirst(Regex("^webcal://", RegexOption.IGNORE_CASE), "https://")
            val conn = URI(u).toURL().openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 20_000
                conn.setRequestProperty("User-Agent", "DailyHabit-Windows")
                if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
                conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()?.takeIf { it.contains("BEGIN:VCALENDAR") }
        if (fresh != null) {
            runCatching { cache.writeText(fresh, Charsets.UTF_8) }
            return fresh
        }
        return runCatching { cache.readText(Charsets.UTF_8) }.getOrNull()
    }

    /** 날짜별로 묶기. 여러 날에 걸친 일정은 해당하는 모든 날에 표시한다. */
    fun groupByDay(events: List<CalendarEvent>, from: LocalDate, days: Int): Map<LocalDate, List<CalendarEvent>> =
        (0 until days).associate { offset ->
            val day = from.plusDays(offset.toLong())
            day to events.filter { !it.start.toLocalDate().isAfter(day) && !it.end.toLocalDate().isBefore(day) }
        }
}

/** 필요한 만큼만 구현한 iCalendar(.ics) 해석기: 단일 일정, 반복 일정(RRULE·EXDATE·RECURRENCE-ID) */
internal object Ics {
    private val PALETTE = listOf(0xFF4285F4, 0xFF0B8043, 0xFF8E24AA, 0xFFF4511E, 0xFF039BE5, 0xFFD50000).map { it.toInt() }

    private class Prop(val name: String, val params: Map<String, String>, val value: String)

    private class When(val time: LocalDateTime, val allDay: Boolean)

    fun parse(text: String, calendarIndex: Int, from: LocalDate, to: LocalDate): List<CalendarEvent> {
        // 접힌 줄 펼치기
        val lines = text.replace("\r\n", "\n").replace("\n ", "").replace("\n\t", "").split('\n')
        var calName = ""
        val events = mutableListOf<List<Prop>>()
        var current: MutableList<Prop>? = null
        var depth = 0
        for (line in lines) {
            if (line.isBlank()) continue
            val p = prop(line) ?: continue
            when {
                p.name == "BEGIN" && p.value == "VEVENT" -> { current = mutableListOf(); depth = 0 }
                p.name == "END" && p.value == "VEVENT" -> { current?.let { events += it }; current = null }
                current != null && p.name == "BEGIN" -> depth++
                current != null && p.name == "END" -> depth--
                current != null && depth == 0 -> current.add(p)
                p.name == "X-WR-CALNAME" -> calName = unescape(p.value)
            }
        }
        val color = PALETTE[calendarIndex.mod(PALETTE.size)]
        val name = calName.ifBlank { "캘린더 ${calendarIndex + 1}" }

        // 반복 일정 중 따로 고친 회차 (UID + 원래 시작 시각)
        val overridden = events.mapNotNull { e ->
            val rid = e.firstOrNull { it.name == "RECURRENCE-ID" } ?: return@mapNotNull null
            val uid = e.firstOrNull { it.name == "UID" }?.value.orEmpty()
            uid to (time(rid)?.time ?: return@mapNotNull null)
        }.toSet()

        val result = mutableListOf<CalendarEvent>()
        for (e in events) {
            fun v(n: String) = e.firstOrNull { it.name == n }
            if (v("STATUS")?.value == "CANCELLED") continue
            val start = v("DTSTART")?.let { time(it) } ?: continue
            val endProp = v("DTEND")?.let { time(it) }
            val duration = when {
                endProp != null -> java.time.Duration.between(start.time, endProp.time)
                start.allDay -> java.time.Duration.ofDays(1)
                else -> v("DURATION")?.value?.let { runCatching { java.time.Duration.parse(it) }.getOrNull() }
                    ?: java.time.Duration.ZERO
            }
            val uid = v("UID")?.value.orEmpty()
            val title = v("SUMMARY")?.value?.let(::unescape)?.takeIf { it.isNotBlank() } ?: "(제목 없음)"
            val location = v("LOCATION")?.value?.let(::unescape).orEmpty()
            val url = v("URL")?.value.orEmpty()
            val exdates = e.filter { it.name == "EXDATE" }.flatMap { p ->
                p.value.split(',').mapNotNull { time(Prop("EXDATE", p.params, it))?.time }
            }.toSet()
            val rrule = v("RRULE")?.value
            val starts = if (rrule != null && v("RECURRENCE-ID") == null) {
                expand(start.time, rrule, from, to).filter { it !in exdates && (uid to it) !in overridden }
            } else listOf(start.time)

            for (s in starts) {
                var end = s.plus(duration)
                // 종일 일정의 끝은 다음 날 0시로 저장되므로 하루 앞당긴다
                if (start.allDay) end = end.minusSeconds(1)
                if (end.isBefore(s)) end = s
                if (s.toLocalDate().isBefore(to) && !end.toLocalDate().isBefore(from)) {
                    result += CalendarEvent(
                        id = (uid + s).hashCode().toLong(),
                        title = title,
                        start = s,
                        end = end,
                        allDay = start.allDay,
                        location = location,
                        calendarName = name,
                        color = color,
                        url = url,
                    )
                }
            }
        }
        return result
    }

    private fun prop(line: String): Prop? {
        // 값 안의 ':'는 그대로 두고, 따옴표 안이 아닌 첫 ':'에서 나눈다
        var inQuote = false
        var idx = -1
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') inQuote = !inQuote
            if (c == ':' && !inQuote) { idx = i; break }
        }
        if (idx < 0) return null
        val head = line.substring(0, idx).split(';')
        val params = head.drop(1).mapNotNull {
            val eq = it.indexOf('=')
            if (eq < 0) null else it.substring(0, eq).uppercase() to it.substring(eq + 1).trim('"')
        }.toMap()
        return Prop(head[0].uppercase(), params, line.substring(idx + 1))
    }

    private fun unescape(s: String) = s.replace("\\n", " ").replace("\\N", " ").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    private val LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    private fun time(p: Prop): When? = runCatching {
        val v = p.value.trim()
        if (p.params["VALUE"] == "DATE" || v.length == 8) {
            When(LocalDate.parse(v, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay(), true)
        } else {
            val local = LocalDateTime.parse(v.removeSuffix("Z"), LOCAL)
            val zone = when {
                v.endsWith("Z") -> ZoneOffset.UTC
                p.params["TZID"] != null -> runCatching { ZoneId.of(p.params["TZID"]) }.getOrDefault(ZoneId.systemDefault())
                else -> ZoneId.systemDefault()
            }
            When(local.atZone(zone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), false)
        }
    }.getOrNull()

    private val DAYS = mapOf(
        "MO" to DayOfWeek.MONDAY, "TU" to DayOfWeek.TUESDAY, "WE" to DayOfWeek.WEDNESDAY, "TH" to DayOfWeek.THURSDAY,
        "FR" to DayOfWeek.FRIDAY, "SA" to DayOfWeek.SATURDAY, "SU" to DayOfWeek.SUNDAY,
    )

    /** 반복 규칙으로 [from, to) 안의 시작 시각들을 만든다 (자주 쓰는 경우만) */
    private fun expand(start: LocalDateTime, rule: String, from: LocalDate, to: LocalDate): List<LocalDateTime> {
        val r = rule.split(';').mapNotNull { val kv = it.split('=', limit = 2); if (kv.size == 2) kv[0].uppercase() to kv[1] else null }.toMap()
        val freq = r["FREQ"] ?: return listOf(start)
        val interval = r["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val count = r["COUNT"]?.toIntOrNull()
        val until = r["UNTIL"]?.let { u -> time(Prop("UNTIL", emptyMap(), u))?.time }
        val byDay = r["BYDAY"]?.split(',')?.mapNotNull { DAYS[it.takeLast(2)] }.orEmpty()
        val byMonthDay = r["BYMONTHDAY"]?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        val out = mutableListOf<LocalDateTime>()
        var produced = 0
        val limit = to.atStartOfDay()
        fun accept(t: LocalDateTime): Boolean {
            if (t.isBefore(start)) return true
            if (until != null && t.isAfter(until)) return false
            if (count != null && produced >= count) return false
            produced++
            if (!t.toLocalDate().isBefore(from.minusDays(31)) && t.isBefore(limit)) out += t
            return true
        }
        var guard = 0
        when (freq) {
            "DAILY" -> {
                var t = start
                while (t.isBefore(limit) && guard++ < 20000) {
                    if (!accept(t)) break
                    t = t.plusDays(interval.toLong())
                }
            }
            "WEEKLY" -> {
                val days = byDay.ifEmpty { listOf(start.dayOfWeek) }.sorted()
                var weekStart = start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                loop@ while (weekStart.atStartOfDay().isBefore(limit) && guard++ < 5000) {
                    for (d in days) {
                        val t = weekStart.plusDays((d.value - 1).toLong()).atTime(start.toLocalTime())
                        if (!accept(t)) break@loop
                    }
                    weekStart = weekStart.plusWeeks(interval.toLong())
                }
            }
            "MONTHLY" -> {
                var month = start.toLocalDate().withDayOfMonth(1)
                loop@ while (month.atStartOfDay().isBefore(limit) && guard++ < 2000) {
                    val candidates = when {
                        byMonthDay.isNotEmpty() -> byMonthDay.mapNotNull { d ->
                            val day = if (d > 0) d else month.lengthOfMonth() + d + 1
                            if (day in 1..month.lengthOfMonth()) month.withDayOfMonth(day) else null
                        }
                        r["BYDAY"] != null -> r["BYDAY"]!!.split(',').mapNotNull { spec ->
                            val dow = DAYS[spec.takeLast(2)] ?: return@mapNotNull null
                            val n = spec.dropLast(2).toIntOrNull() ?: 1
                            if (n > 0) month.with(TemporalAdjusters.dayOfWeekInMonth(n, dow)).takeIf { it.month == month.month }
                            else month.with(TemporalAdjusters.lastInMonth(dow))
                        }
                        else -> listOfNotNull(start.dayOfMonth.takeIf { it <= month.lengthOfMonth() }?.let { month.withDayOfMonth(it) })
                    }.sorted()
                    for (c in candidates) if (!accept(c.atTime(start.toLocalTime()))) break@loop
                    month = month.plusMonths(interval.toLong())
                }
            }
            "YEARLY" -> {
                var t = start
                while (t.isBefore(limit) && guard++ < 500) {
                    if (!accept(t)) break
                    t = start.plusYears(interval.toLong() * guard)
                }
            }
            else -> return listOf(start)
        }
        return out
    }
}
