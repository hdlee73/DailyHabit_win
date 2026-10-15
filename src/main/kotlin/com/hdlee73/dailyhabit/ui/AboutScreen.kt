package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.UpdateInfo
import com.hdlee73.dailyhabit.data.Updater
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUi
    data class Ready(val info: UpdateInfo, val file: File, val hint: String?) : UpdateUi
    data class Failed(val message: String) : UpdateUi
}

private fun openUrl(url: String) = Desktop.openUrl(url)

/** 설치 프로그램을 띄우고, 파일을 바꿀 수 있도록 앱을 닫는다 (설치가 끝나면 다시 열림) */
private fun startInstall(file: File, version: String): Boolean {
    val opened = Updater.install(file, version)
    if (opened) {
        Thread {
            Thread.sleep(1500)
            kotlin.system.exitProcess(0)
        }.start()
    }
    return opened
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val settings = remember { AppSettings.default }
    val scope = rememberCoroutineScope()
    var autoCheck by remember { mutableStateOf(settings.autoUpdateCheck) }
    var lastChecked by remember { mutableStateOf(settings.lastUpdateCheck) }
    var state by remember {
        mutableStateOf<UpdateUi>(Updater.available?.takeIf { Updater.hasUpdate }?.let { UpdateUi.Available(it) } ?: UpdateUi.Idle)
    }

    fun check() {
        state = UpdateUi.Checking
        scope.launch {
            runCatching { Updater.fetchLatest() }
                .onSuccess { latest ->
                    settings.lastUpdateCheck = System.currentTimeMillis()
                    lastChecked = settings.lastUpdateCheck
                    Updater.available = latest
                    state = if (latest != null && Updater.isNewer(latest.version, BuildConfig.VERSION_NAME)) UpdateUi.Available(latest)
                    else UpdateUi.UpToDate
                }
                .onFailure { state = UpdateUi.Failed("확인하지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요.") }
        }
    }

    fun download(info: UpdateInfo) {
        state = UpdateUi.Downloading(info, 0f)
        scope.launch {
            runCatching { Updater.download(info) { p -> state = UpdateUi.Downloading(info, p) } }
                .onSuccess { file ->
                    val opened = startInstall(file, info.version)
                    state = UpdateUi.Ready(
                        info, file,
                        if (opened) null else "설치 프로그램을 열지 못했어요. 아래 버튼을 다시 누르거나 릴리스 페이지에서 내려받아 주세요.",
                    )
                }
                .onFailure { state = UpdateUi.Failed("내려받지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요.") }
        }
    }

    TopScrollColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 앱 이름과 버전
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource("icon.png"),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            Text(
                "DailyHabit",
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                "버전 ${BuildConfig.VERSION_NAME}  (빌드 ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Pill(
                if (BuildConfig.CATHOLIC) "천주교인용" else "일반용",
                MaterialTheme.colorScheme.primary,
                Modifier.padding(top = 8.dp),
            )
        }

        // 업데이트
        SoftCard {
            SectionLabel("업데이트", color = MaterialTheme.colorScheme.primary)
            when (val s = state) {
                UpdateUi.Idle, UpdateUi.UpToDate, is UpdateUi.Failed -> {
                    Text(
                        when (s) {
                            UpdateUi.UpToDate -> "최신 버전을 쓰고 있어요 ✓"
                            is UpdateUi.Failed -> s.message
                            else -> "새 버전이 나왔는지 확인해 보세요"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    if (lastChecked > 0) Text(
                        "마지막 확인 ${SimpleDateFormat("M월 d일 HH:mm", Locale.KOREAN).format(Date(lastChecked))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Button(onClick = ::check, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("업데이트 확인") }
                }
                UpdateUi.Checking -> {
                    Text("확인하는 중…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 14.dp))
                }
                is UpdateUi.Available -> {
                    Text("새 버전 v${s.info.version}이 있어요", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        "현재 v${BuildConfig.VERSION_NAME}" + if (s.info.apkSize > 0) " · 내려받기 ${"%.1f".format(s.info.apkSize / 1048576.0)}MB" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s.info.notes.isNotBlank()) Text(
                        s.info.notes.lines().filter { it.isNotBlank() }.joinToString("\n") { it.trim().trimStart('#', ' ') }.take(500),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Updater.lastInstallError?.let { err ->
                        Text(
                            "$err 다시 해 보고, 그래도 안 되면 아래 ‘설치 파일 직접 받기’로 받아 실행해 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Button(onClick = { download(s.info) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("다운로드하고 설치") }
                    if (Updater.lastInstallError != null) OutlinedButton(onClick = { openUrl(s.info.pageUrl) }, modifier = Modifier.fillMaxWidth()) { Text("설치 파일 직접 받기") }
                    Text(
                        "설치해도 할 일, 일기 같은 기록은 그대로 남아요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                is UpdateUi.Downloading -> {
                    Text("v${s.info.version} 내려받는 중… ${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                }
                is UpdateUi.Ready -> {
                    Text("v${s.info.version} 설치 준비가 됐어요", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        s.hint ?: "설치 프로그램이 열리면 ‘예’를 눌러 주세요. 앱이 잠시 닫혔다가 설치가 끝나면 다시 열려요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = {
                            val opened = startInstall(s.file, s.info.version)
                            state = s.copy(hint = if (opened) null else s.hint)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    ) { Text("설치 화면 열기") }
                    OutlinedButton(onClick = ::check, modifier = Modifier.fillMaxWidth()) { Text("다시 확인") }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("새 버전 알림 받기", style = MaterialTheme.typography.bodyLarge)
                    Text("새 버전이 나오면 윈도우 알림을 보내고, 설치할 때까지 앱을 열 때마다 알려줘요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoCheck, onCheckedChange = { autoCheck = it; settings.autoUpdateCheck = it })
            }
        }

        SoftCard {
            InfoRow(Icons.Outlined.Person, "만든이", "이현덕 · hdlee73@gmail.com") { openUrl("mailto:hdlee73@gmail.com") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(Icons.Outlined.History, "변경 내역 · 이전 버전", "GitHub 릴리스") { openUrl(Updater.RELEASES_PAGE) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(Icons.Outlined.Code, "소스 코드", "github.com/${Updater.REPO}") { openUrl("https://github.com/${Updater.REPO}") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(Icons.Outlined.RateReview, "의견 · 오류 알리기", "GitHub 이슈") { openUrl("https://github.com/${Updater.REPO}/issues") }
        }

        SoftCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "할 일, 루틴, 일기, 장보기, 독서, 맛집 기록은 이 PC에만 저장돼요. " +
                        "휴대폰 앱과 기록을 주고받으려면 설정의 ‘백업 파일 만들기’와 ‘백업 파일에서 복원’을 쓰세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
            Text(
                (if (BuildConfig.CATHOLIC) "복음: 한국천주교주교회의 매일미사 · 기도문: 가톨릭 기도서\n" else "") +
                    "일정: 구글 캘린더 비공개 주소(iCal)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
    }
}
