package com.hdlee73.dailyhabit

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Book
import com.hdlee73.dailyhabit.data.DiaryEntry
import com.hdlee73.dailyhabit.data.Restaurant
import com.hdlee73.dailyhabit.data.RestaurantVisit
import com.hdlee73.dailyhabit.data.Routine
import com.hdlee73.dailyhabit.data.RoutineCheck
import com.hdlee73.dailyhabit.data.Todo
import com.hdlee73.dailyhabit.ui.DailyHabitTheme
import com.hdlee73.dailyhabit.ui.Section
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.time.LocalDate
import kotlin.test.Test

/** -Pscreens=<폴더>로 실행하면 메뉴의 각 화면을 PNG로 저장한다 (디자인 확인용) */
class ScreensTest {
    @Test
    fun renderScreens() {
        val dir = System.getProperty("dailyhabit.screensDir")?.let { File(it) } ?: return
        dir.mkdirs()
        seed()
        for (section in Section.available()) {
            MainState.section = section
            MainState.drawerOpen = true
            val scene = ImageComposeScene(1180, 820, Density(1f)) { DailyHabitTheme { MainApp() } }
            try {
                // 데이터가 비동기로 들어오므로 몇 번 나눠 그린다
                var image = scene.render(0)
                for (i in 1..8) {
                    Thread.sleep(250)
                    image = scene.render(i * 100_000_000L)
                }
                val png = image.encodeToData(EncodedImageFormat.PNG) ?: continue
                File(dir, "${section.ordinal.toString().padStart(2, '0')}-${section.name.lowercase()}.png").writeBytes(png.bytes)
            } finally {
                scene.close()
            }
        }
    }

    private fun seed() = runBlocking {
        val db = AppDatabase.get()
        if (db.routineDao.allRoutines().size > 3) return@runBlocking
        val today = LocalDate.now()
        val t = today.toEpochDay()
        db.todoDao.insert(Todo(title = "보고서 마무리", dueEpochDay = t, dueMinute = 15 * 60, priority = 2, category = "일"))
        db.todoDao.insert(Todo(title = "우유 사기", dueEpochDay = t + 1, category = "쇼핑"))
        db.todoDao.insert(Todo(title = "부모님께 전화", category = "가정"))
        val r1 = db.routineDao.insert(Routine(title = "물 2L 마시기", emoji = "💧", color = 2, createdEpochDay = t - 10))
        db.routineDao.insert(Routine(title = "30분 걷기", emoji = "🚶", color = 3, createdEpochDay = t - 10, reminderEnabled = true))
        db.routineDao.insert(Routine(title = "영어 단어", emoji = "📚", color = 4, createdEpochDay = t - 10))
        (0L..6L).forEach { db.routineDao.check(RoutineCheck(r1, t - it)) }
        db.diaryDao.upsert(DiaryEntry(t, "오랜만에 산책하면서 하늘을 봤다. 감사한 하루.", 1))
        db.diaryDao.upsert(DiaryEntry(t - 1, "회의가 길었지만 결과가 좋았다.", 3))
        val rid = db.restaurantDao.insert(
            Restaurant(name = "을지로 평양냉면", category = "한식", menu = "물냉면, 수육", address = "서울 중구 을지로", rating = 5,
                comment = "육수가 깔끔해요", tags = "가족,회식", price = 3, visitCount = 2, lastVisitEpochDay = t - 3)
        )
        db.visitDao.insert(RestaurantVisit(restaurantId = rid, epochDay = t - 3, note = "수육 추천"))
        db.visitDao.insert(RestaurantVisit(restaurantId = rid, epochDay = t - 20))
        db.restaurantDao.insert(Restaurant(name = "성수 베이글", category = "카페·디저트", address = "서울 성동구 성수동", wish = true))
        db.bookDao.insert(Book(title = "데미안", author = "헤르만 헤세", totalPages = 240, currentPage = 120, status = 0, startEpochDay = t - 5))
        db.bookDao.insert(Book(title = "어린 왕자", author = "생텍쥐페리", status = 2, rating = 5, endEpochDay = t - 30, review = "다시 읽어도 좋다"))
    }
}
