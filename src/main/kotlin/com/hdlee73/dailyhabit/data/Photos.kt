package com.hdlee73.dailyhabit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

/** 맛집 방문 사진. 앱 전용 폴더에 줄여서 저장 (%APPDATA%\DailyHabit\photos) */
object Photos {
    private fun dir(): File {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        return File(File(appData, "DailyHabit"), "photos").apply { mkdirs() }
    }

    fun file(name: String): File? =
        name.takeIf { it.isNotBlank() }?.let { File(dir(), it) }?.takeIf { it.exists() }

    fun delete(name: String) {
        if (name.isNotBlank()) File(dir(), name).delete()
    }

    /** 파일을 읽어 긴 변 1280px 이하 JPEG로 줄여 저장하고 파일 이름을 반환 */
    suspend fun import(sourceFile: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val original = ImageIO.read(sourceFile) ?: return@runCatching null
            val longest = max(original.width, original.height)
            val scale = (1280f / longest).coerceAtMost(1f)
            val targetW = (original.width * scale).toInt()
            val targetH = (original.height * scale).toInt()

            val resized = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
            val g2d = resized.createGraphics()
            g2d.drawImage(original.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH), 0, 0, null)
            g2d.dispose()

            val name = "p_${System.currentTimeMillis()}_${(0..9999).random()}.jpg"
            val dest = File(dir(), name)
            ImageIO.write(resized, "jpg", dest)
            name
        }.getOrNull()
    }
}
