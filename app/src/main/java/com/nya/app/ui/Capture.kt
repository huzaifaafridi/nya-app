package com.nya.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import kotlinx.coroutines.delay

@Composable
fun CaptureScreen(onBack: (() -> Unit)? = null) {
    val c = LocalNya.current
    var mode by remember { mutableIntStateOf(0) }
    var rec by remember { mutableStateOf(false) }
    var secs by remember { mutableIntStateOf(0) }
    var saved by remember { mutableIntStateOf(0) }

    LaunchedEffect(rec) {
        if (rec) {
            while (true) {
                delay(1000)
                secs++
            }
        } else {
            secs = 0
        }
    }

    val t = rememberInfiniteTransition(label = "rec")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
    )

    Column(Modifier.fillMaxSize().nyaBg()) {
        if (onBack != null) {
            XTop("Capture", onBack)
        } else {
            Text("Capture", color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 24.dp, bottom = 18.dp).asHeading())
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            listOf("Photo", "Video", "Voice").forEachIndexed { i, label ->
                val sel = mode == i
                val shape = RoundedCornerShape(20.dp)
                Box(
                    Modifier.pressScale(role = Role.RadioButton, selected = sel) { if (!rec) mode = i }.clip(shape)
                        .background(if (sel) c.ac else c.sf)
                        .border(1.dp, if (sel) c.ac else c.ln, shape)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) { Text(label, color = if (sel) c.on else c.sb, fontSize = 14.sp) }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            if (rec) "%02d:%02d".format(secs / 60, secs % 60) else "Ready",
            color = c.tx, fontSize = 40.sp, fontWeight = FontWeight.Light,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text("Headband connected", color = NyaGreen, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
        Spacer(Modifier.height(36.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).scale(if (rec) pulse else 1f)
                    .border(3.dp, c.ac, CircleShape)
            )
            Box(
                Modifier.size(if (rec) 70.dp else 112.dp)
                    .pressScale(description = if (mode == 0) "Take photo" else if (rec) "Stop recording" else "Start recording") {
                        if (mode == 0) saved++
                        else if (rec) { rec = false; saved++ }
                        else rec = true
                    }
                    .clip(if (rec) RoundedCornerShape(18.dp) else CircleShape)
                    .background(Brush.linearGradient(listOf(c.b1, c.b2)))
            )
        }

        Spacer(Modifier.height(30.dp))
        Text(
            if (saved == 0) "Nothing captured yet" else "$saved saved to Memories",
            color = c.sb, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(110.dp))
    }
}

