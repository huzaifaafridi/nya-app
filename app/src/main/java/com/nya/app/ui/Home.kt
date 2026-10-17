package com.nya.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import java.util.Calendar

object Nav { var toChat: () -> Unit = {} }
var savedTab: Tab = Tab.Home
private var homeGreeted = false

enum class Tab(val label: String) { Home("Home"), Capture("Capture"), Memories("Memories"), Device("Device") }

fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
fun MainTabs() {
    var tab by remember { mutableStateOf(savedTab) }
    val onTab: (Tab) -> Unit = { tab = it; savedTab = it }
    Box(Modifier.fillMaxSize().nyaBg()) {
        Crossfade(tab, animationSpec = motionSpec(Motion.MED), label = "tab") { t ->
            when (t) {
                Tab.Home -> HomeScreen(onSeeAll = { onTab(Tab.Memories) })
                Tab.Capture -> CaptureScreen()
                Tab.Memories -> MemoriesScreen()
                Tab.Device -> DeviceScreen()
            }
        }
        NavBar(tab, onTab, Modifier.align(Alignment.BottomCenter))
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
        Tab.values().forEach { t -> NavItem(t, t == tab) { onTab(t) } }
    }
}

@Composable
private fun NavItem(t: Tab, selected: Boolean, onClick: () -> Unit) {
    val c = LocalNya.current
    val reduce = LocalReduceMotion.current
    val col by animateColorAsState(if (selected) c.ac else c.sb, motionSpec(), label = "navColor")
    // small bounce on the icon when this tab becomes active (not on first show)
    val bounce = remember { Animatable(1f) }
    var wasSelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        val becameSelected = selected && !wasSelected
        wasSelected = selected
        if (becameSelected && !reduce) {
            bounce.snapTo(.82f)
            bounce.animateTo(1f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val icon = when (t) {
        Tab.Home -> NyaIcons.Home
        Tab.Capture -> NyaIcons.Capture
        Tab.Memories -> NyaIcons.Grid
        Tab.Device -> NyaIcons.Device
    }
    Column(
        Modifier.pressScale(onClick = onClick).clip(RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NyaIcon(icon, tint = col, modifier = Modifier.graphicsLayer { scaleX = bounce.value; scaleY = bounce.value })
        Text(t.label, color = col, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun HomeScreen(onSeeAll: () -> Unit) {
    val c = LocalNya.current
    val greet = remember { !homeGreeted }
    LaunchedEffect(Unit) { homeGreeted = true }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
    ) {
        Row(Modifier.fillMaxWidth().staggerIn(0), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), color = c.sb, fontSize = 13.sp)
                Text(UserPref.name, color = c.tx, fontSize = 30.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
            }
            Box(Modifier.size(46.dp).breathe().pressScale { SubNav.open("Settings") }.clip(CircleShape).background(Brush.radialGradient(listOf(c.b1, c.b2))))
        }

        Spacer(Modifier.height(22.dp))
        HomeHero("Sunset at the pier", "Latest memory · 2 hours ago", onClick = { MemNav.open(0) }, modifier = Modifier.staggerIn(1))

        Spacer(Modifier.height(20.dp))
        Row(Modifier.staggerIn(2), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("Travel", "Focus", "Wellness", "Creator").forEachIndexed { i, t ->
                NyaChip(t, UserPref.mode == i, { UserPref.mode = i }, Modifier.weight(1f), hPad = 6.dp)
            }
        }

        Spacer(Modifier.height(12.dp))
        val card = RoundedCornerShape(22.dp)
        Row(
            Modifier.fillMaxWidth().staggerIn(3).clip(card).background(c.sf).border(1.dp, c.ln, card).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NyaIcon(NyaIcons.Speaker, tint = c.ac)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Quiet mode", color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Silences voice replies only", color = c.sb, fontSize = 13.sp)
            }
            Switch(
                checked = UserPref.quiet, onCheckedChange = { UserPref.quiet = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.on, checkedTrackColor = c.ac,
                    uncheckedThumbColor = c.sb, uncheckedTrackColor = c.bg
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().staggerIn(4).pressScale { Nav.toChat() }.clip(card).background(c.sf).border(1.dp, c.ln, card)
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NyaOrb(56.dp) { Avatar2D(Modifier.fillMaxSize(), UserPref.companion, UserPref.skin, greet = greet) }
            Spacer(Modifier.width(14.dp))
            Text("What's on your mind?", color = c.tx, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(42.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(c.b1, c.b2))), contentAlignment = Alignment.Center) {
                NyaIcon(NyaIcons.Up, tint = c.on, iconSize = 20.dp, strokeW = 2f)
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().staggerIn(5), verticalAlignment = Alignment.CenterVertically) {
            Text("MEMORIES", color = c.sb, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            Text("See all", color = c.ac, fontSize = 13.sp, modifier = Modifier.pressScale { onSeeAll() })
        }
        Spacer(Modifier.height(10.dp))
        RecentRow(onOpen = { MemNav.open(0) }, start = 6)
    }
}