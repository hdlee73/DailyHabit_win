package com.hdlee73.dailyhabit

import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Backup
import com.hdlee73.dailyhabit.data.Book
import com.hdlee73.dailyhabit.data.DiaryEntry
import com.hdlee73.dailyhabit.data.Export
import com.hdlee73.dailyhabit.data.Prayers
import com.hdlee73.dailyhabit.data.Quotes
import com.hdlee73.dailyhabit.data.Restaurant
import com.hdlee73.dailyhabit.data.Routine
import com.hdlee73.dailyhabit.data.RoutineStats
import com.hdlee73.dailyhabit.data.ShoppingItem
import com.hdlee73.dailyhabit.data.Todo
import com.hdlee73.dailyhabit.data.Updater
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DailyHabitHarnessTest {

    @Test
    fun testDatabaseCrud() = runTest {
        val db = AppDatabase.get()

        // 1. Todo CRUD
        val todoId = db.todoDao.insert(Todo(title = "테스트 할 일", priority = 2, category = "일"))
        assertTrue(todoId > 0)
        val allTodos = db.todoDao.all()
        assertTrue(allTodos.any { it.id == todoId && it.title == "테스트 할 일" })
        val insertedTodo = allTodos.first { it.id == todoId }
        db.todoDao.update(insertedTodo.copy(done = true))
        assertTrue(db.todoDao.all().first { it.id == todoId }.done)

        // 2. Routine CRUD & Check Toggle
        val routineId = db.routineDao.insert(Routine(title = "물 마시기", emoji = "💧", createdEpochDay = LocalDate.now().toEpochDay()))
        assertTrue(routineId > 0)
        val todayEpoch = LocalDate.now().toEpochDay()
        assertEquals(0, db.routineDao.isChecked(routineId, todayEpoch))
        db.routineDao.toggle(routineId, todayEpoch)
        assertEquals(1, db.routineDao.isChecked(routineId, todayEpoch))
        db.routineDao.toggle(routineId, todayEpoch)
        assertEquals(0, db.routineDao.isChecked(routineId, todayEpoch))

        // 3. Diary CRUD
        val diaryDay = LocalDate.of(2026, 10, 12).toEpochDay()
        db.diaryDao.upsert(DiaryEntry(epochDay = diaryDay, text = "오늘도 좋은 하루", mood = 1))
        val diary = db.diaryDao.all().firstOrNull { it.epochDay == diaryDay }
        assertNotNull(diary)
        assertEquals("오늘도 좋은 하루", diary.text)

        // 4. Restaurant CRUD
        val restId = db.restaurantDao.insert(Restaurant(name = "맛있는 식당", category = "한식", rating = 5))
        assertTrue(restId > 0)
        assertTrue(db.restaurantDao.all().any { it.name == "맛있는 식당" })

        // 5. Books CRUD
        val bookId = db.bookDao.insert(Book(title = "클린 코드", author = "로버트 마틴", totalPages = 400, currentPage = 100))
        assertTrue(bookId > 0)
        val book = db.bookDao.allBooks().first { it.id == bookId }
        assertEquals(0.25f, book.progress)

        // 6. Shopping CRUD
        val shopId = db.shoppingDao.insert(ShoppingItem(name = "사과", qty = "3개"))
        assertTrue(shopId > 0)
        assertTrue(db.shoppingDao.all().any { it.name == "사과" })
    }

    @Test
    fun testRoutineStreakCalculations() {
        val today = LocalDate.of(2026, 10, 12)
        val routine = Routine(title = "매일 운동", createdEpochDay = today.minusDays(10).toEpochDay())
        val checks = setOf(
            today.toEpochDay(),
            today.minusDays(1).toEpochDay(),
            today.minusDays(2).toEpochDay(),
        )
        val stat = RoutineStats.stat(routine, checks, today)
        assertEquals(3, stat.currentStreak)
        assertEquals(3, stat.bestStreak)
    }

    @Test
    fun testBackupAndRestore() = runTest {
        val db = AppDatabase.get()
        db.todoDao.insert(Todo(title = "백업 테스트 할일"))

        val json = Backup.export()
        assertTrue(json.contains("DailyHabit"))
        assertTrue(json.contains("백업 테스트 할일"))

        val summary = Backup.inspect(json).getOrNull()
        assertNotNull(summary)
        assertTrue(summary.counts.any { it.first == "할 일" && it.second > 0 })

        val restoreResult = Backup.restore(json)
        assertTrue(restoreResult.isSuccess)
    }

    @Test
    fun testExportXlsx() = runTest {
        val out = ByteArrayOutputStream()
        Export.writeXlsx(out)
        val bytes = out.toByteArray()
        assertTrue(bytes.isNotEmpty())
        // ZIP/XLSX 매직 넘버 확인: PK (0x50, 0x4B)
        assertEquals(0x50.toByte(), bytes[0])
        assertEquals(0x4B.toByte(), bytes[1])
    }

    @Test
    fun testQuotesAndPrayers() {
        val today = LocalDate.now()
        val quote = Quotes.forDate(today)
        assertTrue(quote.ko.isNotBlank())
        assertTrue(quote.author.isNotBlank())

        assertTrue(Prayers.all.isNotEmpty())
        val ourFather = Prayers.byId("our_father")
        assertNotNull(ourFather)
        assertTrue(ourFather.text.contains("하늘에 계신 우리 아버지"))
    }

    @Test
    fun testUpdaterVersionComparison() {
        assertTrue(Updater.isNewer("1.1.0", "1.0.0"))
        assertTrue(Updater.isNewer("2.0.0", "1.9.9"))
        assertFalse(Updater.isNewer("1.0.0", "1.0.0"))
        assertFalse(Updater.isNewer("1.0.0", "1.1.0"))
    }
}
