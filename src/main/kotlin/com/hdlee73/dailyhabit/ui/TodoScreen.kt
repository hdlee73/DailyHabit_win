package com.hdlee73.dailyhabit.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Priority
import com.hdlee73.dailyhabit.data.TODO_CATEGORIES
import com.hdlee73.dailyhabit.data.Todo
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class TodoFilter(val label: String) { ALL("전체"), TODAY("오늘"), UPCOMING("예정"), OVERDUE("지난 일"), DONE("완료") }

@Composable
fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> Color(0xFFD9534F)
    Priority.NORMAL -> MaterialTheme.colorScheme.primary
    Priority.LOW -> Color(0xFF8E9AA6)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(openEditor: Boolean = false, onEditorOpened: () -> Unit = {}, modifier: Modifier = Modifier) {
    val dao = remember { AppDatabase.get().todoDao }
    val todos by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TodoFilter.ALL) }
    var category by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Todo?>(null) }
    var creating by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    LaunchedEffect(openEditor) {
        if (openEditor) {
            creating = true
            onEditorOpened()
        }
    }
    val t = today.toEpochDay()

    fun quickAdd() {
        val title = input.trim()
        if (title.isEmpty()) return
        // 오늘 탭/분류 필터 상태에 맞춰 기본값 채우기
        scope.launch {
            dao.insert(
                Todo(
                    title = title,
                    dueEpochDay = if (filter == TodoFilter.TODAY) t else null,
                    category = category.orEmpty(),
                )
            )
        }
        input = ""
    }

    fun toggle(todo: Todo) = scope.launch {
        val updated = todo.copy(done = !todo.done, completedAt = if (!todo.done) System.currentTimeMillis() else null)
        dao.update(updated)
    }

    fun delete(todo: Todo) = scope.launch {
        dao.delete(todo)
        val r = snackbar.showSnackbar("‘${todo.title}’ 삭제됨", actionLabel = "되돌리기", withDismissAction = true)
        if (r == SnackbarResult.ActionPerformed) {
            val id = dao.insert(todo.copy(id = 0))
        }
    }

    val pending = todos.filter { !it.done }
    val overdueCount = pending.count { (it.dueEpochDay ?: Long.MAX_VALUE) < t }
    val todayCount = pending.count { it.dueEpochDay == t }
    val doneToday = todos.count { it.done && it.completedAt != null &&
        java.time.Instant.ofEpochMilli(it.completedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today }

    val visible = todos
        .filter { category == null || it.category == category }
        .filter {
            when (filter) {
                TodoFilter.ALL -> !it.done
                TodoFilter.TODAY -> !it.done && it.dueEpochDay != null && it.dueEpochDay <= t
                TodoFilter.UPCOMING -> !it.done && it.dueEpochDay != null && it.dueEpochDay > t
                TodoFilter.OVERDUE -> !it.done && it.dueEpochDay != null && it.dueEpochDay < t
                TodoFilter.DONE -> it.done
            }
        }
    val sections: List<Pair<String, List<Todo>>> = if (filter == TodoFilter.DONE) {
        listOf("완료한 일" to visible.sortedByDescending { it.completedAt ?: 0 })
    } else {
        fun inRange(x: Todo, from: Long, to: Long) = x.dueEpochDay != null && x.dueEpochDay in from..to
        listOf(
            "기한 지남" to visible.filter { (it.dueEpochDay ?: Long.MAX_VALUE) < t },
            "오늘" to visible.filter { it.dueEpochDay == t },
            "내일" to visible.filter { it.dueEpochDay == t + 1 },
            "이번 주" to visible.filter { inRange(it, t + 2, t + 7) },
            "나중에" to visible.filter { (it.dueEpochDay ?: Long.MIN_VALUE) > t + 7 },
            "날짜 없음" to visible.filter { it.dueEpochDay == null },
        ).filter { it.second.isNotEmpty() }
            .map { (k, v) -> k to v.sortedWith(compareByDescending<Todo> { it.priority }.thenBy { it.dueEpochDay ?: 0 }.thenBy { it.dueMinute ?: 0 }) }
    }

    Box(modifier.fillMaxSize()) {
        TopLazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── 요약
            item {
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val totalToday = doneToday + todayCount + overdueCount
                        ProgressRing(
                            progress = if (totalToday == 0) 0f else doneToday.toFloat() / totalToday,
                            size = 72.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        ) {
                            Text("$doneToday", style = MaterialTheme.typography.titleLarge)
                        }
                        Column(Modifier.padding(start = 18.dp).weight(1f)) {
                            Text(
                                when {
                                    pending.isEmpty() -> "할 일을 모두 마쳤어요 🎉"
                                    overdueCount > 0 -> "밀린 일이 ${overdueCount}개 있어요"
                                    todayCount > 0 -> "오늘 할 일 ${todayCount}개"
                                    else -> "남은 할 일 ${pending.size}개"
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("오늘 ${doneToday}개 완료", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StatBlock("${pending.size}", "남음")
                                StatBlock("$todayCount", "오늘")
                                StatBlock("$overdueCount", "지남", color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            // ── 빠른 추가
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("할 일을 빠르게 추가") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { quickAdd() }),
                        trailingIcon = {
                            IconButton(onClick = { creating = true }) { Icon(Icons.Filled.Tune, contentDescription = "자세히 추가") }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconButton(onClick = { quickAdd() }, modifier = Modifier.padding(start = 8.dp).size(52.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "추가")
                    }
                }
            }
            // ── 필터
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TodoFilter.entries.forEach { f ->
                            val count = when (f) {
                                TodoFilter.ALL -> pending.size
                                TodoFilter.TODAY -> todayCount + overdueCount
                                TodoFilter.UPCOMING -> pending.count { (it.dueEpochDay ?: Long.MIN_VALUE) > t }
                                TodoFilter.OVERDUE -> overdueCount
                                TodoFilter.DONE -> todos.size - pending.size
                            }
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(if (count > 0) "${f.label} $count" else f.label) },
                            )
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf<String?>(null) + TODO_CATEGORIES).forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c ?: "모든 분류") },
                            )
                        }
                    }
                }
            }

            if (sections.isEmpty()) {
                item {
                    EmptyState(
                        if (filter == TodoFilter.DONE) "🗂️" else "🌤️",
                        if (filter == TodoFilter.DONE) "완료한 일이 없어요" else "할 일이 없어요",
                        "위 입력창에 적거나 ⚙ 버튼으로 마감일·알림까지 자세히 추가할 수 있어요.",
                    )
                }
            }
            sections.forEach { (title, list) ->
                item(key = "h_$title") {
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(
                            title,
                            color = if (title == "기한 지남") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("  ${list.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
                items(list, key = { it.id }) { todo ->
                    TodoRow(todo, today, onToggle = { toggle(todo) }, onClick = { editing = todo })
                }
            }
            if (filter == TodoFilter.DONE && visible.isNotEmpty()) {
                item {
                    TextButton(onClick = { scope.launch { dao.clearDone() } }) { Text("완료한 항목 모두 지우기") }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (creating || editing != null) {
        TodoEditorSheet(
            initial = editing ?: Todo(
                title = input.trim(),
                dueEpochDay = if (filter == TodoFilter.TODAY) t else null,
                category = category.orEmpty(),
            ),
            isNew = editing == null,
            onDismiss = { creating = false; editing = null },
            onSave = { todo ->
                scope.launch {
                    val saved = if (todo.id == 0L) todo.copy(id = dao.insert(todo)) else todo.also { dao.update(it) }
                }
                if (editing == null) input = ""
                creating = false
                editing = null
            },
            onDelete = { todo ->
                delete(todo)
                editing = null
            },
        )
    }
}

@Composable
private fun TodoRow(todo: Todo, today: LocalDate, onToggle: () -> Unit, onClick: () -> Unit) {
    val pColor = priorityColor(todo.priorityEnum)
    val dueDate = todo.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
    val overdue = !todo.done && dueDate != null && dueDate.isBefore(today)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.Top,
    ) {
        CheckCircle(todo.done, if (todo.done) MaterialTheme.colorScheme.outline else pColor, Modifier.padding(top = 1.dp), size = 24.dp, onClick = onToggle)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (todo.memo.isNotBlank()) {
                Text(todo.memo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val hasMeta = dueDate != null || todo.category.isNotBlank() || todo.priorityEnum == Priority.HIGH
            if (hasMeta) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (todo.priorityEnum == Priority.HIGH && !todo.done) Pill("중요", pColor)
                    if (todo.category.isNotBlank()) Pill(todo.category, MaterialTheme.colorScheme.tertiary)
                    if (dueDate != null) {
                        val c = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(Icons.Filled.Event, null, Modifier.size(14.dp), tint = c)
                        Text(formatDue(dueDate, todo.dueMinute, today), style = MaterialTheme.typography.labelMedium, color = c)
                        if (todo.remind && todo.dueMinute != null) Icon(Icons.Filled.Notifications, "알림", Modifier.size(14.dp), tint = c)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TodoEditorSheet(
    initial: Todo,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial.title) }
    var memo by remember { mutableStateOf(initial.memo) }
    var priority by remember { mutableStateOf(initial.priorityEnum) }
    var category by remember { mutableStateOf(initial.category) }
    var due by remember { mutableStateOf(initial.dueEpochDay?.let { LocalDate.ofEpochDay(it) }) }
    var dueMinute by remember { mutableStateOf(initial.dueMinute) }
    var remind by remember { mutableStateOf(initial.remind) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (isNew) "새 할 일" else "할 일 수정", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("할 일") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("중요도")
                SegmentedTabs(
                    options = Priority.entries.map { it.label },
                    selected = priority.ordinal,
                    onSelect = { priority = Priority.entries[it] },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("분류")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TODO_CATEGORIES.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = if (category == c) "" else c },
                            label = { Text(c) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("마감")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = due == today, onClick = { due = today }, label = { Text("오늘") })
                    FilterChip(selected = due == today.plusDays(1), onClick = { due = today.plusDays(1) }, label = { Text("내일") })
                    InputChip(
                        selected = due != null && due != today && due != today.plusDays(1),
                        onClick = { pickDate = true },
                        label = { Text(due?.takeIf { it != today && it != today.plusDays(1) }?.let { formatKoreanDate(it) } ?: "날짜 선택") },
                        leadingIcon = { Icon(Icons.Filled.Event, null, Modifier.size(18.dp)) },
                    )
                    if (due != null) {
                        InputChip(
                            selected = dueMinute != null,
                            onClick = { pickTime = true },
                            label = { Text(dueMinute?.let { formatMinute(it) } ?: "시간 추가") },
                            leadingIcon = { Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)) },
                            trailingIcon = if (dueMinute != null) {
                                { Icon(Icons.Filled.Close, "시간 지우기", Modifier.size(16.dp).clickable { dueMinute = null; remind = false }) }
                            } else null,
                        )
                        TextButton(onClick = { due = null; dueMinute = null; remind = false }) { Text("마감 없음") }
                    }
                }
                if (due != null && dueMinute != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("마감 시각에 알림 받기", Modifier.weight(1f).padding(start = 12.dp))
                        Switch(checked = remind, onCheckedChange = { remind = it })
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isNew) {
                    OutlinedButton(
                        onClick = { onDelete(initial) },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("삭제") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("취소") }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(
                            initial.copy(
                                title = title.trim(),
                                memo = memo.trim(),
                                priority = priority.ordinal,
                                category = category,
                                dueEpochDay = due?.toEpochDay(),
                                dueMinute = if (due != null) dueMinute else null,
                                remind = remind && due != null && dueMinute != null,
                            )
                        )
                    },
                ) { Text("저장") }
            }
        }
    }

    if (pickDate) DatePickDialog(due ?: today, onDismiss = { pickDate = false }) { due = it }
    if (pickTime) TimePickDialog(dueMinute ?: 9 * 60, onDismiss = { pickTime = false }) {
        dueMinute = it
        remind = true
    }
}
