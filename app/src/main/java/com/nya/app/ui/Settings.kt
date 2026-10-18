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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.AppViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val prefs = LocalPrefs.current
    val app = viewModel<AppViewModel>(factory = AppViewModel.Factory)
    var notifs by remember { mutableStateOf(true) }
    val seg = RoundedCornerShape(16.dp)

    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Settings", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            XCard(Modifier.staggerIn(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c.b1, c.b2))),
                        contentAlignment = Alignment.Center
                    ) { Text(prefs.name.take(1), color = c.on, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clearAndSetSemantics {}) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(prefs.name, color = c.tx, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Free plan", color = c.sb, fontSize = 13.sp)
                    }
                }
            }

            XLabel("Appearance", Modifier.staggerIn(1))
            Row(
                Modifier.fillMaxWidth().staggerIn(2).clip(seg).background(c.sf).border(1.dp, c.ln, seg).padding(4.dp)
            ) {
                listOf("Auto", "Light", "Dark").forEachIndexed { i, label ->
                    val sel = prefs.theme == i
                    Box(
                        Modifier.weight(1f).pressScale(role = Role.RadioButton, selected = sel) { app.setTheme(i) }.clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.ac else Color.Transparent)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (sel) c.on else c.sb, fontSize = 14.sp) }
                }
            }

            XLabel("Preferences", Modifier.staggerIn(3))
            XCard(Modifier.staggerIn(4)) {
                XToggle("Notifications", "Memory and device alerts", notifs) { notifs = it }
                XDivider()
                XToggle("Quiet mode", "Pause voice replies", prefs.quiet) { app.setQuiet(it) }
            }

            XLabel("Account", Modifier.staggerIn(5))
            XCard(Modifier.staggerIn(6)) {
                XRow("Subscription", "Free") { SubNav.open("Subscription") }
                XDivider()
                XRow("Help & support") { SubNav.open("Help") }
                XDivider()
                XRow("Read-only mode preview") { SubNav.open("ReadOnly") }
                XDivider()
                XRow("Version", "0.1.0")
            }

            Spacer(Modifier.height(24.dp))
            XGhost("Sign out", { SubNav.signOut() }, Modifier.fillMaxWidth().staggerIn(7))
            Spacer(Modifier.height(40.dp))
        }
    }
}

