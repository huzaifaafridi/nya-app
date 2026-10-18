package com.nya.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.data.ble.LinkState
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.DeviceViewModel
import com.nya.app.viewmodel.valueOrNull

@Composable
fun FindMyScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<DeviceViewModel>(factory = DeviceViewModel.Factory)
    val device = vm.device.collectAsState().value.valueOrNull()
    val ringing by vm.ringing.collectAsState()
    val t = rememberInfiniteTransition(label = "radar")
    val p by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p"
    )

    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Find my Nya", onBack)
        CenterScroll(Modifier.weight(1f)) {
            Box(
                Modifier.fillMaxWidth().height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                repeat(3) { k ->
                    val f = (p + k / 3f) % 1f
                    Box(
                        Modifier.size(260.dp).scale(0.3f + 0.7f * f).alpha(1f - f)
                            .border(2.dp, c.ac, CircleShape)
                    )
                }
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c.b1, c.b2)))
                )
            }
            Text(device?.name ?: "Your Nya", color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    ringing -> "Ringing…"
                    device?.state == LinkState.Connected -> "Connected · nearby"
                    else -> "Not connected"
                },
                color = c.sb, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)
            )
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 30.dp)) {
            XBtn(if (ringing) "Stop sound" else "Play sound", { vm.ring(!ringing) })
            Spacer(Modifier.height(12.dp))
            XGhost("Mark as lost", { })
        }
    }
}

