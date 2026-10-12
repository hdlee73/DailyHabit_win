package com.hdlee73.dailyhabit.data

import java.time.LocalDate

data class RoutineStat(
    val routine: Routine,
    val currentStreak: Int,
    val bestStreak: Int,
    /** 최근 7일 달성률 (0..1), 해당 요일만 계산 */
    val week: Float,
    /** 최근 30일 달성률 */
    val month: Float,
    val totalChecks: Int,
)

/** 루틴 통계 계산. 반복 요일이 아닌 날은 연속 기록을 끊지 않는다. */
object RoutineStats {

    fun stat(routine: Routine, checkedDays: Set<Long>, today: LocalDate): RoutineStat {
        val start = LocalDate.ofEpochDay(minOf(routine.createdEpochDay, checkedDays.minOrNull() ?: routine.createdEpochDay))

        // 현재 연속: 오늘 아직 안 했으면 어제부터 센다
        var current = 0
        var d = today
        if (routine.activeOn(d.dayOfWeek) && d.toEpochDay() !in checkedDays) d = d.minusDays(1)
        while (!d.isBefore(start)) {
            if (routine.activeOn(d.dayOfWeek)) {
                if (d.toEpochDay() in checkedDays) current++ else break
            }
            d = d.minusDays(1)
        }

        var best = 0
        var run = 0
        d = start
        while (!d.isAfter(today)) {
            if (routine.activeOn(d.dayOfWeek)) {
                if (d.toEpochDay() in checkedDays) {
                    run++
                    if (run > best) best = run
                } else if (d != today) {
                    run = 0
                }
            }
            d = d.plusDays(1)
        }

        return RoutineStat(
            routine = routine,
            currentStreak = current,
            bestStreak = maxOf(best, current),
            week = rate(routine, checkedDays, today, 7, start),
            month = rate(routine, checkedDays, today, 30, start),
            totalChecks = checkedDays.size,
        )
    }

    fun rate(routine: Routine, checkedDays: Set<Long>, today: LocalDate, days: Int, start: LocalDate? = null): Float {
        var due = 0
        var done = 0
        for (i in 0 until days) {
            val d = today.minusDays(i.toLong())
            if (start != null && d.isBefore(start)) break
            if (!routine.activeOn(d.dayOfWeek)) continue
            val checked = d.toEpochDay() in checkedDays
            // 오늘은 이미 했을 때만 분모에 포함
            if (d == today && !checked) continue
            due++
            if (checked) done++
        }
        return if (due == 0) 0f else done.toFloat() / due
    }

    /** 날짜별 전체 달성률 (모든 활성 루틴 기준) */
    fun dailyRate(routines: List<Routine>, checks: Map<Long, Set<Long>>, day: LocalDate): Float? {
        val due = routines.filter { it.activeOn(day.dayOfWeek) && it.createdEpochDay <= day.toEpochDay() }
        if (due.isEmpty()) return null
        val done = due.count { day.toEpochDay() in checks[it.id].orEmpty() }
        return done.toFloat() / due.size
    }
}
