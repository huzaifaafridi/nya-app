package com.nya.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
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

/** the little Nya face from the design */
@Composable
fun NyaFace(size: Dp) {
    val hair = remember { PathParser().parsePathString("M35 48C30 22 46 10 62 10c18 0 32 14 24 40-2-16-10-24-26-24S39 34 35 48z").toPath() }
    val band = remember { PathParser().parsePathString("M37 40c4-12 14-18 23-18s20 6 24 18c-8-6-16-8-24-8s-15 2-23 8z").toPath() }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 120f
        scale(s, s, pivot = Offset.Zero) {
            drawOval(Color(0xFFE2AC82), topLeft = Offset(36f, 22f), size = Size(48f, 56f))
            drawPath(hair, Color(0xFF3A2218))
            drawPath(band, Color(0xFFEFE1CD))
        }
    }
}

@Composable
fun IconBtn(d: String, size: Dp = 42.dp, onClick: () -> Unit = {}) {
    val c = LocalNya.current
    Box(
        Modifier.size(size).clip(CircleShape).background(c.sf).border(1.dp, c.ln, CircleShape)
            .clickable { onClick() },
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

@Composable
fun ChatHeader(onBack: () -> Unit, name: String, sub: String) {
    val c = LocalNya.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(NyaIcons.Back, onClick = onBack)
        Spacer(Modifier.width(12.dp))
        NyaOrb(42.dp, glow = false) { NyaFace(40.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = c.sb, fontSize = 13.sp)
        }
        IconBtn(NyaIcons.Speaker)
    }
}
