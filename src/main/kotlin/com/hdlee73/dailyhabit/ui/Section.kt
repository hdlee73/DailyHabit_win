package com.hdlee73.dailyhabit.ui

import com.hdlee73.dailyhabit.data.AppSettings

/** 메뉴 항목 */
enum class Section(val label: String) {
    GOSPEL("오늘의 말씀"),
    PRAYER("기도"),
    SCHEDULE("일정"),
    TODO("할 일"),
    ROUTINE("루틴"),
    SETTINGS("설정"),
    QUOTE("오늘의 명언"),
    RESTAURANT("맛집"),
    JOURNAL("한 줄 일기"),
    SHOPPING("장보기"),
    BOOKS("독서"),
    ABOUT("앱 정보"),
    REVIEW("월간 돌아보기"),
    ;

    companion object {
        val HOME: Section get() = if (AppSettings.default.isCatholic) GOSPEL else QUOTE

        val primary: List<Section> get() = if (AppSettings.default.isCatholic) listOf(GOSPEL, PRAYER) else listOf(QUOTE)

        val daily = listOf(SCHEDULE, TODO, ROUTINE, JOURNAL, RESTAURANT, BOOKS, REVIEW)

        fun available(): List<Section> = primary + daily + SETTINGS + ABOUT

        fun fromIndex(i: Int): Section {
            val s = entries.getOrElse(i) { HOME }
            return if (s in available()) s else HOME
        }
    }
}
