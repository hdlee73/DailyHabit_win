package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.data.PcSync
import com.hdlee73.dailyhabit.sync.PairingCode
import com.hdlee73.dailyhabit.sync.SyncProtocol
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** QR 코드 그림 (흰 바탕, 검은 칸) */
fun qrImage(text: String, cells: Int = 6): ImageBitmap {
    val m = QRCodeWriter().encode(
        text, BarcodeFormat.QR_CODE, 0, 0,
        mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET to "UTF-8"),
    )
    val img = BufferedImage(m.width * cells, m.height * cells, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until m.height) for (x in 0 until m.width) {
        val c = if (m.get(x, y)) 0x000000 else 0xFFFFFF
        for (dy in 0 until cells) for (dx in 0 until cells) img.setRGB(x * cells + dx, y * cells + dy, c)
    }
    return img.toComposeImageBitmap()
}

private fun time(ms: Long) = SimpleDateFormat("M월 d일 HH:mm", Locale.KOREAN).format(Date(ms))

/** 설정의 '휴대폰과 동기화' 카드 (PC 쪽) */
@Composable
fun SyncCard() {
    var showQr by remember { mutableStateOf<PairingCode?>(null) }
    val devices = PcSync.devices
    val running = PcSync.running

    LaunchedEffect(PcSync.justPaired) {
        PcSync.justPaired?.let {
            Desktop.toast("$it 휴대폰과 짝을 맺었어요")
            PcSync.justPaired = null
            showQr = null
        }
    }

    SoftCard {
        SectionLabel("휴대폰과 동기화", color = MaterialTheme.colorScheme.primary)
        Text(
            "같은 와이파이에 있는 휴대폰 DailyHabit과 할 일, 루틴, 일기, 맛집(사진 포함), 독서, 장보기 기록을 서로 맞춰요. " +
                "인터넷 서버나 계정 없이 두 기기가 직접 주고받고, 내용은 짝을 맺을 때 나눈 비밀 키로 암호화돼요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (devices.isNotEmpty()) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("동기화 켜기", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (running) "켜져 있어요 · ${PcSync.localAddresses().joinToString(", ").ifEmpty { "네트워크 없음" }}"
                        else "꺼져 있으면 휴대폰이 이 PC를 찾지 못해요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = running, onCheckedChange = { if (it) PcSync.turnOn() else PcSync.turnOff() })
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            devices.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PhoneAndroid, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(d.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (d.lastSync > 0) "마지막 동기화 ${time(d.lastSync)}" else "짝 맺음 ${time(d.pairedAt)} · 아직 동기화 전",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { PcSync.unpair(d.id); Desktop.toast("${d.name} 연결을 해제했어요") }) { Text("연결 해제") }
                }
            }
        }
        PcSync.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        FilledTonalButton(
            onClick = {
                showQr = PcSync.beginPairing()
                if (showQr == null) Desktop.toast(PcSync.error ?: "동기화 서버를 열지 못했어요")
            },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) { Text(if (devices.isEmpty()) "휴대폰과 짝 맺기" else "다른 휴대폰 추가") }
        Text(
            "휴대폰은 앱을 열 때, 닫을 때, 그리고 와이파이에 있는 동안 30분마다 이 PC와 맞춰요. 휴대폰 앱의 설정 → PC와 동기화에서 ‘지금 동기화’를 누르면 바로 맞춰요. " +
                "PC 앱이 켜져 있어야 하고(창을 닫아도 트레이에 있으면 돼요), 처음 한 번 Windows 방화벽이 물으면 ‘개인 네트워크’를 허용해 주세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }

    showQr?.let { code ->
        val image = remember(code) { qrImage(code.encode()) }
        AlertDialog(
            onDismissRequest = { showQr = null; PcSync.cancelPairing() },
            title = { Text("휴대폰으로 QR 코드를 찍어 주세요") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "휴대폰 DailyHabit${if (BuildConfig.CATHOLIC) " 천주교인용" else ""} → 설정 → PC와 동기화 → ‘QR 코드로 짝 맺기’",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)) {
                        Image(image, "짝 맺기 QR 코드", Modifier.size(260.dp), filterQuality = FilterQuality.None)
                    }
                    Text(
                        "PC: ${code.name} · ${code.hosts.joinToString(", ").ifEmpty { "네트워크 주소를 찾지 못했어요" }} · 포트 ${code.port}\n" +
                            "${SyncProtocol.editionLabel(code.edition)}끼리만 짝을 맺을 수 있어요. 이 QR은 10분 동안만 쓸 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { Desktop.copy(code.encode()); Desktop.toast("짝 맺기 코드를 복사했어요") }) {
                        Text("카메라가 안 되면: 짝 맺기 코드 복사")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQr = null; PcSync.cancelPairing() }) { Text("닫기") } },
        )
    }
}
