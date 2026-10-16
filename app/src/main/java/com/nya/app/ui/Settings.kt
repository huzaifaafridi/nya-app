package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    var notifs by remember { mutableStateOf(true) }
    var quiet by remember { mutableStateOf(false) }
    val seg = RoundedCornerShape(16.dp)

    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Settings", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            XCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c.b1, c.b2))),
                        contentAlignment = Alignment.Center
                    ) { Text("M", color = c.on, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Mercy", color = c.tx, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Free plan", color = c.sb, fontSize = 13.sp)
                    }
                }
            }

            XLabel("Appearance")
            Row(
                Modifier.fillMaxWidth().clip(seg).background(c.sf).border(1.dp, c.ln, seg).padding(4.dp)
            ) {
                listOf("Auto", "Light", "Dark").forEachIndexed { i, label ->
                    val sel = ThemePref.mode == i
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.ac else Color.Transparent)
                            .clickable { ThemePref.mode = i }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (sel) c.on else c.sb, fontSize = 14.sp) }
                }
            }

            XLabel("Preferences")
            XCard {
                XToggle("Notifications", "Memory and device alerts", notifs) { notifs = it }
                XDivider()
                XToggle("Quiet mode", "Pause voice replies", quiet) { quiet = it }
            }

            XLabel("Account")
            XCard {
                XRow("Subscription", "Free") { SubNav.open("Subscription") }
                XDivider()
                XRow("Help & support") { SubNav.open("Help") }
                XDivider()
                XRow("Read-only mode preview") { SubNav.open("ReadOnly") }
                XDivider()
                XRow("Version", "0.1.0")
            }

            Spacer(Modifier.height(24.dp))
            XGhost("Sign out", { SubNav.signOut() })
            Spacer(Modifier.height(40.dp))
        }
    }
}

