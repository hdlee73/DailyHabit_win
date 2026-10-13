package com.hdlee73.dailyhabit.data

import com.hdlee73.dailyhabit.BuildConfig
import java.time.LocalDate

/** 천주교인용: '오늘의 말씀 읽기'와 '기도하기' 루틴. 해당 화면을 보면 오늘 체크가 자동으로 된다 */
object FaithRoutines {
    const val KIND_GOSPEL = 1
    const val KIND_PRAYER = 2

    /** 처음 한 번만 만든다. 이후 지우거나 고쳐도 다시 만들지 않는다 */
    suspend fun seed() {
        if (!BuildConfig.CATHOLIC) return
        val settings = AppSettings.default
        if (settings.faithRoutinesSeeded) return
        val dao = AppDatabase.get().routineDao
        val existing = dao.allRoutines()
        val today = LocalDate.now().toEpochDay()
        var order = existing.size
        if (existing.none { it.kind == KIND_GOSPEL }) {
            dao.insert(Routine(title = "오늘의 말씀 읽기", emoji = "📖", color = 0, createdEpochDay = today, sortOrder = order++, kind = KIND_GOSPEL))
        }
        if (existing.none { it.kind == KIND_PRAYER }) {
            dao.insert(Routine(title = "기도하기", emoji = "🙏", color = 1, createdEpochDay = today, sortOrder = order, kind = KIND_PRAYER))
        }
        settings.faithRoutinesSeeded = true
    }

    /** 말씀 화면을 열었거나 기도문을 열었을 때 호출. 오늘 이미 체크됐거나 오늘 쉬는 루틴이면 그대로 둔다 */
    suspend fun markDone(kind: Int) {
        if (!BuildConfig.CATHOLIC) return
        val dao = AppDatabase.get().routineDao
        val routine = dao.active().firstOrNull { it.kind == kind } ?: return
        val today = LocalDate.now()
        if (!routine.activeOn(today.dayOfWeek)) return
        if (dao.isChecked(routine.id, today.toEpochDay()) == 0) dao.check(RoutineCheck(routine.id, today.toEpochDay()))
    }
}
