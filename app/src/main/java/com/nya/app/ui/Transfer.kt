package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import kotlinx.coroutines.delay

@Composable
fun TransferScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val names = listOf("Sunset at the pier", "Coffee with Ali", "Morning walk", "Late night idea")
    val sizes = listOf("24 MB", "88 MB", "41 MB", "6 MB")
    val progress = remember { mutableStateListOf(1f, 0.6f, 0f, 0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            val i = progress.indexOfFirst { it < 1f }
            if (i == -1) break
            progress[i] = minOf(1f, progress[i] + 0.03f)
        }
    }
    val done = progress.count { it >= 1f }

    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Transfer", onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                if (done == names.size) "All synced" else "Syncing from NYA F1",
                color = c.tx, fontSize = 22.sp, fontWeight = FontWeight.SemiBold
            )
            Text("$done of ${names.size} done", color = c.sb, fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

            names.forEachIndexed { i, n ->
                XCard(Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(n, color = c.tx, fontSize = 15.sp)
                            Text(sizes[i], color = c.sb, fontSize = 12.sp)
                        }
                        Text(
                            if (progress[i] >= 1f) "✓" else "${(progress[i] * 100).toInt()}%",
                            color = if (progress[i] >= 1f) NyaGreen else c.sb, fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    XBar(progress[i])
                }
            }
        }
    }
}

