package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.data.ble.LinkState
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.DeviceViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun DeviceScreen() {
    val c = LocalNya.current
    val features = LocalConfig.current.features
    val vm = viewModel<DeviceViewModel>(factory = DeviceViewModel.Factory)
    val state = vm.device.collectAsState().value
    val connecting by vm.connecting.collectAsState()
    val card = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
    ) {
        Text("Device", color = c.tx, fontSize = 34.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
            modifier = Modifier.staggerIn(0).asHeading())
        LoadableContent(state, vm::retry) { d ->
            if (d == null) {
                EmptyState("No headband paired", "Pair your Nya headband to see battery, storage and updates.", Modifier.padding(vertical = 40.dp))
                return@LoadableContent
            }
            if (d.lost && features.findMy) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().pressScale { SubNav.open("FindMy") }.clip(card).background(c.sf)
                        .border(1.dp, NyaDanger, card).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NyaIcon(NyaIcons.Pin, tint = NyaDanger)
                    Spacer(Modifier.width(12.dp))
                    Text("Marked as lost", color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("Open Find My", color = c.ac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.staggerIn(1), verticalAlignment = Alignment.CenterVertically) {
                NyaOrb(84.dp) { NyaIcon(NyaIcons.Device, tint = c.ac, iconSize = 44.dp, strokeW = 1.2f) }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(d.name, color = c.tx, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        val connected = d.state == LinkState.Connected
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) NyaGreen else c.sb))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (d.state) {
                                LinkState.Connected -> "Connected · ${d.link}"
                                LinkState.Connecting -> "Connecting…"
                                LinkState.Disconnected -> if (connecting) "Connecting…" else "Disconnected"
                            },
                            color = c.sb, fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(Modifier.staggerIn(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("BATTERY", "${(d.battery * 100).roundToInt()}%", d.battery, Modifier.weight(1f), delayMs = 2 * Motion.STAGGER)
                StatCard("STORAGE", "${d.storageUsedGb} of ${d.storageTotalGb.roundToInt()} GB", d.storageUsedGb / d.storageTotalGb, Modifier.weight(1f), delayMs = 3 * Motion.STAGGER)
            }

            d.latestFirmware?.takeIf { features.firmwareUpdates }?.let {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().staggerIn(3).clip(card).background(c.sf).border(1.dp, c.ac, card).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NyaIcon(NyaIcons.Download, tint = c.ac)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Update available", color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Firmware $it · you have ${d.firmware}", color = c.sb, fontSize = 13.sp)
                    }
                    Text("Update", color = c.on, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pressScale { SubNav.open("Firmware") }
                            .sizeIn(minHeight = 48.dp).wrapContentHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(c.b1, c.b2)))
                            .padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().staggerIn(4).clip(card).background(c.sf).border(1.dp, c.ln, card)) {
                if (features.findMy) {
                    DevRow(NyaIcons.Pin, "Find My Nya", chevron = true) { SubNav.open("FindMy") }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
                }
                DevRow(NyaIcons.Gear, "Headband settings", chevron = true) { SubNav.open("HeadbandSettings") }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
                DevRow(NyaIcons.Refresh, "Reconnect") { vm.reconnect { SubNav.open("PairFail") } }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
                DevRow(NyaIcons.Trash, "Forget this device", danger = true) { SubNav.open("ReadOnly") }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, frac: Float, modifier: Modifier, delayMs: Long = 0) {
    val c = LocalNya.current
    val card = RoundedCornerShape(22.dp)
    // bar fills from 0 on first show; read in the layout phase so it never recomposes
    val reduce = LocalReduceMotion.current
    val fill = remember { Animatable(if (reduce) frac else 0f) }
    LaunchedEffect(frac) {
        if (reduce) fill.snapTo(frac)
        else { delay(delayMs); fill.animateTo(frac, motionTween(Motion.LONG)) }
    }
    Column(modifier.clip(card).background(c.sf).border(1.dp, c.ln, card).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label, color = c.sb, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = c.tx, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(5.dp).clip(CircleShape).background(c.ln)) {
            Box(Modifier
                .layout { m, cs ->
                    val w = (cs.maxWidth * fill.value).roundToInt()
                    val p = m.measure(Constraints.fixed(w, cs.maxHeight))
                    layout(w, cs.maxHeight) { p.place(0, 0) }
                }
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(c.b2, c.b1))))
        }
    }
}

@Composable
private fun DevRow(icon: String, label: String, chevron: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val c = LocalNya.current
    val col = if (danger) NyaDanger else c.tx
    Row(
        Modifier.fillMaxWidth().pressScale { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NyaIcon(icon, tint = col, iconSize = 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = col, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (chevron) NyaIcon(NyaIcons.Chevron, tint = c.sb, iconSize = 18.dp)
    }
}