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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.FaithRoutines
import com.hdlee73.dailyhabit.data.PrayerItem
import com.hdlee73.dailyhabit.data.Prayers
import java.time.LocalDate

@Composable
fun PrayerScreen(modifier: Modifier = Modifier) {
    var openId by remember { mutableStateOf<String?>(null) }
    val open = openId?.let { Prayers.byId(it) }

    if (open != null) {
        PrayerDetail(open, onBack = { openId = null }, modifier)
    } else {
        PrayerList(onOpen = { openId = it.id }, modifier)
    }
}

@Composable
private fun PrayerList(onOpen: (PrayerItem) -> Unit, modifier: Modifier = Modifier) {
    val items = Prayers.all
    val today = remember { LocalDate.now() }
    val mystery = remember(today) { Prayers.mysteryFor(today) }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("기도", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("가톨릭 주요 기도문 및 오늘의 신비", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 오늘의 묵주기도 신비 배너
                item {
                    SoftCard(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        onClick = { onOpen(Prayers.byId(Prayers.ROSARY_ID)!!) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📿", fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("오늘의 묵주기도 (${mystery.days})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(mystery.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }

                items(Prayers.categories) { cat ->
                    val catItems = items.filter { it.category == cat }
                    if (catItems.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        SectionLabel(cat)
                        Spacer(Modifier.height(4.dp))
                        catItems.forEach { p ->
                            SoftCard(onClick = { onOpen(p) }, modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(p.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerDetail(p: PrayerItem, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var fontScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(p) {
        FaithRoutines.markDone(FaithRoutines.KIND_PRAYER)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Row {
                IconButton(onClick = { if (fontScale > 0.8f) fontScale -= 0.1f }) {
                    Icon(Icons.Default.TextDecrease, contentDescription = "글자 축소")
                }
                IconButton(onClick = { if (fontScale < 1.6f) fontScale += 0.1f }) {
                    Icon(Icons.Default.TextIncrease, contentDescription = "글자 확대")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SoftCard {
            Text(p.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            if (p.id == Prayers.ROSARY_ID) {
                RosaryDetailContent(fontScale)
            } else {
                Text(
                    p.text.trimIndent(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (16 * fontScale).sp,
                        lineHeight = (28 * fontScale).sp,
                    ),
                )
            }

            if (p.note.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    p.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RosaryDetailContent(scale: Float) {
    val today = remember { LocalDate.now() }
    val mystery = remember(today) { Prayers.mysteryFor(today) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("【오늘의 신비: ${mystery.name}】", fontWeight = FontWeight.Bold, fontSize = (18 * scale).sp, color = MaterialTheme.colorScheme.primary)
        mystery.decades.forEachIndexed { i, d ->
            Text("제${i + 1}단: $d", fontSize = (15 * scale).sp, lineHeight = (24 * scale).sp)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("【묵주기도 바치는 순서】", fontWeight = FontWeight.Bold, fontSize = (16 * scale).sp)
        Prayers.rosaryOrder.forEachIndexed { idx, step ->
            Text("${idx + 1}. $step", fontSize = (14 * scale).sp, lineHeight = (22 * scale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
