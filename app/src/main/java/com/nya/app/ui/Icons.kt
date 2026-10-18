
package com.nya.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nya.app.ui.theme.LocalNya
import com.nya.app.ui.theme.NyaDark

private fun rr(x: Int, y: Int) =
    "M${x + 1.5} ${y}h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1 -1.5 1.5h-4a1.5 1.5 0 0 1 -1.5 -1.5v-4a1.5 1.5 0 0 1 1.5 -1.5z"

object NyaIcons {
    const val Home = "M4 11l8-7 8 7v9h-5v-6H9v6H4z"
    const val Capture = "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0 -16z M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0 -6z"
    val Grid = rr(4, 4) + rr(13, 4) + rr(4, 13) + rr(13, 13)
    const val Device = "M3 17a9 9 0 0 1 18 0 M6 17v-3 M18 17v-3"
    const val Mic = "M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1 -6 0v-6a3 3 0 0 1 3 -3z M5 11a7 7 0 0 0 14 0 M12 18v3"
    const val Back = "M15 5l-7 7 7 7"
    const val Chevron = "M9 5l7 7-7 7"
    const val Up = "M12 19V5 M5 12l7-7 7 7"
    const val Play = "M8 5l11 7-11 7z"
    const val Stop = "M7 7h10v10H7z"
    const val Filter = "M4 7h16 M7 12h10 M10 17h4"
    const val Search = "M11 5a6 6 0 1 0 0 12a6 6 0 1 0 0 -12z M16 16l4 4"
    const val Speaker = "M4 10v4h4l5 4V6L8 10z M16 9a4 4 0 0 1 0 6"
    const val Pin = "M12 7a3 3 0 1 0 0 6a3 3 0 1 0 0 -6z M12 21s7-6 7-11a7 7 0 0 0 -14 0c0 5 7 11 7 11z"
    const val Gear = "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0 -6z M12 3v3 M12 18v3 M3 12h3 M18 12h3 M5.6 5.6l2.1 2.1 M16.3 16.3l2.1 2.1 M5.6 18.4l2.1-2.1 M16.3 7.7l2.1-2.1"
    const val Refresh = "M4 12a8 8 0 0 1 14 -5 M20 12a8 8 0 0 1 -14 5 M18 3v4h-4 M6 21v-4h4"
    const val Trash = "M5 7h14 M9 7V4h6v3 M7 7l1 13h8l1-13"
    const val Download = "M12 4v11 M8 11l4 4 4-4 M5 20h14"
}

@Composable
fun NyaIcon(
    d: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalNya.current.tx,
    iconSize: Dp = 24.dp,
    filled: Boolean = false,
    strokeW: Float = 1.6f
) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Canvas(modifier.size(iconSize)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path, tint,
                style = if (filled) Fill else Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// warm glow at the top of the screen, like the design
@Composable
fun Modifier.nyaGlow(): Modifier {
    val c = LocalNya.current
    val glow = if (c == NyaDark) Color(0x3DE8A86E) else Color(0x33BE6E32)
    return this.drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(glow, Color.Transparent),
                center = Offset(size.width / 2f, 40f),
                radius = size.width * 0.85f
            )
        )
    }
}

// background color + glow in one
@Composable
fun Modifier.nyaBg(): Modifier = this.background(LocalNya.current.bg).nyaGlow()
