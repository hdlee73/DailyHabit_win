package com.hdlee73.dailyhabit.data

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 외부 라이브러리 없이 만드는 아주 단순한 .xlsx 파일 (표 여러 장, 첫 줄 굵게·고정) */
object Xlsx {
    class Sheet(val name: String, val header: List<String>, val rows: List<List<Any?>>, val widths: List<Int> = emptyList())

    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private val BAD_CHARS = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]")

    private fun esc(s: String) = s.replace(BAD_CHARS, "")
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + n % 26))
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    private fun cell(ref: String, value: Any?, style: Int): String = when (value) {
        null -> ""
        is Int, is Long, is Double, is Float ->
            "<c r=\"$ref\"${if (style != 0) " s=\"$style\"" else ""}><v>$value</v></c>"
        else ->
            "<c r=\"$ref\"${if (style != 0) " s=\"$style\"" else ""} t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(value.toString())}</t></is></c>"
    }

    private fun sheetXml(sheet: Sheet): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"$NS\">")
        append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        if (sheet.widths.isNotEmpty()) {
            append("<cols>")
            sheet.widths.forEachIndexed { i, w -> append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>") }
            append("</cols>")
        }
        append("<sheetData>")
        append("<row r=\"1\">")
        sheet.header.forEachIndexed { c, h -> append(cell("${colName(c)}1", h, 1)) }
        append("</row>")
        sheet.rows.forEachIndexed { r, row ->
            append("<row r=\"${r + 2}\">")
            row.forEachIndexed { c, v -> append(cell("${colName(c)}${r + 2}", v, 0)) }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    fun write(out: OutputStream, sheets: List<Sheet>) {
        require(sheets.isNotEmpty())
        ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val head = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            put(
                "[Content_Types].xml",
                head + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                    sheets.indices.joinToString("") {
                        "<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    } + "</Types>",
            )
            put(
                "_rels/.rels",
                head + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>",
            )
            put(
                "xl/workbook.xml",
                head + "<workbook xmlns=\"$NS\" xmlns:r=\"$REL\"><sheets>" +
                    sheets.mapIndexed { i, s -> "<sheet name=\"${esc(s.name.take(31))}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>" }.joinToString("") +
                    "</sheets></workbook>",
            )
            put(
                "xl/_rels/workbook.xml.rels",
                head + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    sheets.indices.joinToString("") { "<Relationship Id=\"rId${it + 1}\" Type=\"$REL/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>" } +
                    "<Relationship Id=\"rId${sheets.size + 1}\" Type=\"$REL/styles\" Target=\"styles.xml\"/></Relationships>",
            )
            put(
                "xl/styles.xml",
                head + "<styleSheet xmlns=\"$NS\">" +
                    "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
                    "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
                    "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                    "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                    "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>" +
                    "</styleSheet>",
            )
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
    }
}
