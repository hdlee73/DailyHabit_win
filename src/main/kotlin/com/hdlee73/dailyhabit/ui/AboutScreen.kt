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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.Updater
import java.awt.Desktop
import java.net.URI

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("앱 정보", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

            SoftCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Text("🌱", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("DailyHabit for Windows", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("버전 ${Updater.CURRENT_VERSION}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "매일의 습관, 할 일, 기도와 삶의 소중한 순간들을 정갈하게 가꾸어 나가는 윈도우 데스크톱 프로그램입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            SoftCard {
                SectionLabel("오픈소스 & 저장소")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("GitHub 저장소", fontWeight = FontWeight.Medium)
                    TextButton(onClick = {
                        runCatching { Desktop.getDesktop().browse(URI("https://github.com/${Updater.REPO}")) }
                    }) {
                        Text("github.com/${Updater.REPO} ↗")
                    }
                }
            }
        }
    }
}
