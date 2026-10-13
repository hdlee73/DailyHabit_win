package com.hdlee73.dailyhabit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.net.URLEncoder

/**
 * 안드로이드에서 시스템에 맡기던 일(링크 열기, 공유, 토스트, 파일 고르기)의 윈도우 버전.
 * - 공유: 공유 시트가 없으므로 클립보드에 복사하고 알려준다
 * - 토스트: 창 아래쪽에 잠깐 뜨는 안내
 */
object Desktop {
    /** 화면 아래에 잠깐 보이는 안내 문구 (MainApp이 표시) */
    var toast by mutableStateOf<Pair<String, Long>?>(null)

    /** 파일 대화상자의 부모 창 */
    var window: Frame? = null

    fun toast(message: String) {
        toast = message to System.nanoTime()
    }

    fun openUrl(url: String) {
        runCatching { java.awt.Desktop.getDesktop().browse(URI(url.trim())) }
            .onFailure { toast("링크를 열지 못했어요") }
    }

    fun encode(s: String): String = URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")

    /** 지도 검색 (네이버 지도) */
    fun openMap(query: String) = openUrl("https://map.naver.com/p/search/" + encode(query))

    fun copy(text: String) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
    }

    /** 공유 = 클립보드에 복사 (메신저·메일에 붙여 넣기) */
    fun share(text: String) {
        copy(text)
        toast("복사했어요. 메신저나 메일에 붙여 넣으세요")
    }

    fun openFile(title: String, vararg extensions: String): File? {
        val fd = FileDialog(window, title, FileDialog.LOAD)
        if (extensions.isNotEmpty()) {
            fd.setFilenameFilter { _, name -> extensions.any { name.lowercase().endsWith(".$it") } }
            fd.file = extensions.joinToString(";") { "*.$it" }
        }
        fd.isVisible = true
        val name = fd.file ?: return null
        return File(fd.directory, name)
    }

    fun saveFile(title: String, defaultName: String): File? {
        val fd = FileDialog(window, title, FileDialog.SAVE)
        fd.file = defaultName
        fd.isVisible = true
        val name = fd.file ?: return null
        val ext = defaultName.substringAfterLast('.', "")
        val fixed = if (ext.isNotEmpty() && !name.lowercase().endsWith(".$ext")) "$name.$ext" else name
        return File(fd.directory, fixed)
    }
}
