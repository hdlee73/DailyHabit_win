package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.DIARY_MOODS
import com.hdlee73.dailyhabit.data.DiaryEntry
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    val scope = rememberCoroutineScope()
    val entries by db.diaryDao.observeAll().collectAsState(initial = emptyList())
    val today = remember { LocalDate.now() }

    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("한 줄 일기", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "총 ${entries.size}개의 기록",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = {
                    val existingToday = entries.firstOrNull { it.epochDay == today.toEpochDay() }
                    editingEntry = existingToday ?: DiaryEntry(epochDay = today.toEpochDay(), text = "")
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("오늘 일기 쓰기")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (entries.isEmpty()) {
                EmptyState("📖", "작성된 일기가 없습니다", "오늘 하루를 간단한 한 줄과 기분 이모지로 기록해 보세요!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.epochDay }) { entry ->
                        val date = LocalDate.ofEpochDay(entry.epochDay)
                        val moodEmoji = DIARY_MOODS.getOrNull(entry.mood - 1) ?: "📝"

                        SoftCard(onClick = { editingEntry = entry }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(moodEmoji, fontSize = 28.sp)
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "$date (${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        entry.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch { db.diaryDao.delete(entry.epochDay) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingEntry != null) {
        DiaryEditDialog(
            initial = editingEntry!!,
            onDismiss = { editingEntry = null },
            onSave = { saved ->
                scope.launch {
                    db.diaryDao.upsert(saved)
                    editingEntry = null
                }
            },
        )
    }
}

@Composable
private fun DiaryEditDialog(
    initial: DiaryEntry,
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit,
) {
    var text by remember { mutableStateOf(initial.text) }
    var mood by remember { mutableIntStateOf(initial.mood) }
    val date = remember(initial.epochDay) { LocalDate.ofEpochDay(initial.epochDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$date 일기") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 기분 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    DIARY_MOODS.forEachIndexed { index, emoji ->
                        val moodValue = index + 1
                        val isSelected = mood == moodValue
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { mood = if (isSelected) 0 else moodValue },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(200) },
                    label = { Text("오늘의 한 줄 (최대 200자)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
                Text(
                    "${text.length} / 200자",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(
                            initial.copy(
                                text = text.trim(),
                                mood = mood,
                                updatedAt = System.currentTimeMillis(),
                            )
                        )
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
