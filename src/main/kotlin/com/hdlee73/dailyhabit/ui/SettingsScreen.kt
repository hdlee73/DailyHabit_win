package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.Backup
import com.hdlee73.dailyhabit.data.Export
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@Composable
fun SettingsScreen(onSettingsChanged: () -> Unit, modifier: Modifier = Modifier) {
    val settings = remember { AppSettings.default }
    val scope = rememberCoroutineScope()

    var isCatholic by remember { mutableStateOf(settings.isCatholic) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var restoreSummary by remember { mutableStateOf<Backup.Summary?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("설정", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

            // 에디션(판본) 설정
            SoftCard {
                SectionLabel("앱 모드 설정")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("천주교인용 모드", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isCatholic) "오늘의 말씀(복음) 및 기도서 화면을 기본으로 표시합니다."
                            else "오늘의 명언 화면을 기본으로 표시합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isCatholic,
                        onCheckedChange = {
                            isCatholic = it
                            settings.isCatholic = it
                            onSettingsChanged()
                        },
                    )
                }
            }

            // 데이터 백업 및 복원
            SoftCard {
                SectionLabel("데이터 백업 및 복원")
                Spacer(Modifier.height(8.dp))
                Text(
                    "모든 할 일, 루틴, 일기, 맛집, 독서 데이터를 JSON 파일로 내보내거나 Android 앱의 백업 파일을 가져올 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val fd = FileDialog(null as Frame?, "백업 파일 저장", FileDialog.SAVE)
                        fd.file = "DailyHabit_backup_${LocalDate.now()}.json"
                        fd.isVisible = true
                        val dir = fd.directory
                        val file = fd.file
                        if (dir != null && file != null) {
                            scope.launch {
                                val json = Backup.export()
                                File(dir, file).writeText(json, Charsets.UTF_8)
                                statusMessage = "백업이 저장되었습니다: $file"
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("백업 파일 저장 (Export)")
                    }

                    OutlinedButton(onClick = {
                        val fd = FileDialog(null as Frame?, "백업 파일 선택", FileDialog.LOAD)
                        fd.isVisible = true
                        val dir = fd.directory
                        val file = fd.file
                        if (dir != null && file != null) {
                            scope.launch {
                                val text = File(dir, file).readText(Charsets.UTF_8)
                                Backup.inspect(text).fold(
                                    onSuccess = {
                                        restoreSummary = it
                                        pendingRestoreJson = text
                                    },
                                    onFailure = {
                                        statusMessage = "올바른 백업 파일이 아닙니다: ${it.message}"
                                    }
                                )
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("백업 파일 복원 (Import)")
                    }
                }
            }

            // 엑셀 내보내기
            SoftCard {
                SectionLabel("엑셀(.xlsx) 파일 내보내기")
                Spacer(Modifier.height(8.dp))
                Text(
                    "한 줄 일기와 맛집 탐방 기록을 표 형태의 엑셀 스프레드시트로 내보냅니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val fd = FileDialog(null as Frame?, "엑셀 파일 저장", FileDialog.SAVE)
                    fd.file = "DailyHabit_기록_${LocalDate.now()}.xlsx"
                    fd.isVisible = true
                    val dir = fd.directory
                    val file = fd.file
                    if (dir != null && file != null) {
                        scope.launch {
                            val target = File(dir, file)
                            FileOutputStream(target).use { out ->
                                Export.writeXlsx(out)
                            }
                            statusMessage = "엑셀 파일이 저장되었습니다: $file"
                        }
                    }
                }) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("엑셀 파일로 내보내기")
                }
            }
        }
    }

    if (statusMessage != null) {
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("알림") },
            text = { Text(statusMessage!!) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) { Text("확인") }
            },
        )
    }

    if (restoreSummary != null && pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = { restoreSummary = null; pendingRestoreJson = null },
            title = { Text("백업 파일 복원 확인") },
            text = {
                Column {
                    Text("다음 데이터로 기존 데이터를 덮어씁니다:")
                    Spacer(Modifier.height(8.dp))
                    Text(restoreSummary!!.text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("계속하시겠습니까?", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val rawJson = pendingRestoreJson
                    if (rawJson != null) {
                        scope.launch {
                            Backup.restore(rawJson).fold(
                                onSuccess = {
                                    statusMessage = "데이터가 성공적으로 복원되었습니다."
                                    onSettingsChanged()
                                },
                                onFailure = {
                                    statusMessage = "복원 실패: ${it.message}"
                                }
                            )
                            restoreSummary = null
                            pendingRestoreJson = null
                        }
                    }
                }) {
                    Text("복원 진행")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreSummary = null; pendingRestoreJson = null }) { Text("취소") }
            },
        )
    }
}
