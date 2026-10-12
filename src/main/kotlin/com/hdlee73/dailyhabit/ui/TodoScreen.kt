package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Priority
import com.hdlee73.dailyhabit.data.TODO_CATEGORIES
import com.hdlee73.dailyhabit.data.Todo
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodoScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    val scope = rememberCoroutineScope()
    val todos by db.todoDao.observeAll().collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyPending by remember { mutableStateOf(false) }

    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var isNewDialog by remember { mutableStateOf(false) }

    val filteredTodos = remember(todos, selectedCategory, showOnlyPending) {
        todos.filter { todo ->
            (selectedCategory == null || todo.category == selectedCategory) &&
                (!showOnlyPending || !todo.done)
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("할 일", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "미완료 ${todos.count { !it.done }}개 · 완료 ${todos.count { it.done }}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (todos.any { it.done }) {
                        OutlinedButton(onClick = { scope.launch { db.todoDao.clearDone() } }) {
                            Text("완료 항목 정리")
                        }
                    }
                    Button(onClick = {
                        editingTodo = Todo(title = "")
                        isNewDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("할 일 추가")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 카테고리 필터
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("전체") },
                )
                TODO_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) },
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = showOnlyPending,
                    onClick = { showOnlyPending = !showOnlyPending },
                    label = { Text("미완료만") },
                )
            }

            Spacer(Modifier.height(16.dp))

            // 할 일 목록
            if (filteredTodos.isEmpty()) {
                EmptyState("📝", "등록된 할 일이 없습니다", "새로운 할 일을 추가하고 실천해 보세요!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredTodos, key = { it.id }) { todo ->
                        TodoItemRow(
                            todo = todo,
                            onToggle = {
                                scope.launch {
                                    db.todoDao.update(
                                        todo.copy(
                                            done = !todo.done,
                                            completedAt = if (!todo.done) System.currentTimeMillis() else null,
                                        )
                                    )
                                }
                            },
                            onEdit = {
                                editingTodo = todo
                                isNewDialog = false
                            },
                            onDelete = {
                                scope.launch { db.todoDao.delete(todo) }
                            },
                        )
                    }
                }
            }
        }
    }

    // 추가/수정 다이얼로그
    if (editingTodo != null) {
        TodoEditDialog(
            initial = editingTodo!!,
            isNew = isNewDialog,
            onDismiss = { editingTodo = null },
            onSave = { saved ->
                scope.launch {
                    if (isNewDialog) {
                        db.todoDao.insert(saved)
                    } else {
                        db.todoDao.update(saved)
                    }
                    editingTodo = null
                }
            },
        )
    }
}

@Composable
private fun TodoItemRow(
    todo: Todo,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SoftCard(
        color = if (todo.done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLowest,
        onClick = onEdit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleCheck(
                checked = todo.done,
                onToggle = onToggle,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        todo.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = if (todo.done) FontWeight.Normal else FontWeight.Medium,
                        ),
                        color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    if (todo.priority == 2) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                            Text("높음", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (todo.category.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(todo.category, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (todo.memo.isNotBlank() || todo.dueEpochDay != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (todo.dueEpochDay != null) {
                            val due = LocalDate.ofEpochDay(todo.dueEpochDay)
                            val isOverdue = !todo.done && due.isBefore(LocalDate.now())
                            Text(
                                "📅 $due" + (todo.dueMinute?.let { " %02d:%02d".format(it / 60, it % 60) } ?: ""),
                                fontSize = 12.sp,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (todo.memo.isNotBlank()) {
                            Text(todo.memo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun TodoEditDialog(
    initial: Todo,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Todo) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var memo by remember { mutableStateOf(initial.memo) }
    var priority by remember { mutableIntStateOf(initial.priority) }
    var category by remember { mutableStateOf(initial.category) }
    var dueDate by remember { mutableStateOf(initial.dueEpochDay?.let { LocalDate.ofEpochDay(it) }) }
    var dueMinute by remember { mutableStateOf(initial.dueMinute) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "할 일 추가" else "할 일 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("할 일 제목") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )

                // 우선순위
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("우선순위: ", fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    listOf(Priority.LOW to 0, Priority.NORMAL to 1, Priority.HIGH to 2).forEach { (p, idx) ->
                        FilterChip(
                            selected = priority == idx,
                            onClick = { priority = idx },
                            label = { Text(p.label) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }

                // 카테고리
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Text("카테고리: ", fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    TODO_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = if (category == cat) "" else cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                // 마감일
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(dueDate?.toString() ?: "마감일 설정")
                    }
                    if (dueDate != null) {
                        IconButton(onClick = { dueDate = null; dueMinute = null }) {
                            Icon(Icons.Default.Close, contentDescription = "마감일 삭제")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text(dueMinute?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "시간 추가")
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
                                memo = memo.trim(),
                                priority = priority,
                                category = category,
                                dueEpochDay = dueDate?.toEpochDay(),
                                dueMinute = dueMinute,
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )

    if (showDatePicker) {
        DatePickDialog(
            initial = dueDate ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onPick = { dueDate = it },
        )
    }

    if (showTimePicker) {
        TimePickDialog(
            initialMinute = dueMinute ?: (12 * 60),
            onDismiss = { showTimePicker = false },
            onPick = { dueMinute = it },
        )
    }
}
