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
import androidx.compose.material.icons.filled.Star
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
import com.hdlee73.dailyhabit.data.Book
import com.hdlee73.dailyhabit.data.BookStatus
import kotlinx.coroutines.launch

@Composable
fun BooksScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    val scope = rememberCoroutineScope()
    val books by db.bookDao.observeBooks().collectAsState(initial = emptyList())

    var statusFilter by remember { mutableStateOf<BookStatus?>(null) }
    var editingBook by remember { mutableStateOf<Book?>(null) }
    var isNewDialog by remember { mutableStateOf(false) }

    val filtered = remember(books, statusFilter) {
        if (statusFilter == null) books else books.filter { it.statusEnum == statusFilter }
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
                    Text("독서", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "읽는 중 ${books.count { it.statusEnum == BookStatus.READING }} · 완독 ${books.count { it.statusEnum == BookStatus.DONE }} · 읽고 싶은 ${books.count { it.statusEnum == BookStatus.WANT }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = {
                    editingBook = Book(title = "")
                    isNewDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("책 등록")
                }
            }

            Spacer(Modifier.height(16.dp))

            // 상태 필터
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("전체") },
                )
                BookStatus.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = if (statusFilter == status) null else status },
                        label = { Text(status.label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                EmptyState("📚", "등록된 책이 없습니다", "읽고 있는 책이나 읽고 싶은 책을 등록해 보세요!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { book ->
                        SoftCard(onClick = {
                            editingBook = book
                            isNewDialog = false
                        }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            book.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        )
                                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(book.statusEnum.label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (book.author.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(book.author, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (book.totalPages > 0) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            LinearProgressIndicator(
                                                progress = { book.progress },
                                                modifier = Modifier.width(120.dp).height(6.dp),
                                            )
                                            Text("${book.currentPage} / ${book.totalPages}p (${(book.progress * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (book.review.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text("“${book.review}”", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = {
                                    scope.launch { db.bookDao.delete(book.id) }
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

    if (editingBook != null) {
        BookEditDialog(
            initial = editingBook!!,
            isNew = isNewDialog,
            onDismiss = { editingBook = null },
            onSave = { saved ->
                scope.launch {
                    if (isNewDialog) {
                        db.bookDao.insert(saved)
                    } else {
                        db.bookDao.update(saved)
                    }
                    editingBook = null
                }
            },
        )
    }
}

@Composable
private fun BookEditDialog(
    initial: Book,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Book) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var author by remember { mutableStateOf(initial.author) }
    var totalPages by remember { mutableIntStateOf(initial.totalPages) }
    var currentPage by remember { mutableIntStateOf(initial.currentPage) }
    var status by remember { mutableIntStateOf(initial.status) }
    var review by remember { mutableStateOf(initial.review) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "책 등록" else "책 정보 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("책 제목 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("지은이") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st.ordinal,
                            onClick = { status = st.ordinal },
                            label = { Text(st.label) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (currentPage > 0) currentPage.toString() else "",
                        onValueChange = { currentPage = it.toIntOrNull() ?: 0 },
                        label = { Text("현재 쪽수") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = if (totalPages > 0) totalPages.toString() else "",
                        onValueChange = { totalPages = it.toIntOrNull() ?: 0 },
                        label = { Text("전체 쪽수") },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("한 줄 소감") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            initial.copy(
                                title = title.trim(),
                                author = author.trim(),
                                totalPages = totalPages,
                                currentPage = currentPage,
                                status = status,
                                review = review.trim(),
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
}
