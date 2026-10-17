package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

enum class Scene { Sea, Forest, Dusk, Mount, Yoga, City }

private fun Scene.stops(): Array<Pair<Float, Color>> = when (this) {
    Scene.Sea -> arrayOf(0f to Color(0xFF39406A), .30f to Color(0xFFC46F5A), .47f to Color(0xFFF0A56A),
        .49f to Color(0xFFE8975C), .50f to Color(0xFF2B3D58), 1f to Color(0xFF162234))
    Scene.Forest -> arrayOf(0f to Color(0xFF8FB09B), .38f to Color(0xFF4C7A5E), .70f to Color(0xFF24432F), 1f to Color(0xFF102019))
    Scene.Dusk -> arrayOf(0f to Color(0xFF2F2A55), .55f to Color(0xFF8A4F6B), 1f to Color(0xFFE39A6A))
    Scene.Mount -> arrayOf(0f to Color(0xFFF5CBA3), .38f to Color(0xFFD79A78), .39f to Color(0xFF6B5C80),
        .70f to Color(0xFF36345A), 1f to Color(0xFF1D1B33))
    Scene.Yoga -> arrayOf(0f to Color(0xFF3F6B55), .55f to Color(0xFFB88A55), 1f to Color(0xFF3A2A1E))
    Scene.City -> arrayOf(0f to Color(0xFF223048), .55f to Color(0xFF6E5B7A), 1f to Color(0xFFF0A06C))
}

/** gradient "photo" placeholder. Later swap the background for a real image. */
@Composable
fun NyaThumb(
    scene: Scene,
    modifier: Modifier = Modifier,
    badge: String? = null,
    play: Boolean = false,
    onClick: (() -> Unit)? = null,
    index: Int = 0
) {
    val base = modifier.popIn(index).then(if (onClick != null) Modifier.pressScale { onClick() } else Modifier)
        .clip(RoundedCornerShape(18.dp))
        .background(Brush.verticalGradient(colorStops = scene.stops()))
        .drawBehind {
            // sun / light spot
            val (cx, cy) = when (scene) {
                Scene.Sea -> .68f to .44f
                Scene.Forest -> .28f to .06f
                else -> .5f to .7f
            }
            drawRect(Brush.radialGradient(
                listOf(Color(0x66FFD7A0), Color.Transparent),
                center = Offset(size.width * cx, size.height * cy), radius = size.width * .55f))
        }
    Box(base) {
        if (play) Box(
            Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape)
                .background(Color(0x80000000)).border(1.dp, Color(0xCCFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) { NyaIcon(NyaIcons.Play, tint = Color.White, iconSize = 18.dp, filled = true) }
        if (badge != null) Text(
            badge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0x80000000))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** filter chip with the glowing ring when active */
@Composable
fun NyaChip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, hPad: Dp = 16.dp) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(19.dp)
    val ring by animateFloatAsState(if (on) .22f else 0f, motionSpec(), label = "ring")
    val line by animateColorAsState(if (on) c.ac else c.ln, motionSpec(), label = "line")
    val ink by animateColorAsState(if (on) c.ac else c.tx, motionSpec(), label = "ink")
    Box(
        modifier
            .pressScale { onClick() }
            .border(3.dp, c.ac.copy(alpha = ring), RoundedCornerShape(22.dp)).padding(3.dp)
            .height(38.dp).clip(shape)
            .background(c.sf)
            .border(1.dp, line, shape)
            .padding(horizontal = hPad),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
    }
}

/** Home hero card: sea photo with text on a dark fade */
@Composable
fun HomeHero(title: String, sub: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(156.dp).pressScale { onClick() }.clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(colorStops = Scene.Sea.stops()))
    ) {
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(listOf(Color(0x0D000000), Color(0xCC080605)))))
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Text(sub, color = Color.White.copy(alpha = .92f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Home: row of 3 recent memories */
@Composable
fun RecentRow(onOpen: () -> Unit, start: Int = 0) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Scene.Sea, Scene.Forest, Scene.Dusk).forEachIndexed { i, s ->
            NyaThumb(s, Modifier.weight(1f).height(84.dp), onClick = onOpen, index = start + i)
        }
    }
}

/** Memories: the masonry grid from the design */
@Composable
fun MemoriesGrid(onOpen: () -> Unit, start: Int = 0) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NyaThumb(Scene.Sea, Modifier.weight(1f).height(190.dp), "Clip · 0:08", true, onOpen, start)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NyaThumb(Scene.Mount, Modifier.fillMaxWidth().height(90.dp), "Photo", false, onOpen, start + 1)
                NyaThumb(Scene.Dusk, Modifier.fillMaxWidth().height(90.dp), "Photo", false, onOpen, start + 2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NyaThumb(Scene.Forest, Modifier.weight(1f).height(130.dp), "Clip · 0:06", true, onOpen, start + 3)
            NyaThumb(Scene.Yoga, Modifier.weight(1f).height(130.dp), "Photo", false, onOpen, start + 4)
        }
    }
}

