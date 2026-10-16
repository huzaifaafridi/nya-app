package com.nya.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

object Nav { var toChat: () -> Unit = {} }
var savedTab: Tab = Tab.Home

enum class Tab(val label: String) { Home("Home"), Capture("Capture"), Memories("Memories"), Device("Device") }

fun scene(a: Long, b: Long) = Brush.verticalGradient(listOf(Color(a), Color(b)))

@Composable
fun MainTabs() {
    val c = LocalNya.current
    var tab by remember { mutableStateOf(savedTab) }
    Box(Modifier.fillMaxSize().nyaBg()) {
        Crossfade(tab, animationSpec = tween(250), label = "tab") { t ->
            when (t) {
                Tab.Home -> HomeScreen()
                else -> when (t.label) {
                    "Memories" -> MemoriesScreen()
                    "Device" -> DeviceScreen()
                    "Capture" -> CaptureScreen()
                    else -> ComingSoon(t.label)
                }
            }
        }
        NavBar(tab, { tab = it; savedTab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun NavBar(tab: Tab, onTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(34.dp)
    Row(
        modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .height(68.dp).clip(shape).background(c.sf).border(1.dp, c.ln, shape),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab.values().forEach { t ->
            val col = if (t == tab) c.ac else c.sb
            val icon = when (t.label) {
                "Home" -> NyaIcons.Home
                "Capture" -> NyaIcons.Capture
                "Memories" -> NyaIcons.Grid
                else -> NyaIcons.Device
            }
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).clickable { onTab(t) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NyaIcon(icon, tint = col)
                Text(t.label, color = col, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
fun ComingSoon(name: String) {
    val c = LocalNya.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name (coming next)", color = c.sb, fontSize = 16.sp)
    }
}

@Composable
fun HomeScreen() {
    val c = LocalNya.current
    var mode by remember { mutableStateOf(2) }
    var quiet by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Good morning", color = c.sb, fontSize = 13.sp)
                Text("Mercy", color = c.tx, fontSize = 30.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
            }
            Box(Modifier.size(46.dp).clip(CircleShape).clickable { SubNav.open("Settings") }.background(Brush.radialGradient(listOf(c.b1, c.b2))))
        }

        Spacer(Modifier.height(22.dp))
        HomeHero("Sunset at the pier", "Latest memory · 2 hours ago", onClick = { MemNav.open(0) })

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("Travel", "Focus", "Wellness", "Creator").forEachIndexed { i, t ->
                NyaChip(t, mode == i, { mode = i }, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        val card = RoundedCornerShape(22.dp)
        Row(
            Modifier.fillMaxWidth().clip(card).background(c.sf).border(1.dp, c.ln, card).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NyaIcon(NyaIcons.Speaker, tint = c.ac)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Quiet mode", color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Silences voice replies only", color = c.sb, fontSize = 13.sp)
            }
            Switch(
                checked = quiet, onCheckedChange = { quiet = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.on, checkedTrackColor = c.ac,
                    uncheckedThumbColor = c.sb, uncheckedTrackColor = c.bg
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(card).background(c.sf).border(1.dp, c.ln, card)
                .clickable { Nav.toChat() }.padding(start = 20.dp, top = 14.dp, bottom = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ask Nya anything…", color = c.sb, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(42.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(c.b1, c.b2))), contentAlignment = Alignment.Center) {
                NyaIcon(NyaIcons.Up, tint = c.on, iconSize = 20.dp, strokeW = 2f)
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MEMORIES", color = c.sb, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            Text("See all", color = c.ac, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        RecentRow(onOpen = { MemNav.open(0) })
    }
}