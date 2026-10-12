package com.hdlee73.dailyhabit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.FaithRoutines
import com.hdlee73.dailyhabit.ui.AboutScreen
import com.hdlee73.dailyhabit.ui.AppSidebar
import com.hdlee73.dailyhabit.ui.BooksScreen
import com.hdlee73.dailyhabit.ui.DailyHabitTheme
import com.hdlee73.dailyhabit.ui.JournalScreen
import com.hdlee73.dailyhabit.ui.PrayerScreen
import com.hdlee73.dailyhabit.ui.QuoteScreen
import com.hdlee73.dailyhabit.ui.RestaurantScreen
import com.hdlee73.dailyhabit.ui.ReviewScreen
import com.hdlee73.dailyhabit.ui.RoutineScreen
import com.hdlee73.dailyhabit.ui.ScheduleScreen
import com.hdlee73.dailyhabit.ui.Section
import com.hdlee73.dailyhabit.ui.SettingsScreen
import com.hdlee73.dailyhabit.ui.ShoppingScreen
import com.hdlee73.dailyhabit.ui.TodayScreen
import com.hdlee73.dailyhabit.ui.TodoScreen

fun main() = application {
    val windowState = rememberWindowState(width = 1100.dp, height = 780.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "DailyHabit - Habit, Todo & Life Manager",
    ) {
        DailyHabitTheme {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    var currentSection by remember { mutableStateOf(Section.HOME) }
    var refreshSignal by remember { mutableStateOf(0) }

    LaunchedEffect(refreshSignal) {
        FaithRoutines.seed()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 좌측 사이드바
            AppSidebar(
                currentSection = currentSection,
                onSelectSection = { currentSection = it },
            )

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 메인 콘텐츠 영역
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { section ->
                    when (section) {
                        Section.GOSPEL -> TodayScreen(onNavigate = { currentSection = it })
                        Section.QUOTE -> QuoteScreen(onNavigate = { currentSection = it })
                        Section.PRAYER -> PrayerScreen()
                        Section.SCHEDULE -> ScheduleScreen()
                        Section.TODO -> TodoScreen()
                        Section.ROUTINE -> RoutineScreen()
                        Section.JOURNAL -> JournalScreen()
                        Section.RESTAURANT -> RestaurantScreen()
                        Section.SHOPPING -> ShoppingScreen()
                        Section.BOOKS -> BooksScreen()
                        Section.REVIEW -> ReviewScreen()
                        Section.SETTINGS -> SettingsScreen(onSettingsChanged = {
                            refreshSignal++
                            if (currentSection == Section.GOSPEL && !AppSettings.default.isCatholic) {
                                currentSection = Section.QUOTE
                            } else if (currentSection == Section.QUOTE && AppSettings.default.isCatholic) {
                                currentSection = Section.GOSPEL
                            }
                        })
                        Section.ABOUT -> AboutScreen()
                    }
                }
            }
        }
    }
}
