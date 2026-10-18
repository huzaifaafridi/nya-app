package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.MemoriesViewModel

@Composable
fun ReadOnlyScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<MemoriesViewModel>(factory = MemoriesViewModel.Factory)
    val state = vm.memories.collectAsState().value
    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Memories", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            XCard {
                Text("🔒  Read-only mode", color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Your headband isn't connected and your plan has ended. You can view memories but not add new ones.",
                    color = c.sb, fontSize = 14.sp, lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            LoadableContent(state, vm::retry) { memories ->
                if (memories.isEmpty()) EmptyState("No memories yet", "Capture a moment and it will show up here.", Modifier.padding(vertical = 40.dp))
                memories.take(3).forEach { m ->
                    Row(
                        Modifier.padding(bottom = 12.dp).fillMaxWidth().alpha(0.6f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                                .background(m.scene.brush())
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(m.title, color = c.tx, fontSize = 15.sp)
                            Text(memoryTime(m.createdAt), color = c.sb, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            XBtn("Reconnect headband", { SubNav.open("PairFail") })
            Spacer(Modifier.height(12.dp))
            XGhost("Renew plan", { SubNav.open("Subscription") })
        }
    }
}

