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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.SHOPPING_CATEGORIES
import com.hdlee73.dailyhabit.data.ShoppingItem
import kotlinx.coroutines.launch

@Composable
fun ShoppingScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    val scope = rememberCoroutineScope()
    val items by db.shoppingDao.observeAll().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("장보기", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "살 것 ${items.count { !it.checked }}개 · 완료 ${items.count { it.checked }}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (items.any { it.checked }) {
                        OutlinedButton(onClick = { scope.launch { db.shoppingDao.clearChecked() } }) {
                            Text("완료 항목 삭제")
                        }
                    }
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("품목 추가")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (items.isEmpty()) {
                EmptyState("🛒", "장보기 목록이 비어 있습니다", "사야 할 품목을 등록해 보세요!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        SoftCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircleCheck(
                                    checked = item.checked,
                                    onToggle = {
                                        scope.launch {
                                            db.shoppingDao.update(
                                                item.copy(
                                                    checked = !item.checked,
                                                    checkedAt = if (!item.checked) System.currentTimeMillis() else null,
                                                )
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                        ),
                                        color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (item.qty.isNotBlank() || item.category.isNotBlank()) {
                                        Text(
                                            listOf(item.category, item.qty).filter { it.isNotBlank() }.joinToString(" · "),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { scope.launch { db.shoppingDao.delete(item) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("기타") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("품목 추가") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("품목 이름 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("수량 / 메모 (예: 2개, 500g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                db.shoppingDao.insert(
                                    ShoppingItem(name = name.trim(), qty = qty.trim(), category = category)
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            },
        )
    }
}
