package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.DeviceViewModel
import com.nya.app.viewmodel.Loadable
import com.nya.app.viewmodel.valueOrNull

@Composable
fun TransferScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<DeviceViewModel>(factory = DeviceViewModel.Factory)
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by remember(attempt) { vm.transfers() }.collectAsState(Loadable.Loading)
    val items = loaded.valueOrNull().orEmpty()
    val done = items.count { it.progress >= 1f }

    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Transfer", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(
                if (items.isNotEmpty() && done == items.size) "All synced" else "Syncing from ${vm.device.collectAsState().value.valueOrNull()?.name ?: "your headband"}",
                color = c.tx, fontSize = 22.sp, fontWeight = FontWeight.SemiBold
            )
            Text("$done of ${items.size} done", color = c.sb, fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

            when (loaded) {
                Loadable.Loading -> LoadingState(Modifier.padding(vertical = 40.dp))
                is Loadable.Failed -> ErrorState("Couldn't reach your headband.", { attempt++ }, Modifier.padding(vertical = 40.dp))
                is Loadable.Ready -> {}
            }
            items.forEach { t ->
                XCard(Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, color = c.tx, fontSize = 15.sp)
                            Text("${t.sizeMb} MB", color = c.sb, fontSize = 12.sp)
                        }
                        Text(
                            if (t.progress >= 1f) "✓" else "${(t.progress * 100).toInt()}%",
                            color = if (t.progress >= 1f) NyaGreen else c.sb, fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    XBar(t.progress)
                }
            }
        }
    }
}

