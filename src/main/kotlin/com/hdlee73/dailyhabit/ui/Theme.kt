package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Claude 앱 느낌: 따뜻한 미색 바탕 + 테라코타 포인트 + 차분한 회색 카드
val Terracotta = Color(0xFFD97757)

private val Light = lightColorScheme(
    primary = Color(0xFFC6613F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E3DA),
    onPrimaryContainer = Color(0xFF4A1C0C),
    secondary = Color(0xFF8C6A3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E8D8),
    onSecondaryContainer = Color(0xFF33240E),
    tertiary = Color(0xFF5E6B8A),
    tertiaryContainer = Color(0xFFE6E9F1),
    onTertiaryContainer = Color(0xFF1A2236),
    background = Color(0xFFFAF9F5),
    onBackground = Color(0xFF1F1E1D),
    surface = Color(0xFFFAF9F5),
    onSurface = Color(0xFF1F1E1D),
    surfaceVariant = Color(0xFFEDEBE4),
    onSurfaceVariant = Color(0xFF73726C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F6F1),
    surfaceContainer = Color(0xFFF0EEE6),
    surfaceContainerHigh = Color(0xFFEAE8E0),
    surfaceContainerHighest = Color(0xFFE3E1D8),
    outline = Color(0xFFB5B3AC),
    outlineVariant = Color(0xFFE5E3DA),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE08A6B),
    onPrimary = Color(0xFF3B1407),
    primaryContainer = Color(0xFF5A2D1D),
    onPrimaryContainer = Color(0xFFF6E3DA),
    secondary = Color(0xFFD8B98A),
    onSecondary = Color(0xFF3A2A10),
    secondaryContainer = Color(0xFF4A3B24),
    onSecondaryContainer = Color(0xFFF1E8D8),
    tertiary = Color(0xFFB4C0DD),
    tertiaryContainer = Color(0xFF3A4560),
    onTertiaryContainer = Color(0xFFE6E9F1),
    background = Color(0xFF262624),
    onBackground = Color(0xFFF5F4EE),
    surface = Color(0xFF262624),
    onSurface = Color(0xFFF5F4EE),
    surfaceVariant = Color(0xFF3A3936),
    onSurfaceVariant = Color(0xFFB0AEA5),
    surfaceContainerLowest = Color(0xFF1F1E1D),
    surfaceContainerLow = Color(0xFF2B2A28),
    surfaceContainer = Color(0xFF30302E),
    surfaceContainerHigh = Color(0xFF393937),
    surfaceContainerHighest = Color(0xFF42413E),
    outline = Color(0xFF6E6C66),
    outlineVariant = Color(0xFF3A3936),
)

private val base = Typography()
private val AppTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

/** 복음·기도문용 본문 서체 */
val ScriptureStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 30.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DailyHabitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** 루틴 색상 */
val RoutinePalette = listOf(
    Color(0xFF5B5FC7), // 남색
    Color(0xFFE07A3F), // 주황
    Color(0xFF2E9E7A), // 초록
    Color(0xFFD9547A), // 장미
    Color(0xFF3A8FD1), // 하늘
    Color(0xFF9B59B6), // 보라
    Color(0xFFC9A227), // 금색
    Color(0xFF6D7A88), // 회청
)

fun routineColor(index: Int): Color = RoutinePalette[index.mod(RoutinePalette.size)]

/** 전례색 */
fun liturgicalColor(day: String): Color = when {
    day.startsWith("(백)") -> Color(0xFFD4AF37)
    day.startsWith("(홍)") -> Color(0xFFC62828)
    day.startsWith("(녹)") -> Color(0xFF2E7D32)
    day.startsWith("(자)") -> Color(0xFF6A1B9A)
    day.startsWith("(장미)") -> Color(0xFFE48FB1)
    day.startsWith("(흑)") -> Color(0xFF37474F)
    else -> Color(0xFFD4AF37)
}
