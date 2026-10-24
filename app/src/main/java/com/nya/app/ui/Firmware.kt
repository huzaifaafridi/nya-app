package com.nya.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.DeviceViewModel
import com.nya.app.viewmodel.FirmwareUiState
import com.nya.app.viewmodel.valueOrNull
import kotlin.math.roundToInt

// The view model is shared with the other device screens (activity scoped), so leaving this screen
// does not cancel a running update; the Cancel button is the only way to stop it.
@Composable
fun FirmwareScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<DeviceViewModel>(factory = DeviceViewModel.Factory)
    val state by vm.firmware.collectAsState()
    val current = vm.device.collectAsState().value.valueOrNull()?.firmware
    // a failure after an update started can be retried directly; any other failure needs a fresh check
    var started by remember { mutableStateOf(false) }
    // an update in progress keeps its live progress; anything else is checked afresh
    LaunchedEffect(Unit) { vm.check() }
    LaunchedEffect(state) {
        if (state is FirmwareUiState.Downloading || state is FirmwareUiState.Installing) started = true
    }

    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Firmware", onBack)
        CenterScroll(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            when (val s = state) {
                FirmwareUiState.Idle, FirmwareUiState.Checking -> {
                    LoadingState()
                    Status("Checking for updates…")
                }
                FirmwareUiState.UpToDate -> Status("You're up to date", current?.let { "Firmware $it" })
                is FirmwareUiState.Available -> {
                    Status("Firmware ${s.version} is ready", current?.let { "You have $it" })
                    Text("Keep your headband nearby and charged.", color = c.sb, fontSize = 13.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
                }
                is FirmwareUiState.Downloading -> Progress("Downloading", s.progress)
                is FirmwareUiState.Installing -> Progress("Installing", s.progress)
                is FirmwareUiState.Done -> Status("Updated to ${s.version}")
                is FirmwareUiState.Failed -> Text(s.message, color = NyaDanger, fontSize = 15.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 30.dp)) {
            when (state) {
                is FirmwareUiState.Available -> XBtn("Update now", vm::start)
                is FirmwareUiState.Downloading, is FirmwareUiState.Installing -> XGhost("Cancel", vm::cancel)
                is FirmwareUiState.Done -> XBtn("Done", onBack)
                is FirmwareUiState.Failed -> XBtn("Try again", if (started) vm::start else vm::check)
                else -> {}
            }
        }
    }
}

@Composable
private fun Status(title: String, detail: String? = null) {
    val c = LocalNya.current
    Text(title, color = c.tx, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp).semantics { liveRegion = LiveRegionMode.Polite })
    if (detail != null) Text(detail, color = c.sb, fontSize = 14.sp, textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Progress(phase: String, progress: Float) {
    val c = LocalNya.current
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text("$phase… ${(progress * 100).roundToInt()}%", color = c.tx, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        XBar(progress)
        Text("Keep your headband nearby.", color = c.sb, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
    }
}
