package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.CalendarEvent
import com.hdlee73.dailyhabit.data.CalendarRepository
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val DAYS = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val repo = remember { CalendarRepository() }
    var granted by remember { mutableStateOf(repo.hasPermission()) }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var grouped by remember { mutableStateOf<Map<LocalDate, List<CalendarEvent>>>(emptyMap()) }
    val today = LocalDate.now()

    LaunchedEffect(granted, reload) {
        if (!granted) return@LaunchedEffect
        grouped = repo.groupByDay(repo.events(today, DAYS), today, DAYS)
        loading = false
        refreshing = false
    }

    if (!granted) {
        Column(
            modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyState("📅", "캘린더를 연결해 주세요", "구글 캘린더 일정을 보여드리려면 캘린더의 ‘비공개 주소(iCal 형식)’가 필요해요.")
            CalendarConnect(onConnected = { granted = true; reload++ })
        }
        return
    }

    if (loading) {
        Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val total = grouped.values.flatten().distinctBy { it.id to it.start }.size
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { refreshing = true; reload++ },
        modifier = modifier.fillMaxSize(),
    ) {
        TopLazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 4.dp)) {
                    SectionLabel("앞으로 7일", Modifier.weight(1f))
                    Text("일정 ${total}개", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { refreshing = true; reload++ }, modifier = Modifier.size(32.dp).padding(start = 8.dp)) {
                        Icon(Icons.Filled.Refresh, "새로고침", Modifier.size(18.dp))
                    }
                }
            }
            items(grouped.entries.toList(), key = { it.key.toEpochDay() }) { (day, events) ->
                DayCard(day, events, today)
            }
            item {
                Text(
                    "구글 캘린더에서 바꾼 일정은 몇 분 뒤에 반영돼요. 캘린더를 더 넣거나 빼려면 설정 > 구글 캘린더 연결에서 바꾸세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DayCard(day: LocalDate, events: List<CalendarEvent>, today: LocalDate) {
    val isToday = day == today
    val weekend = day.dayOfWeek.value >= 6
    SoftCard(color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(verticalAlignment = Alignment.Top) {
            // 날짜 기둥
            Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        day.dayOfWeek.value == 7 -> Color(0xFFD9534F)
                        day.dayOfWeek.value == 6 -> Color(0xFF3A8FD1)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${day.dayOfMonth}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                val rel = relativeDayLabel(day, today)
                if (rel.isNotEmpty() && !isToday) {
                    Text(rel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) {
                    Text(
                        if (weekend) "여유로운 하루예요" else "일정 없음",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                events.forEach { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                            .clickable {
                                Desktop.openUrl(e.url.ifBlank { "https://calendar.google.com/calendar/r/day/${day.year}/${day.monthValue}/${day.dayOfMonth}" })
                            },
                    ) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(if (e.color != 0) Color(e.color) else MaterialTheme.colorScheme.primary)
                        )
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val meta = listOf(formatEventTime(e, day), e.location).filter { it.isNotBlank() }.joinToString(" · ")
                            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** 구글 캘린더 비공개 iCal 주소 넣기 (설정 화면에서도 사용) */
@Composable
fun CalendarConnect(onConnected: () -> Unit, modifier: Modifier = Modifier) {
    var url by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "구글 캘린더(웹) > 설정 > 내 캘린더 설정에서 캘린더를 고른 뒤 ‘캘린더 통합’의 ‘iCal 형식의 비공개 주소’를 복사해 붙여 넣으세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim() },
            label = { Text("https://calendar.google.com/calendar/ical/…/basic.ics") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { Desktop.openUrl("https://calendar.google.com/calendar/r/settings") }) { Text("구글 캘린더 설정 열기") }
            Spacer(Modifier.weight(1f))
            Button(
                enabled = url.startsWith("http", ignoreCase = true) || url.startsWith("webcal", ignoreCase = true),
                onClick = {
                    val s = AppSettings.default
                    s.calendarUrls = (s.calendarUrls + url).distinct()
                    url = ""
                    onConnected()
                },
            ) { Text("연결") }
        }
    }
}
