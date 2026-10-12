package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun Section.icon(): ImageVector = when (this) {
    Section.GOSPEL -> Icons.AutoMirrored.Outlined.MenuBook
    Section.PRAYER -> Icons.Outlined.VolunteerActivism
    Section.QUOTE -> Icons.Outlined.FormatQuote
    Section.SCHEDULE -> Icons.Outlined.CalendarMonth
    Section.TODO -> Icons.Outlined.CheckCircle
    Section.ROUTINE -> Icons.Outlined.Repeat
    Section.JOURNAL -> Icons.Outlined.EditNote
    Section.RESTAURANT -> Icons.Outlined.RestaurantMenu
    Section.SHOPPING -> Icons.Outlined.ShoppingCart
    Section.BOOKS -> Icons.Outlined.AutoStories
    Section.REVIEW -> Icons.Outlined.Insights
    Section.SETTINGS -> Icons.Outlined.Settings
    Section.ABOUT -> Icons.Outlined.Info
}

@Composable
fun AppSidebar(
    currentSection: Section,
    onSelectSection: (Section) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 앱 로고 & 타이틀
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🌱", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("DailyHabit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Desktop Edition", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Primary 그룹 (말씀 / 명언 등)
            Section.primary.forEach { s ->
                SidebarItem(section = s, isSelected = s == currentSection, onClick = { onSelectSection(s) })
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Text("하루 관리", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            Spacer(Modifier.height(4.dp))

            // Daily 그룹 (일정, 할일, 루틴, 일기, 맛집, 독서, 회고)
            Section.daily.forEach { s ->
                SidebarItem(section = s, isSelected = s == currentSection, onClick = { onSelectSection(s) })
            }

            // 장보기 (메뉴에 표시)
            SidebarItem(section = Section.SHOPPING, isSelected = Section.SHOPPING == currentSection, onClick = { onSelectSection(Section.SHOPPING) })

            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // 설정 & 앱 정보
            SidebarItem(section = Section.SETTINGS, isSelected = Section.SETTINGS == currentSection, onClick = { onSelectSection(Section.SETTINGS) })
            SidebarItem(section = Section.ABOUT, isSelected = Section.ABOUT == currentSection, onClick = { onSelectSection(Section.ABOUT) })
        }
    }
}

@Composable
private fun SidebarItem(
    section: Section,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            section.icon(),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            section.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = contentColor,
        )
    }
}
