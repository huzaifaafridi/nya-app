package com.nya.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

/** glowing ring orb */
@Composable
fun NyaOrb(size: Dp, glow: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val c = LocalNya.current
    Box(
        Modifier.size(size)
            .drawBehind {
                if (glow) {
                    drawCircle(c.ac.copy(alpha = .08f), radius = this.size.width / 2 + 34.dp.toPx())
                    drawCircle(c.ac.copy(alpha = .16f), radius = this.size.width / 2 + 10.dp.toPx())
                }
            }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(c.sf, c.bg)))
            .border(1.5.dp, c.ac, CircleShape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun IconBtn(d: String, size: Dp = 42.dp, onClick: () -> Unit = {}) {
    val c = LocalNya.current
    Box(
        Modifier.size(size).pressScale { onClick() }.clip(CircleShape).background(c.sf).border(1.dp, c.ln, CircleShape),
        contentAlignment = Alignment.Center
    ) { NyaIcon(d, tint = c.tx, iconSize = 20.dp) }
}

@Composable
fun UserBubble(text: String) {
    val c = LocalNya.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text, color = c.on, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp,
            modifier = Modifier.widthIn(max = 290.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                .background(Brush.verticalGradient(listOf(c.b1, c.b2)))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun AiBubble(text: String) {
    val c = LocalNya.current
    val sh = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            text, color = c.tx, fontSize = 15.sp, lineHeight = 21.sp,
            modifier = Modifier.widthIn(max = 310.dp).clip(sh).background(c.sf).border(1.dp, c.ln, sh)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/** Nya's "typing..." bubble: three dots pulsing one after another */
@Composable
fun TypingBubble() {
    val c = LocalNya.current
    val sh = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    val reduce = LocalReduceMotion.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier.clip(sh).background(c.sf).border(1.dp, c.ln, sh)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val t = rememberInfiniteTransition(label = "typing")
            repeat(3) { i ->
                val p = t.animateFloat(
                    0f, 1f,
                    infiniteRepeatable(
                        keyframes { durationMillis = 1200; 0f at 0; 1f at 300 using FastOutSlowInEasing; 0f at 600 },
                        initialStartOffset = StartOffset(i * 160)
                    ),
                    label = "dot$i"
                )
                Box(
                    Modifier.size(7.dp)
                        .graphicsLayer {
                            val v = if (reduce) .5f else p.value
                            alpha = .35f + .65f * v
                            translationY = -3.dp.toPx() * v
                        }
                        .clip(CircleShape).background(c.sb)
                )
            }
        }
    }
}

@Composable
fun ChatHeader(onBack: () -> Unit, name: String, sub: String, state: AvatarState) {
    val c = LocalNya.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(NyaIcons.Back, onClick = onBack)
        Spacer(Modifier.width(12.dp))
        NyaOrb(42.dp, glow = false) { Avatar2D(Modifier.size(40.dp), UserPref.companion, UserPref.skin, state) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = c.sb, fontSize = 13.sp)
        }
        IconBtn(NyaIcons.Speaker)
    }
}
