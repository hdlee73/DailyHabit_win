package com.hdlee73.dailyhabit.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.DataInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

/** 맛집 방문 사진. 앱 데이터 폴더(photos)에 줄여서 저장한다 (PC 밖으로 나가지 않음) */
object Photos {
    private fun dir() = File(AppDirs.root, "photos").apply { mkdirs() }

    fun file(name: String): File? =
        name.takeIf { it.isNotBlank() }?.let { File(dir(), it) }?.takeIf { it.exists() }

    fun delete(name: String) {
        if (name.isNotBlank()) File(dir(), name).delete()
    }

    /** 고른 사진을 긴 변 1280px 이하 JPEG로 줄여 저장하고 파일 이름을 돌려준다. 실패하면 null */
    suspend fun import(source: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val original = ImageIO.read(source) ?: return@runCatching null
            val rotated = rotate(original, exifRotation(source))
            val scale = (1280f / max(rotated.width, rotated.height)).coerceAtMost(1f)
            val out = resize(rotated, (rotated.width * scale).toInt().coerceAtLeast(1), (rotated.height * scale).toInt().coerceAtLeast(1))
            val name = "p_${System.currentTimeMillis()}_${(0..9999).random()}.jpg"
            ImageIO.write(out, "jpg", File(dir(), name))
            name
        }.getOrNull()
    }

    /** 화면에 그릴 작은 이미지 */
    suspend fun load(name: String, maxPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val f = file(name) ?: return@withContext null
        runCatching {
            val img = ImageIO.read(f) ?: return@runCatching null
            val scale = (maxPx.toFloat() / max(img.width, img.height)).coerceAtMost(1f)
            val small = if (scale < 1f) resize(img, (img.width * scale).toInt().coerceAtLeast(1), (img.height * scale).toInt().coerceAtLeast(1)) else img
            small.toComposeImageBitmap()
        }.getOrNull()
    }

    private fun resize(src: BufferedImage, w: Int, h: Int): BufferedImage {
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(src, 0, 0, w, h, null)
        g.dispose()
        return out
    }

    private fun rotate(src: BufferedImage, degrees: Int): BufferedImage {
        if (degrees == 0) return src
        val swap = degrees == 90 || degrees == 270
        val w = if (swap) src.height else src.width
        val h = if (swap) src.width else src.height
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        val t = AffineTransform()
        t.translate(w / 2.0, h / 2.0)
        t.rotate(Math.toRadians(degrees.toDouble()))
        t.translate(-src.width / 2.0, -src.height / 2.0)
        g.drawImage(src, t, null)
        g.dispose()
        return out
    }

    /** 휴대폰 사진의 EXIF 방향 값을 읽어 돌릴 각도로 바꾼다 (JPEG가 아니거나 값이 없으면 0) */
    private fun exifRotation(f: File): Int = runCatching {
        DataInputStream(f.inputStream().buffered()).use { input -> readOrientation(input) }
    }.getOrDefault(0)

    private fun readOrientation(input: DataInputStream): Int {
        if (input.readUnsignedShort() != 0xFFD8) return 0
        while (true) {
            val marker = input.readUnsignedShort()
            if (marker and 0xFF00 != 0xFF00 || marker == 0xFFDA) return 0
            val len = input.readUnsignedShort()
            if (marker != 0xFFE1) {
                input.skipBytes(len - 2)
                continue
            }
            val data = ByteArray(len - 2)
            input.readFully(data)
            if (String(data, 0, 4, Charsets.US_ASCII) != "Exif") return 0
            val base = 6
            val little = data[base] == 'I'.code.toByte()
            fun u16(o: Int): Int = if (little) (data[o].toInt() and 0xFF) or ((data[o + 1].toInt() and 0xFF) shl 8)
            else ((data[o].toInt() and 0xFF) shl 8) or (data[o + 1].toInt() and 0xFF)
            fun u32(o: Int): Int = if (little) u16(o) or (u16(o + 2) shl 16) else (u16(o) shl 16) or u16(o + 2)
            val ifd = base + u32(base + 4)
            for (i in 0 until u16(ifd)) {
                val e = ifd + 2 + i * 12
                if (u16(e) == 0x0112) {
                    return when (u16(e + 8)) {
                        6 -> 90
                        3 -> 180
                        8 -> 270
                        else -> 0
                    }
                }
            }
            return 0
        }
    }
}
