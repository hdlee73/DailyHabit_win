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
 * 캐시는 %APPDATA%\DailyHabit\gospel_cache.json 에 저장.
 */
class GospelRepository {
    private val cacheFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "DailyHabit").apply { mkdirs() }
        File(dir, "gospel_cache.json")
    }

    private var cacheJson: JSONObject = JSONObject()

    init {
        if (cacheFile.exists()) {
            runCatching {
                cacheJson = JSONObject(cacheFile.readText(Charsets.UTF_8))
            }
        }
    }

    fun sourceUrl(date: LocalDate): String =
        "https://maria.catholic.or.kr/mi_pr/missa/missa.asp?menu=missa" +
            "&gyear=${date.year}&gmonth=${"%02d".format(date.monthValue)}&gday=${"%02d".format(date.dayOfMonth)}"

    fun cached(date: LocalDate): Gospel? {
        val raw = cacheJson.optString(date.toString(), null) ?: return null
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

        cacheJson.put(g.date.toString(), o.toString())
        runCatching {
            cacheFile.writeText(cacheJson.toString(2), Charsets.UTF_8)
        }
    }

    private fun fetch(date: LocalDate): Gospel {
        val url = sourceUrl(date)
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) DailyHabit-Win")
            .timeout(10_000)
            .get()

        val day = doc.select(".missa_tit").firstOrNull()?.text().orEmpty()
            .ifEmpty { doc.select(".tit_box .tit").firstOrNull()?.text().orEmpty() }

        val gospels = doc.select(".missa_box").filter { box ->
            val h = box.select(".tit, h4, h3, .sub_tit").text()
            h.contains("복음") && !h.contains("복음환호송")
        }
        val target = gospels.firstOrNull() ?: doc.select(".missa_box").lastOrNull()
            ?: throw IllegalStateException("복음 본문을 찾을 수 없습니다.")

        val title = target.select(".tit, h4, h3").firstOrNull()?.text()
            ?.replace("복음", "")?.trim()?.trim('(', ')', '<', '>', '·', ' ')
            .orEmpty()

        val ref = target.select(".sub_tit, .ref, p.sub").firstOrNull()?.text().orEmpty()

        val textEls = target.select(".txt, p:not(.sub):not(.ref), .con")
        val body = if (textEls.isNotEmpty()) {
            textEls.joinToString("\n\n") { it.wholeText().trim() }
        } else {
            target.wholeText().trim()
        }.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")

        return Gospel(
            date = date,
            liturgicalDay = day,
            title = title,
            reference = ref,
            body = body,
            sourceUrl = url,
        )
    }

    companion object {
        val default = GospelRepository()
    }
}
