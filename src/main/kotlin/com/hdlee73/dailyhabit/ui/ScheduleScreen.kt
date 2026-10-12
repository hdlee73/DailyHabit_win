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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.CalendarEvent
import com.hdlee73.dailyhabit.data.CalendarRepository
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val repo = remember { CalendarRepository() }
    val today = remember { LocalDate.now() }
    var grouped by remember { mutableStateOf<Map<LocalDate, List<CalendarEvent>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        grouped = repo.groupByDay(repo.events(today, 7), today, 7)
    }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("일정", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("앞으로 7일간의 마감 일정 및 할 일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(grouped.entries.toList(), key = { it.key.toEpochDay() }) { (day, events) ->
                    val isToday = day == today
                    SoftCard(
                        color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Text("${day.monthValue}.${day.dayOfMonth}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                if (events.isEmpty()) {
                                    Text("예정된 일정이 없습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    events.forEach { e ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                if (e.allDay) "종일" else "%02d:%02d".format(e.start.hour, e.start.minute),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(48.dp),
                                            )
                                            Text(e.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            if (e.location.isNotBlank()) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                                    Text(e.location, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
