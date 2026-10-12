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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Routine
import com.hdlee73.dailyhabit.data.RoutineStats
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun RoutineScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    val routines by db.routineDao.observeActive().collectAsState(initial = emptyList())
    val checks by db.routineDao.observeChecks().collectAsState(initial = emptyList())

    val checkedMap = remember(checks) {
        checks.groupBy { it.routineId }.mapValues { it.value.map { c -> c.epochDay }.toSet() }
    }

    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var isNewDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("루틴", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    val todayActive = routines.filter { it.activeOn(today.dayOfWeek) }
                    val doneCount = todayActive.count { (checkedMap[it.id] ?: emptySet()).contains(today.toEpochDay()) }
                    Text(
                        "오늘 달성률 $doneCount / ${todayActive.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = {
                    editingRoutine = Routine(title = "", createdEpochDay = today.toEpochDay())
                    isNewDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("루틴 추가")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (routines.isEmpty()) {
                EmptyState("✨", "등록된 루틴이 없습니다", "매일 실천하고 싶은 습관을 등록해 보세요!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(routines, key = { it.id }) { routine ->
                        val routineChecks = checkedMap[routine.id] ?: emptySet()
                        val isCheckedToday = routineChecks.contains(today.toEpochDay())
                        val stat = RoutineStats.stat(routine, routineChecks, today)

                        RoutineItemCard(
                            routine = routine,
                            isCheckedToday = isCheckedToday,
                            stat = stat,
                            onToggle = {
                                scope.launch { db.routineDao.toggle(routine.id, today.toEpochDay()) }
                            },
                            onEdit = {
                                editingRoutine = routine
                                isNewDialog = false
                            },
                            onDelete = {
                                scope.launch { db.routineDao.delete(routine.id) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (editingRoutine != null) {
        RoutineEditDialog(
            initial = editingRoutine!!,
            isNew = isNewDialog,
            onDismiss = { editingRoutine = null },
            onSave = { saved ->
                scope.launch {
                    if (isNewDialog) {
                        db.routineDao.insert(saved)
                    } else {
                        db.routineDao.update(saved)
                    }
                    editingRoutine = null
                }
            },
        )
    }
}

@Composable
private fun RoutineItemCard(
    routine: Routine,
    isCheckedToday: Boolean,
    stat: com.hdlee73.dailyhabit.data.RoutineStat,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SoftCard(
        color = if (isCheckedToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLowest,
        onClick = onEdit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleCheck(
                checked = isCheckedToday,
                onToggle = onToggle,
                size = 32.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(routine.emoji, fontSize = 20.sp)
                    Text(
                        routine.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔥 현재 ${stat.currentStreak}일 연속", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    Text("최고 ${stat.bestStreak}일", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("최근 7일 ${(stat.week * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun RoutineEditDialog(
    initial: Routine,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Routine) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var daysMask by remember { mutableIntStateOf(initial.days) }

    val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "루틴 추가" else "루틴 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(2) },
                        label = { Text("이모지") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("루틴 이름") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }

                Column {
                    Text("반복 요일", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dayNames.forEachIndexed { index, name ->
                            val bit = 1 shl index
                            val selected = (daysMask and bit) != 0
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    daysMask = if (selected) daysMask and bit.inv() else daysMask or bit
                                },
                                label = { Text(name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            initial.copy(
                                title = title.trim(),
                                emoji = emoji.ifBlank { "✨" },
                                days = daysMask,
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && daysMask != 0,
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
