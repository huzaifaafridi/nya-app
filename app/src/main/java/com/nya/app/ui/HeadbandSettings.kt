package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.data.DeviceInfo
import com.nya.app.data.MaxDeviceNameLength
import com.nya.app.data.ble.LinkState
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.DeviceViewModel
import kotlin.math.roundToInt

private val QualityLabels = listOf("Standard", "High", "Max")

@Composable
fun HeadbandSettingsScreen(onBack: () -> Unit) {
    val vm = viewModel<DeviceViewModel>(factory = DeviceViewModel.Factory)
    val state = vm.device.collectAsState().value
    Column(Modifier.fillMaxSize().nyaBg().imePadding()) {
        XTop("Headband settings", onBack)
        LoadableContent(state, vm::retry) { d ->
            if (d == null) EmptyState("No headband paired", "Pair your Nya headband to change its settings.", Modifier.padding(vertical = 40.dp))
            else SettingsForm(d, vm)
        }
    }
}

@Composable
private fun SettingsForm(d: DeviceInfo, vm: DeviceViewModel) {
    val c = LocalNya.current
    // taken once, so live readings never overwrite what the user is typing
    var draft by remember { mutableStateOf(d.settings) }
    val seg = RoundedCornerShape(16.dp)
    // runs when the screen opens and after every edit, so an old result never lingers
    LaunchedEffect(draft) { vm.clearSettingsMessages() }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
        NyaField("Name", draft.name, { draft = draft.copy(name = it.take(MaxDeviceNameLength)) })
        Spacer(Modifier.height(8.dp))
        XToggle("Auto-sync over Wi-Fi", "Send new memories when on Wi-Fi", draft.autoSync) { draft = draft.copy(autoSync = it) }
        XDivider()
        XToggle("LED indicator", "Show the status light", draft.ledIndicator) { draft = draft.copy(ledIndicator = it) }

        XLabel("Capture quality")
        Row(Modifier.fillMaxWidth().clip(seg).background(c.sf).border(1.dp, c.ln, seg).padding(4.dp)) {
            QualityLabels.forEachIndexed { i, label ->
                val sel = draft.captureQuality == i
                Box(
                    Modifier.weight(1f).pressScale(role = Role.RadioButton, selected = sel) { draft = draft.copy(captureQuality = i) }
                        .clip(RoundedCornerShape(12.dp)).background(if (sel) c.ac else Color.Transparent).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, color = if (sel) c.on else c.sb, fontSize = 14.sp) }
            }
        }

        XLabel("About")
        XCard {
            XRow("Firmware", d.firmware)
            XDivider()
            XRow("Battery", "${(d.battery * 100).roundToInt()}%")
            XDivider()
            XRow("Storage", "${d.storageUsedGb} of ${d.storageTotalGb.roundToInt()} GB")
            XDivider()
            XRow("Connection", when (d.state) {
                LinkState.Connected -> "Connected"
                LinkState.Connecting -> "Connecting…"
                LinkState.Disconnected -> "Disconnected"
            })
            if (LocalConfig.current.features.firmwareUpdates) {
                XDivider()
                XRow("Check for updates") { SubNav.open("Firmware") }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (d.state != LinkState.Connected) Text("Your headband is disconnected. Saving will try to reconnect it first.",
            color = c.sb, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
        NyaButton("Save", { vm.saveSettings(draft) }, loading = vm.savingSettings)
        AuthMessage(vm.settingsError, vm.settingsInfo)
    }
}
