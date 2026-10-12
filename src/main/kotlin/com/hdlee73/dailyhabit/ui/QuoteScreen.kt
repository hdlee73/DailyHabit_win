package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Quotes
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun QuoteScreen(onNavigate: (Section) -> Unit, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val todayIndex = remember { Quotes.indexFor(today) }
    var index by remember { mutableIntStateOf(todayIndex) }
    val quote = Quotes.all[index.mod(Quotes.all.size)]
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get() }

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
            Row {
                IconButton(onClick = { index-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전")
                }
                IconButton(onClick = { index = todayIndex }) {
                    Text("오늘", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { index++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음")
                }
            }
        }

        // 명언 카드
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                Text("오늘의 명언", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    val s = "\"${quote.ko}\"\n— ${quote.author}"
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(s), null)
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "복사", modifier = Modifier.padding(4.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "“${quote.ko}”",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal, lineHeight = 34.sp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                quote.en,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "— ${quote.author}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
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
                Text("남은 할 일이 없습니다. 멋진 하루 보내세요!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
