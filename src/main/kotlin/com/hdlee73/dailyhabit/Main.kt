package com.hdlee73.dailyhabit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.FaithRoutines
import com.hdlee73.dailyhabit.data.Updater
import com.hdlee73.dailyhabit.notify.Notifier
import com.hdlee73.dailyhabit.notify.Scheduler
import com.hdlee73.dailyhabit.ui.AboutScreen
import com.hdlee73.dailyhabit.ui.AppDrawer
import com.hdlee73.dailyhabit.ui.BooksScreen
import com.hdlee73.dailyhabit.ui.DailyHabitTheme
import com.hdlee73.dailyhabit.ui.Desktop
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
import com.hdlee73.dailyhabit.ui.icon
import kotlinx.coroutines.delay
import java.time.LocalDate

fun main(args: Array<String>) {
    // 이미 켜져 있으면 그 창을 앞으로 불러오고 끝낸다 (자동 실행 + 바로 가기로 두 번 켜는 경우)
    if (!SingleInstance.acquire()) return
    val startHidden = "--tray" in args

    application {
        var visible by remember { mutableStateOf(!startHidden) }
        val trayState = rememberTrayState()
        val windowState = rememberWindowState(size = DpSize(1180.dp, 820.dp), position = WindowPosition(Alignment.Center))
        val appIcon = painterResource("icon.png")

        LaunchedEffect(Unit) {
            Notifier.tray = trayState
            SingleInstance.onShowRequest = { visible = true }
            FaithRoutines.seed()
            Scheduler.start()
        }

        Tray(
            icon = appIcon,
            state = trayState,
            tooltip = BuildConfig.APP_NAME,
            onAction = { visible = true },
            menu = {
                Item("열기", onClick = { visible = true })
                Separator()
                Item("종료", onClick = ::exitApplication)
            },
        )

        if (visible) {
            Window(
                onCloseRequest = {
                    // 트레이에 남아서 알림을 계속 보낸다 (설정에서 끌 수 있음)
                    if (AppSettings.default.keepInTray) visible = false else exitApplication()
                },
                state = windowState,
                title = BuildConfig.APP_NAME,
                icon = appIcon,
            ) {
                LaunchedEffect(Unit) {
                    Desktop.window = window
                    window.minimumSize = java.awt.Dimension(720, 560)
                    window.toFront()
                }
                DailyHabitTheme { MainApp() }
            }
        }
    }
}

/** 창을 닫았다 다시 열어도 유지되는 화면 상태 */
internal object MainState {
    var section by mutableStateOf(Section.HOME)
    var drawerOpen by mutableStateOf(true)
    var updatePromptShown = false
}

@Composable
fun MainApp() {
    val section = MainState.section
    /** 메뉴의 '새로 만들기'로 편집 화면을 열어야 하는 섹션 */
    var createIn by remember { mutableStateOf<Section?>(null) }

    // 알림으로 창을 열면 그 알림의 화면으로
    LaunchedEffect(Notifier.pendingSection) {
        Notifier.pendingSection?.let { MainState.section = it; Notifier.pendingSection = null }
    }

    val db = remember { AppDatabase.get() }
    val todos by db.todoDao.observeAll().collectAsState(initial = emptyList())
    val routines by db.routineDao.observeActive().collectAsState(initial = emptyList())
    val checks by db.routineDao.observeChecks().collectAsState(initial = emptyList())
    val places by db.restaurantDao.observeAll().collectAsState(initial = emptyList())
    val diary by db.diaryDao.observeAll().collectAsState(initial = emptyList())
    val books by db.bookDao.observeBooks().collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val todaysRoutines = routines.filter { it.activeOn(today.dayOfWeek) }
    val badges = mapOf(
        Section.TODO to todos.count { !it.done }.takeIf { it > 0 }?.toString().orEmpty(),
        Section.ROUTINE to if (todaysRoutines.isEmpty()) "" else
            "${todaysRoutines.count { r -> checks.any { it.routineId == r.id && it.epochDay == today.toEpochDay() } }}/${todaysRoutines.size}",
        Section.RESTAURANT to places.size.takeIf { it > 0 }?.toString().orEmpty(),
        Section.JOURNAL to if (diary.any { it.epochDay == today.toEpochDay() }) "✓" else "",
        Section.BOOKS to books.count { it.status == 0 }.takeIf { it > 0 }?.toString().orEmpty(),
        Section.ABOUT to if (Updater.hasUpdate) "NEW" else "",
    )

    // 앱을 열 때마다 새 버전을 확인하고, 있으면 설치할 때까지 안내한다
    var updatePrompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Updater.autoCheck()
        if (Updater.hasUpdate && !MainState.updatePromptShown) {
            MainState.updatePromptShown = true
            updatePrompt = true
        }
    }
    val pendingUpdate = Updater.available
    if (updatePrompt && Updater.hasUpdate && pendingUpdate != null && section != Section.ABOUT) {
        AlertDialog(
            onDismissRequest = { updatePrompt = false },
            title = { Text("새 버전이 있어요") },
            text = { Text("v${pendingUpdate.version}이 나왔어요 (지금 v${BuildConfig.VERSION_NAME}).\n설치할 때까지 앱을 열 때마다 알려드려요.") },
            confirmButton = { TextButton(onClick = { updatePrompt = false; MainState.section = Section.ABOUT }) { Text("업데이트") } },
            dismissButton = { TextButton(onClick = { updatePrompt = false }) { Text("나중에") } },
        )
    }

    fun go(s: Section) {
        MainState.section = s
    }

    // 오늘의 말씀 화면을 실제로 보고 있으면 '말씀 읽기' 루틴을 체크한다
    LaunchedEffect(section) {
        if (section == Section.GOSPEL) FaithRoutines.markDone(FaithRoutines.KIND_GOSPEL)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                // 왼쪽 메뉴 (Claude 앱 사이드바). 넓은 화면이라 항상 펼쳐 두고, 상단 메뉴 버튼으로 접는다
                AnimatedVisibility(
                    visible = MainState.drawerOpen,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    AppDrawer(
                        current = section,
                        badges = badges,
                        onSelect = ::go,
                        onCreate = { s -> createIn = s; go(s) },
                    )
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    // 상단: 왼쪽 메뉴 버튼, 가운데 아이콘 + 화면 이름
                    Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
                        IconButton(
                            onClick = { MainState.drawerOpen = !MainState.drawerOpen },
                            modifier = Modifier.align(Alignment.CenterStart),
                        ) { Icon(Icons.Filled.Menu, contentDescription = if (MainState.drawerOpen) "메뉴 접기" else "메뉴 열기") }
                        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                            Icon(section.icon(), null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(section.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    AnimatedContent(
                        targetState = section,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        label = "section",
                    ) { s ->
                        // 휴대폰 화면처럼 읽기 좋은 폭으로 가운데에
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.fillMaxHeight().widthIn(max = 760.dp).fillMaxWidth()) {
                                when (s) {
                                    Section.GOSPEL -> TodayScreen(onNavigate = ::go)
                                    Section.QUOTE -> QuoteScreen(onNavigate = ::go)
                                    Section.PRAYER -> PrayerScreen()
                                    Section.SCHEDULE -> ScheduleScreen()
                                    Section.TODO -> TodoScreen(
                                        openEditor = createIn == Section.TODO,
                                        onEditorOpened = { createIn = null },
                                    )
                                    Section.ROUTINE -> RoutineScreen(
                                        openEditor = createIn == Section.ROUTINE,
                                        onEditorOpened = { createIn = null },
                                    )
                                    Section.RESTAURANT -> RestaurantScreen(
                                        openEditor = createIn == Section.RESTAURANT,
                                        onEditorOpened = { createIn = null },
                                    )
                                    Section.JOURNAL -> JournalScreen(
                                        openEditor = createIn == Section.JOURNAL,
                                        onEditorOpened = { createIn = null },
                                    )
                                    Section.SHOPPING -> ShoppingScreen(
                                        openEditor = createIn == Section.SHOPPING,
                                        onEditorOpened = { createIn = null },
                                    )
                                    Section.BOOKS -> BooksScreen(
                                        openEditor = createIn == Section.BOOKS,
                                        onEditorOpened = { createIn = null },
                                    )
                                    Section.ABOUT -> AboutScreen()
                                    Section.REVIEW -> ReviewScreen()
                                    Section.SETTINGS -> SettingsScreen(onNavigate = ::go)
                                }
                            }
                        }
                    }
                }
            }
            ToastHost(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
        }
    }
}

/** 안드로이드 토스트처럼 아래에 잠깐 보이는 안내 */
@Composable
private fun ToastHost(modifier: Modifier) {
    val current = Desktop.toast
    LaunchedEffect(current) {
        if (current != null) {
            delay(2500)
            if (Desktop.toast == current) Desktop.toast = null
        }
    }
    AnimatedVisibility(current != null, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 4.dp,
        ) {
            Text(current?.first.orEmpty(), Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
