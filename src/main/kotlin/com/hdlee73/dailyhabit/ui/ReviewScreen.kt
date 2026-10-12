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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.BookStatus
import com.hdlee73.dailyhabit.data.DIARY_MOODS
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ReviewScreen(modifier: Modifier = Modifier) {
    val db = remember { AppDatabase.get() }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val diary by db.diaryDao.observeAll().collectAsState(initial = emptyList())
    val routines by db.routineDao.observeActive().collectAsState(initial = emptyList())
    val checks by db.routineDao.observeChecks().collectAsState(initial = emptyList())
    val books by db.bookDao.observeBooks().collectAsState(initial = emptyList())
    val places by db.restaurantDao.observeAll().collectAsState(initial = emptyList())
    val visits by db.visitDao.observeAll().collectAsState(initial = emptyList())

    val monthDiary = remember(diary, currentMonth) {
        diary.filter { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == currentMonth }
    }
    val monthVisits = remember(visits, currentMonth) {
        visits.filter { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == currentMonth }
    }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 헤더 & 월 선택
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("월간 돌아보기", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("${currentMonth.year}년 ${currentMonth.monthValue}월 활동 요약", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달")
                    }
                    Text("${currentMonth.year}. %02d".format(currentMonth.monthValue), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달")
                    }
                }
            }

            // 요약 그리드
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SoftCard(modifier = Modifier.weight(1f)) {
                    Text("작성한 일기", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("${monthDiary.size}일", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                SoftCard(modifier = Modifier.weight(1f)) {
                    Text("맛집 방문", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("${monthVisits.size}회", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                SoftCard(modifier = Modifier.weight(1f)) {
                    val doneBooks = books.count { it.statusEnum == BookStatus.DONE }
                    Text("완독한 책", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("${doneBooks}권", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
            }

            // 루틴 달성 현황
            SoftCard {
                SectionLabel("이달의 루틴 달성 현황")
                Spacer(Modifier.height(12.dp))
                if (routines.isEmpty()) {
                    Text("등록된 루틴이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        routines.forEach { r ->
                            val rChecks = checks.filter { it.routineId == r.id && YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == currentMonth }
                            val daysInMonth = currentMonth.lengthOfMonth()
                            val rate = (rChecks.size.toFloat() / daysInMonth).coerceIn(0f, 1f)

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${r.emoji} ${r.title}", fontWeight = FontWeight.Medium)
                                    Text("${rChecks.size}회 달성", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(progress = { rate }, modifier = Modifier.fillMaxWidth().height(6.dp))
                            }
                        }
                    }
                }
            }

            // 기분 통계
            if (monthDiary.isNotEmpty()) {
                SoftCard {
                    SectionLabel("이달의 기분 분포")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        DIARY_MOODS.forEachIndexed { idx, emoji ->
                            val count = monthDiary.count { it.mood == idx + 1 }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 24.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("${count}회", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
