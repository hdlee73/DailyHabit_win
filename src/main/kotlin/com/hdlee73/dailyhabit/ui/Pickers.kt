package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var selectedDay by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("날짜 선택") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        val prev = LocalDate.of(year, month, 1).minusMonths(1)
                        year = prev.year
                        month = prev.monthValue
                    }) { Text("◀") }
                    Text("$year. %02d".format(month), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = {
                        val next = LocalDate.of(year, month, 1).plusMonths(1)
                        year = next.year
                        month = next.monthValue
                    }) { Text("▶") }
                }

                val firstDay = LocalDate.of(year, month, 1)
                val daysInMonth = firstDay.lengthOfMonth()
                val startCol = firstDay.dayOfWeek.value % 7 // 일=0

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))

                var dayCount = 1
                for (row in 0..5) {
                    if (dayCount > daysInMonth) break
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        for (col in 0..6) {
                            if (row == 0 && col < startCol || dayCount > daysInMonth) {
                                Spacer(Modifier.width(36.dp))
                            } else {
                                val current = LocalDate.of(year, month, dayCount)
                                val isSelected = current == selectedDay
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { selectedDay = current },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "$dayCount",
                                            fontSize = 13.sp,
                                            color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                                dayCount++
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(selectedDay)
                onDismiss()
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
fun TimePickDialog(initialMinute: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var hour by remember { mutableIntStateOf(initialMinute / 60) }
    var minute by remember { mutableIntStateOf(initialMinute % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간 선택") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = "%02d".format(hour),
                    onValueChange = { hour = (it.toIntOrNull() ?: 0).coerceIn(0, 23) },
                    modifier = Modifier.width(70.dp),
                    label = { Text("시") },
                )
                Text(" : ", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedTextField(
                    value = "%02d".format(minute),
                    onValueChange = { minute = (it.toIntOrNull() ?: 0).coerceIn(0, 59) },
                    modifier = Modifier.width(70.dp),
                    label = { Text("분") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(hour * 60 + minute)
                onDismiss()
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
