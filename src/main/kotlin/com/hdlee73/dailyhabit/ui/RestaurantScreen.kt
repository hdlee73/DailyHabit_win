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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.hdlee73.dailyhabit.data.RESTAURANT_CATEGORIES
import com.hdlee73.dailyhabit.data.Restaurant
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

@Composable
fun RestaurantScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    val scope = rememberCoroutineScope()
    val places by db.restaurantDao.observeAll().collectAsState(initial = emptyList())

    var selectedCat by remember { mutableStateOf<String?>(null) }
    var wishFilter by remember { mutableStateOf(false) }

    var editingPlace by remember { mutableStateOf<Restaurant?>(null) }
    var isNewDialog by remember { mutableStateOf(false) }

    val filtered = remember(places, selectedCat, wishFilter) {
        places.filter { p ->
            (selectedCat == null || p.category == selectedCat) &&
                (!wishFilter || p.wish)
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
                    Text("맛집", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "총 ${places.size}곳 (가본 곳 ${places.count { !it.wish }} · 가보고 싶은 곳 ${places.count { it.wish }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = {
                    editingPlace = Restaurant(name = "")
                    isNewDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("식당 등록")
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
                    selected = selectedCat == null,
                    onClick = { selectedCat = null },
                    label = { Text("전체") },
                )
                RESTAURANT_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = selectedCat == cat,
                        onClick = { selectedCat = if (selectedCat == cat) null else cat },
                        label = { Text(cat) },
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = wishFilter,
                    onClick = { wishFilter = !wishFilter },
                    label = { Text("가보고 싶은 곳만") },
                )
            }

            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                EmptyState("🍽️", "등록된 맛집이 없습니다", "좋아하는 식당이나 가보고 싶은 장소를 기록해 두세요!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { place ->
                        SoftCard(onClick = {
                            editingPlace = place
                            isNewDialog = false
                        }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            place.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        )
                                        if (place.category.isNotBlank()) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                                Text(place.category, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        if (place.wish) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                                Text("가보고 싶어요", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    if (place.rating > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        Row {
                                            repeat(place.rating) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    if (place.menu.isNotBlank() || place.address.isNotBlank() || place.comment.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        if (place.menu.isNotBlank()) Text("메뉴: ${place.menu}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (place.address.isNotBlank()) Text("위치: ${place.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (place.comment.isNotBlank()) Text("“${place.comment}”", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (place.link.isNotBlank()) {
                                    IconButton(onClick = {
                                        runCatching { Desktop.getDesktop().browse(URI(place.link)) }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "링크 열기")
                                    }
                                }
                                IconButton(onClick = {
                                    scope.launch { db.restaurantDao.delete(place) }
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

    if (editingPlace != null) {
        RestaurantEditDialog(
            initial = editingPlace!!,
            isNew = isNewDialog,
            onDismiss = { editingPlace = null },
            onSave = { saved ->
                scope.launch {
                    if (isNewDialog) {
                        db.restaurantDao.insert(saved)
                    } else {
                        db.restaurantDao.update(saved)
                    }
                    editingPlace = null
                }
            },
        )
    }
}

@Composable
private fun RestaurantEditDialog(
    initial: Restaurant,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Restaurant) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var menu by remember { mutableStateOf(initial.menu) }
    var address by remember { mutableStateOf(initial.address) }
    var link by remember { mutableStateOf(initial.link) }
    var rating by remember { mutableIntStateOf(initial.rating) }
    var comment by remember { mutableStateOf(initial.comment) }
    var wish by remember { mutableStateOf(initial.wish) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "식당 등록" else "식당 정보 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("식당 이름 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    RESTAURANT_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = if (category == cat) "" else cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = menu,
                    onValueChange = { menu = it },
                    label = { Text("대표 메뉴") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("위치 / 동네") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("지도 / 블로그 링크") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("한 줄 평") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("별점: ", fontSize = 13.sp)
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = if (rating == star) 0 else star }) {
                            Icon(
                                if (star <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (star <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = wish, onCheckedChange = { wish = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (wish) "가보고 싶은 곳 (위시리스트)" else "이미 가본 곳", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                category = category,
                                menu = menu.trim(),
                                address = address.trim(),
                                link = link.trim(),
                                rating = rating,
                                comment = comment.trim(),
                                wish = wish,
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
