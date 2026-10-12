package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.FaithRoutines
import com.hdlee73.dailyhabit.data.Gospel
import com.hdlee73.dailyhabit.data.GospelRepository
import com.hdlee73.dailyhabit.data.Todo
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(onNavigate: (Section) -> Unit, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    val repo = remember { GospelRepository.default }
    val db = remember { AppDatabase.get() }

    var gospel by remember { mutableStateOf<Gospel?>(repo.cached(today)) }
    var loading by remember { mutableStateOf(gospel == null) }
    var error by remember { mutableStateOf<String?>(null) }

    val todos by db.todoDao.observeAll().collectAsState(initial = emptyList())
    val activeRoutines by db.routineDao.observeActive().collectAsState(initial = emptyList())
    val checks by db.routineDao.observeChecks().collectAsState(initial = emptyList())

    val todayChecks = remember(checks, today) {
        checks.filter { it.epochDay == today.toEpochDay() }.map { it.routineId }.toSet()
    }
    val todayRoutines = remember(activeRoutines, today) {
        activeRoutines.filter { it.activeOn(today.dayOfWeek) }
    }
    val pendingTodos = remember(todos, today) {
        todos.filter { !it.done && (it.dueEpochDay == null || it.dueEpochDay <= today.toEpochDay()) }
    }

    LaunchedEffect(today) {
        FaithRoutines.markDone(FaithRoutines.KIND_GOSPEL)
        if (gospel == null) {
            loading = true
            repo.get(today).fold(
                onSuccess = { gospel = it; error = null },
                onFailure = { error = it.message ?: "복음을 불러오지 못했습니다." }
            )
            loading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 날짜 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "${today.year}년 ${today.monthValue}월 ${today.dayOfMonth}일",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = {
                scope.launch {
                    loading = true
                    repo.get(today, forceRefresh = true).fold(
                        onSuccess = { gospel = it; error = null },
                        onFailure = { error = it.message }
                    )
                    loading = false
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "새로고침")
            }
        }

        // 복음 카드
        SoftCard {
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (gospel != null) {
                val g = gospel!!
                if (g.liturgicalDay.isNotBlank()) {
                    Text(
                        g.liturgicalDay,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    g.title.ifEmpty { "오늘의 복음" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (g.reference.isNotBlank()) {
                    Text(
                        g.reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(
                    g.body,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        runCatching { Desktop.getDesktop().browse(URI(g.sourceUrl)) }
                    }) {
                        Text("매일미사 원문 보기 ↗", fontSize = 12.sp)
                    }
                }
            } else {
                Text(error ?: "복음을 불러올 수 없습니다.")
            }
        }

        // 오늘 할 일 요약
        SoftCard(onClick = { onNavigate(Section.TODO) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("오늘 할 일 (${pendingTodos.size})")
                Text("전체 보기 >", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            if (pendingTodos.isEmpty()) {
                Text("남은 할 일이 없습니다. 편안한 하루 보내세요!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pendingTodos.take(4).forEach { todo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleCheck(
                                checked = todo.done,
                                onToggle = {
                                    scope.launch {
                                        db.todoDao.update(todo.copy(done = !todo.done, completedAt = if (!todo.done) System.currentTimeMillis() else null))
                                    }
                                },
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(todo.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // 루틴 요약
        SoftCard(onClick = { onNavigate(Section.ROUTINE) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val doneCount = todayRoutines.count { it.id in todayChecks }
                SectionLabel("오늘 루틴 ($doneCount/${todayRoutines.size})")
                Text("전체 보기 >", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            if (todayRoutines.isEmpty()) {
                Text("오늘 설정된 루틴이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    todayRoutines.forEach { r ->
                        val isChecked = r.id in todayChecks
                        SoftCard(
                            modifier = Modifier.weight(1f),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                scope.launch { db.routineDao.toggle(r.id, today.toEpochDay()) }
                            },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(r.emoji, fontSize = 24.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(r.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
