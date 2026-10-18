package com.nya.app.ui

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// shared motion tokens: short, FastOutSlowIn or springs, nothing flashy
object Motion {
    const val MED = 250
    const val LONG = 350
    const val STAGGER = 60L
}

fun <T> motionTween(ms: Int = Motion.MED) = tween<T>(ms, easing = FastOutSlowInEasing)

/** motionTween, or an instant snap when reduced motion is on */
@Composable
fun <T> motionSpec(ms: Int = Motion.MED): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else motionTween(ms)

/** true when the system "Animator duration scale" is off; provided once at the root */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** fade + 24dp slide + slight scale; forward comes in from the right, back from the left */
fun screenTransition(forward: Boolean, distPx: Int, reduce: Boolean): ContentTransform {
    if (reduce) return EnterTransition.None togetherWith ExitTransition.None
    val d = if (forward) distPx else -distPx
    return (fadeIn(motionTween(Motion.LONG)) +
            slideInHorizontally(motionTween(Motion.LONG)) { d } +
            scaleIn(motionTween(Motion.LONG), initialScale = .98f)) togetherWith
            (fadeOut(motionTween(Motion.MED)) +
                    slideOutHorizontally(motionTween(Motion.LONG)) { -d } +
                    scaleOut(motionTween(Motion.LONG), targetScale = .98f))
}

/**
 * Clickable with a subtle press effect instead of the ripple: scales to 0.96 while pressed
 * and springs back. Put it before clip/background so the whole element scales.
 * [role], [selected] and [description] are what TalkBack announces; use [description] for icon-only targets.
 */
fun Modifier.pressScale(
    enabled: Boolean = true, role: Role = Role.Button, selected: Boolean? = null,
    description: String? = null, onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val s = animateFloatAsState(
        if (pressed && !reduce) .96f else 1f,
        spring(dampingRatio = .6f, stiffness = Spring.StiffnessMedium), label = "press"
    )
    graphicsLayer { scaleX = s.value; scaleY = s.value }
        .clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)
        .semantics {
            if (description != null) contentDescription = description
            if (selected != null) this.selected = selected
        }
}

// 0 -> 1 once, after index * STAGGER; already 1 when motion is reduced or animate is false
@Composable
private fun rememberEntrance(index: Int, animate: Boolean): State<Float> {
    val run = animate && !LocalReduceMotion.current
    val p = remember { Animatable(if (run) 0f else 1f) }
    if (run) LaunchedEffect(Unit) {
        delay(index * Motion.STAGGER)
        p.animateTo(1f, motionTween(Motion.LONG))
    }
    return p.asState()
}

/** list/card entrance: fade in and rise 16dp, staggered by index */
fun Modifier.staggerIn(index: Int, animate: Boolean = true): Modifier = composed {
    val p = rememberEntrance(index, animate)
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 16.dp.toPx()
    }
}

/** thumbnail entrance: fade in and scale 0.96 -> 1, staggered by index */
fun Modifier.popIn(index: Int): Modifier = composed {
    val p = rememberEntrance(index, true)
    graphicsLayer {
        alpha = p.value
        val s = .96f + .04f * p.value
        scaleX = s; scaleY = s
    }
}

/** very slow, subtle breathing pulse (1.0 -> 1.03); stops when it leaves the screen */
fun Modifier.breathe(): Modifier = composed {
    if (LocalReduceMotion.current) return@composed Modifier
    val t = rememberInfiniteTransition(label = "breathe")
    val s = t.animateFloat(
        1f, 1.03f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s"
    )
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}
