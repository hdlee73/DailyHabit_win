package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.Backup
import com.hdlee73.dailyhabit.data.Export
import com.hdlee73.dailyhabit.data.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import com.hdlee73.dailyhabit.notify.Scheduler
import com.hdlee73.dailyhabit.notify.Autostart
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SettingsScreen(onNavigate: (Section) -> Unit = {}, modifier: Modifier = Modifier) {
    val settings = remember { AppSettings.default }
    var enabled by remember { mutableStateOf(settings.dailyEnabled) }
    var minute by remember { mutableIntStateOf(settings.dailyMinute) }
    var pickTime by remember { mutableStateOf(false) }

    // 백업 / 복원
    val scope = rememberCoroutineScope()
    var lastBackup by remember { mutableStateOf(settings.lastBackup) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    fun saveBackup(name: String) {
        val file = Desktop.saveFile("백업 파일 저장", name) ?: return
        scope.launch {
            val ok = runCatching {
                val json = Backup.export()
                withContext(Dispatchers.IO) { file.writeText(json, Charsets.UTF_8) }
            }.isSuccess
            lastBackup = settings.lastBackup
            Desktop.toast(if (ok) "백업 파일을 저장했어요" else "백업 파일을 저장하지 못했어요")
        }
    }
    fun saveExcel(name: String) {
        val file = Desktop.saveFile("엑셀 파일 저장", name) ?: return
        scope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) { file.outputStream().use { Export.writeXlsx(it) } }
            }.isSuccess
            Desktop.toast(if (ok) "엑셀 파일을 저장했어요" else "엑셀 파일을 저장하지 못했어요")
        }
    }
    fun openBackup() {
        val file = Desktop.openFile("백업 파일 고르기", "json", "txt") ?: return
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { file.readText(Charsets.UTF_8) } }
                .onSuccess { pendingRestore = it }
                .onFailure { Desktop.toast("파일을 읽지 못했어요") }
        }
    }

    // 윈도우 전용: 트레이 실행, 자동 실행, 캘린더 주소
    var keepInTray by remember { mutableStateOf(settings.keepInTray) }
    var autostart by remember { mutableStateOf(false) }
    var calendars by remember { mutableStateOf(settings.calendarUrls) }
    LaunchedEffect(Unit) { autostart = withContext(Dispatchers.IO) { Autostart.isEnabled() } }

    TopScrollColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SoftCard {
            SectionLabel("아침 알림", color = MaterialTheme.colorScheme.primary)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("매일 아침 알림 받기", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (com.hdlee73.dailyhabit.BuildConfig.CATHOLIC) "오늘 복음 요약과 오늘 일정을 알려드려요" else "오늘 일정과 오늘 루틴을 알려드려요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    settings.dailyEnabled = it
                })
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { pickTime = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("알림 시간", Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatMinute(minute),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                if (com.hdlee73.dailyhabit.BuildConfig.CATHOLIC) "알림을 누르면 오늘의 복음 화면이 열려요. PC를 늦게 켜면 켜자마자 보내드려요." else "알림을 누르면 오늘의 명언 화면이 열려요. PC를 늦게 켜면 켜자마자 보내드려요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            FilledTonalButton(
                onClick = {
                    Scheduler.sendDailyNow()
                    Desktop.toast("아침 알림을 지금 보내드릴게요")
                },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) { Text("지금 미리 받아보기") }
        }

        SyncCard()

        SoftCard {
            SectionLabel("구글 캘린더 연결")
            if (calendars.isEmpty()) {
                Text(
                    "연결하면 일정 화면과 아침 알림에 구글 캘린더 일정이 보여요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            calendars.forEachIndexed { i, url ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "캘린더 ${i + 1} · " + runCatching {
                            java.net.URLDecoder.decode(url.substringAfter("/ical/").substringBefore('/'), Charsets.UTF_8)
                        }.getOrDefault("").ifBlank { url.take(28) },
                        Modifier.weight(1f).padding(start = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                    )
                    TextButton(onClick = {
                        settings.calendarUrls = calendars - url
                        calendars = settings.calendarUrls
                    }) { Text("빼기") }
                }
            }
            CalendarConnect(
                onConnected = { calendars = settings.calendarUrls; Desktop.toast("캘린더를 연결했어요") },
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        SoftCard {
            SectionLabel("윈도우")
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("창을 닫아도 알림 받기", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "창을 닫으면 작업 표시줄 오른쪽 알림 영역(트레이)에 남아서 아침·루틴·할 일 알림을 보내요. 완전히 끄려면 트레이 아이콘에서 ‘종료’를 누르세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = keepInTray, onCheckedChange = { keepInTray = it; settings.keepInTray = it })
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Windows 시작할 때 실행", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (Autostart.available) "PC를 켜면 트레이에서 조용히 시작해서 알림을 놓치지 않아요."
                        else "설치한 앱에서만 바꿀 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autostart,
                    enabled = Autostart.available,
                    onCheckedChange = { on ->
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { Autostart.setEnabled(on) }
                            if (ok) autostart = on else Desktop.toast("바꾸지 못했어요")
                        }
                    },
                )
            }
        }

        SoftCard {
            SectionLabel("백업과 복원")
            Text(
                "할 일, 루틴, 일기, 장보기, 독서, 맛집 기록을 파일 하나로 저장해요. " +
                    "PC를 바꾸거나 앱을 다시 설치할 때 이 파일로 되돌릴 수 있어요. 휴대폰 앱의 백업 파일과 서로 주고받을 수 있고, 천주교인용과 일반용 사이에서도 옮길 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (lastBackup > 0) Text(
                "마지막 백업 ${SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREAN).format(Date(lastBackup))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            FilledTonalButton(
                onClick = { saveBackup("DailyHabit-backup-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) { Text("백업 파일 만들기") }
            OutlinedButton(
                onClick = { openBackup() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("백업 파일에서 복원") }
            Text(
                "한 줄 일기와 맛집(방문 기록 포함)은 엑셀 파일로도 저장할 수 있어요. 엑셀 파일은 보관·확인용이고, 앱으로 다시 불러오지는 않아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
            FilledTonalButton(
                onClick = { saveExcel("DailyHabit-diary-restaurants-${LocalDate.now()}.xlsx") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("일기·맛집을 엑셀로 저장") }
        }

        SoftCard(onClick = { onNavigate(Section.ABOUT) }) {
            val version = com.hdlee73.dailyhabit.BuildConfig.VERSION_NAME
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("앱 정보 · 업데이트", style = MaterialTheme.typography.bodyLarge)
                    Text("v$version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (Updater.hasUpdate) Pill("NEW", MaterialTheme.colorScheme.primary)
            }
        }
    }

    pendingRestore?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("백업에서 복원할까요?") },
            text = { Text("지금 저장된 할 일, 루틴, 일기, 장보기, 독서, 맛집 기록이 모두 백업 파일의 내용으로 바뀌어요.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        runCatching { Backup.restore(json) }
                            .onSuccess { restoreMessage = "복원했어요\n${it.text}" }
                            .onFailure {
                                restoreMessage = (it as? IllegalArgumentException)?.message
                                    ?: "백업 파일을 읽지 못해서 아무것도 바꾸지 않았어요."
                            }
                    }
                }) { Text("복원", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("취소") } },
        )
    }
    restoreMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { restoreMessage = null },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { restoreMessage = null }) { Text("확인") } },
        )
    }

    if (pickTime) {
        TimePickDialog(minute, onDismiss = { pickTime = false }) {
            minute = it
            settings.dailyMinute = it
            Desktop.toast("매일 ${formatMinute(it)}에 알려드릴게요")
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, label: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
        if (ok) Pill("허용됨", MaterialTheme.colorScheme.primary)
        else TextButton(onClick = onFix) { Text("허용하기") }
    }
}
