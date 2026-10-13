package com.hdlee73.dailyhabit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.time.LocalDate

data class Gospel(
    val date: LocalDate,
    val liturgicalDay: String,
    val title: String,
    val reference: String,
    val body: String,
    val sourceUrl: String,
)

/**
 * 한국천주교주교회의 '매일미사'(maria.catholic.or.kr)에서 오늘의 복음을 가져온다.
 * 성공한 결과는 날짜별로 저장해 두어(앱 데이터 폴더의 gospel_cache.json) 오프라인에서도 다시 볼 수 있다.
 */
class GospelRepository {
    private val cacheFile = File(AppDirs.root, "gospel_cache.json")

    private fun loadCache(): JSONObject =
        runCatching { JSONObject(cacheFile.readText(Charsets.UTF_8)) }.getOrDefault(JSONObject())

    fun sourceUrl(date: LocalDate): String =
        "https://maria.catholic.or.kr/mi_pr/missa/missa.asp?menu=missa" +
            "&gyear=${date.year}&gmonth=${"%02d".format(date.monthValue)}&gday=${"%02d".format(date.dayOfMonth)}"

    fun cached(date: LocalDate): Gospel? {
        val raw = loadCache().optString(date.toString(), "").ifEmpty { return null }
        return runCatching {
            val o = JSONObject(raw)
            Gospel(
                date = date,
                liturgicalDay = o.optString("day"),
                title = o.optString("title"),
                reference = o.optString("ref"),
                body = o.getString("body"),
                sourceUrl = o.optString("url", sourceUrl(date)),
            )
        }.getOrNull()
    }

    suspend fun get(date: LocalDate, forceRefresh: Boolean = false): Result<Gospel> = withContext(Dispatchers.IO) {
        if (!forceRefresh) cached(date)?.let { return@withContext Result.success(it) }
        runCatching { fetch(date) }
            .onSuccess { save(it) }
            .recoverCatching { err -> cached(date) ?: throw err }
    }

    private fun save(g: Gospel) {
        val o = JSONObject()
            .put("day", g.liturgicalDay)
            .put("title", g.title)
            .put("ref", g.reference)
            .put("body", g.body)
            .put("url", g.sourceUrl)
        synchronized(GospelRepository::class.java) {
            val cache = loadCache()
            // 최근 14일치만 보관
            cache.keySet().toList()
                .filter { runCatching { LocalDate.parse(it) }.getOrNull()?.isBefore(g.date.minusDays(14)) == true }
                .forEach { cache.remove(it) }
            cache.put(g.date.toString(), o.toString())
            runCatching { cacheFile.writeText(cache.toString(), Charsets.UTF_8) }
        }
    }

    private fun fetch(date: LocalDate): Gospel {
        val url = sourceUrl(date)
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) DailyHabit/1.0")
            .timeout(15_000)
            .get()
        // 줄바꿈을 보존한 순수 텍스트로 변환
        doc.select("br").forEach { it.after("\n") }
        doc.select("p, div, h1, h2, h3, h4, li, tr").forEach { it.after("\n") }
        val lines = doc.body().wholeText()
            .replace(' ', ' ')
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return parse(lines, date, url)
    }

    companion object {
        private val START = Regex("✠?\\s*(\\S+?)(?:가|이) 전한 거룩한 복음입니다\\.?\\s*(.*)")
        private const val END = "주님의 말씀입니다"
        private val DAY = Regex("^\\((백|홍|녹|자|흑|장미)\\)\\s*(.+)")

        fun parse(lines: List<String>, date: LocalDate, url: String): Gospel {
            val startIdx = lines.indexOfLast { START.containsMatchIn(it) }
            require(startIdx >= 0) { "복음 본문을 찾지 못했습니다" }
            val endIdx = (startIdx + 1 until lines.size).firstOrNull { lines[it].startsWith(END) }
                ?: lines.size
            val m = START.find(lines[startIdx])!!
            val evangelist = m.groupValues[1].trim().removePrefix("✠").trim()
            var reference = m.groupValues[2].trim()
            var bodyStart = startIdx + 1
            if (reference.isEmpty() && bodyStart < endIdx && Regex("^\\d+[,.:]\\d").containsMatchIn(lines[bodyStart])) {
                reference = lines[bodyStart]
                bodyStart++
            }
            val body = lines.subList(bodyStart, endIdx)
                .joinToString("\n")
                .trim()
            require(body.length > 20) { "복음 본문이 너무 짧습니다" }

            // 복음 바로 위의 <...> 형태 주제 문장
            val title = (startIdx - 1 downTo maxOf(0, startIdx - 4))
                .map { lines[it] }
                .firstOrNull { it.startsWith("<") && it.endsWith(">") }
                ?.removeSurrounding("<", ">")
                .orEmpty()
            val day = lines.firstNotNullOfOrNull { DAY.find(it)?.value }.orEmpty()
            return Gospel(
                date = date,
                liturgicalDay = day,
                title = title,
                reference = listOf(evangelist + "복음", reference).filter { it.isNotBlank() }.joinToString(" "),
                body = body,
                sourceUrl = url,
            )
        }
    }
}
