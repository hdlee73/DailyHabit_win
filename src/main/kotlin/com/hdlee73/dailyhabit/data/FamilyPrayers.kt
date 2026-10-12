package com.hdlee73.dailyhabit.data

import java.time.LocalDate

data class Prayer(val title: String, val text: String)

/** 아침 알림에 넣는 가정 기도: 「가톨릭 기도서」의 가정 기도를 날마다 돌아가며 */
object FamilyPrayers {
    fun forDate(date: LocalDate): Prayer {
        val p = Prayers.familyFor(date)
        val text = p.text.trimIndent().lines()
            .filterNot { it.startsWith("(") && it.endsWith(")") }
            .joinToString("\n") { it.removePrefix("○ ").removePrefix("● ").removePrefix("◎ ").removePrefix("╋ ").removePrefix("† ") }
        return Prayer(p.title, text)
    }
}
