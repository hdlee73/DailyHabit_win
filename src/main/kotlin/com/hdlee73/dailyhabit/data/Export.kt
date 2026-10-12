package com.hdlee73.dailyhabit.data

import java.io.OutputStream
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 한 줄 일기와 맛집(방문 기록 포함)을 엑셀 파일로 내보낸다 */
object Export {
    private fun day(epochDay: Long?): String = epochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()
    private fun weekday(epochDay: Long) = LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

    suspend fun writeXlsx(out: OutputStream) {
        val db = AppDatabase.get()
        val diary = db.diaryDao.all().sortedBy { it.epochDay }
        val places = db.restaurantDao.all().sortedBy { it.name }
        val visits = db.visitDao.all().sortedWith(compareBy({ it.epochDay }, { it.id }))
        val byId = places.associateBy { it.id }

        val diarySheet = Xlsx.Sheet(
            "한 줄 일기",
            listOf("날짜", "요일", "기분", "내용"),
            diary.map { listOf(day(it.epochDay), weekday(it.epochDay), DIARY_MOODS.getOrNull(it.mood - 1).orEmpty(), it.text) },
            listOf(13, 7, 7, 70),
        )
        val placeSheet = Xlsx.Sheet(
            "맛집",
            listOf("식당명", "종류", "주요 메뉴", "위치", "링크", "별점", "한 줄 평", "메모", "이럴 때 좋아요", "가격대(1인 기준)", "상태", "또 갈래요", "방문 횟수", "최근 방문일"),
            places.map {
                listOf(
                    it.name, it.category, it.menu, it.address, it.link, if (it.rating > 0) it.rating else null, it.comment, it.memo,
                    it.tagList.joinToString(", "), RESTAURANT_PRICES.getOrNull(it.price)?.takeIf { _ -> it.price > 0 }.orEmpty(),
                    if (it.wish) "가보고 싶어요" else "가봤어요", if (it.revisit) "예" else "", it.visitCount, day(it.lastVisitEpochDay),
                )
            },
            listOf(22, 12, 28, 30, 30, 7, 36, 40, 24, 16, 14, 10, 10, 13),
        )
        val visitSheet = Xlsx.Sheet(
            "맛집 방문 기록",
            listOf("방문일", "요일", "식당명", "종류", "위치", "메모"),
            visits.map {
                val p = byId[it.restaurantId]
                listOf(day(it.epochDay), weekday(it.epochDay), p?.name.orEmpty(), p?.category.orEmpty(), p?.address.orEmpty(), it.note)
            },
            listOf(13, 7, 22, 12, 30, 40),
        )
        Xlsx.write(out, listOf(diarySheet, placeSheet, visitSheet))
    }
}
