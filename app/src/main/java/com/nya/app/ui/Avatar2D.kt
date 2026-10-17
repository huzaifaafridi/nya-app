package com.nya.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onVisibilityChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Companion { Wanderer, Muse, Guide, Yogi, Creator }

enum class AvatarState { Idle, Listening, Thinking, Speaking }

enum class Gender { Female, Male }

val DefaultSkin = Color(0xFFE0AC84)

// one clock (ms) drives breathing (4s), sway (6s), glances, nods, blinks and the mouth
private const val PERIOD = 12000f
private const val BLINK = 160f
private const val TAU = 6.2831855f

private val Ink = Color(0xFF2A1A14)
private val Shade = Color(0x262A1A14)
private val Lips = Color(0xFFB0574A)
private val BandColor = Color(0xFFEFE1CD)
private val Sclera = Color(0xFFFFFBF5)
private val Spark = Color(0xFFFFE2B0)
private val Rim = Color(0x59FFD9B0)

private val Line = Stroke(width = 1.6f, cap = StrokeCap.Round)
private val Thin = Stroke(width = 1f, cap = StrokeCap.Round)
private val Shine = Stroke(width = 2.5f, cap = StrokeCap.Round)

private val Glow = Brush.radialGradient(listOf(Color(0x40E8A86E), Color.Transparent), Offset(60f, 48f), 50f)
private val Iris = Brush.radialGradient(listOf(Color(0xFFB07A4F), Color(0xFF4A2A1A)), Offset.Zero, 3.4f)
private val BlushL = Brush.radialGradient(listOf(Color(0x40F08A7A), Color.Transparent), Offset(44f, 61f), 6f)
private val BlushR = Brush.radialGradient(listOf(Color(0x40F08A7A), Color.Transparent), Offset(76f, 61f), 6f)

private fun path(d: String) = PathParser().parsePathString(d).toPath()

private val BandPath = path("M37 40c4-12 14-18 23-18s20 6 24 18c-8-6-16-8-24-8s-15 2-23 8z")
private val BodyPath = path("M14 120C16 104 30 93 46 91c4 3 9 4 14 4s10-1 14-4c16 2 30 13 32 29z")
private val Star = path("M0 -1Q0.2 -0.2 1 0Q0.2 0.2 0 1Q-0.2 0.2 -1 0Q-0.2 -0.2 0 -1z")
private val SparkAngles = floatArrayOf(-2.6f, -1.75f, -0.7f, 0.2f)
private val EyeX = floatArrayOf(49.5f, 70.5f)

// female: softer jaw, slim brows, lashes, earrings; male: stronger jaw, thick brows, light stubble
private val FaceF = path("M60 22C74 22 84 33 84 48c0 15-11 30-24 30S36 63 36 48C36 33 46 22 60 22z")
private val FaceM = path("M60 22C75 22 84 32 84 47c0 10-2 18-7 24-4 5-10 7-17 7s-13-2-17-7c-5-6-7-14-7-24 0-15 9-25 24-25z")
private val BrowF = Stroke(width = 1.2f, cap = StrokeCap.Round)
private val BrowM = Stroke(width = 2.2f, cap = StrokeCap.Round)
private val Gold = Color(0xFFE8C07A)
private val Stubble = Color(0x142A1A14)

/** everything that makes a companion look different; [extra] is drawn on top in the accent color */
private class Look(
    val female: Boolean, val hair: Color, val accent: Color, front: String,
    back: String? = null, extra: String? = null,
    val glasses: Boolean = false, val serene: Boolean = false
) {
    val front = path(front)
    val back = back?.let(::path)
    val extra = extra?.let(::path)
    val shine = lerp(hair, Color.White, .35f).copy(alpha = .7f)
    val body = Brush.verticalGradient(listOf(lerp(accent, Color.White, .15f), lerp(accent, Color.Black, .15f)), 90f, 120f)
}

private const val BaseHair = "M35 48C30 22 46 10 62 10c18 0 32 14 24 40-2-16-10-24-26-24S39 34 35 48z"
private const val ShortHair = "M36 44C33 21 46 12 60 12s27 9 24 32c-3-12-12-16-24-16s-21 4-24 16z"
private const val Bob = "M32 50C26 24 42 8 60 8s34 16 28 42c2 14 4 26 0 36-6 2-12 0-16-4H48c-4 4-10 6-16 4-4-10-2-22 0-36z"
private const val Bun = "M60 0a10 10 0 1 0 .1 0z"

private fun look(companion: Companion, gender: Gender): Look {
    val f = gender == Gender.Female
    return when (companion) {
        Companion.Wanderer -> Look(f, Color(0xFF3A2218), Color(0xFFB07A4F), if (f) BaseHair else ShortHair,
            back = if (f) Bob else null,
            extra = "M38 30C38 14 48 6 60 6s22 8 22 24z M22 34c12-8 64-8 76 0c-10 5-66 5-76 0z")
        Companion.Muse -> Look(f, Color(0xFF4A2A1A), Color(0xFFE8A86E),
            if (f) "M35 50C30 22 46 8 62 8c20 0 34 16 24 44-4-18-14-26-30-24-10 2-18 12-25 22z"
            else "M35 46C31 24 44 9 62 9c16 0 28 10 26 26-5-9-14-13-26-11-11 2-20 9-27 22z",
            back = if (f) "M30 50C24 20 42 6 60 6s38 12 31 44c3 24 9 40 15 54H14c6-14 12-32 16-54z" else null)
        Companion.Guide -> Look(f, Color(0xFF2E1E16), Color(0xFF8C7B6B), if (f) BaseHair else ShortHair,
            back = if (f) Bob else null, glasses = true)
        Companion.Yogi -> Look(f, Color(0xFF3A2218), Color(0xFFC9A26B), if (f) BaseHair else ShortHair,
            back = Bun, serene = true)
        Companion.Creator -> Look(f, Color(0xFF5A2E1E), Color(0xFFD9775B),
            if (f) "M30 52C20 18 40 0 64 2c24 2 36 24 28 50-2-18-12-30-30-30-14 0-26 12-32 30z"
            else "M33 48C28 26 38 8 58 6c10-1 20 1 26 6 4 3 4 8 0 10 6 6 6 16 2 26-3-14-12-20-26-20-14 0-22 8-27 20z",
            back = if (f) Bob else null,
            extra = "M50 8c8 2 12 10 10 18l-5-1c1-6-1-11-7-14z")
    }
}

// three blinks per clock cycle, each gap a random 3-5s, gaps summing to PERIOD
private fun blinkTimes(): FloatArray {
    val a = Random.nextInt(3000, 5001)
    val b = a + Random.nextInt(maxOf(3000, 7000 - a), minOf(5000, 9000 - a) + 1)
    return floatArrayOf(a.toFloat(), b.toFloat(), 0f)
}

private fun eyeOpen(t: Float, blinks: FloatArray): Float {
    for (b in blinks) {
        val d = (t - b + PERIOD) % PERIOD
        if (d < BLINK) return abs(d / BLINK * 2 - 1)
    }
    return 1f
}

// 0 -> 1 -> 0 while t is in [start, start + len)
private fun bump(t: Float, start: Float, len: Float) =
    if (t < start || t >= start + len) 0f else sin(TAU / 2 * (t - start) / len)

@Composable
fun Avatar2D(
    modifier: Modifier = Modifier,
    companion: Companion,
    skinTone: Color,
    gender: Gender,
    state: AvatarState = AvatarState.Idle,
    greet: Boolean = false,
    mouth: State<Boolean>? = null
) {
    val look = remember(companion, gender) { look(companion, gender) }
    val blinks = remember { blinkTimes() }
    val joy = remember { Animatable(0f) }
    val spark = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val still = LocalReduceMotion.current
    val ease = if (still) snap() else spring<Float>(stiffness = Spring.StiffnessMediumLow)
    val listen = animateFloatAsState(if (state == AvatarState.Listening) 1f else 0f, ease, label = "listen")
    val think = animateFloatAsState(if (state == AvatarState.Thinking) 1f else 0f, ease, label = "think")
    val speak = animateFloatAsState(if (state == AvatarState.Speaking) 1f else 0f, ease, label = "speak")
    var visible by remember { mutableStateOf(false) }
    val clock = if (visible) {
        rememberInfiniteTransition(label = "avatar").animateFloat(
            0f, PERIOD, infiniteRepeatable(tween(PERIOD.toInt(), easing = LinearEasing)), label = "clock"
        )
    } else remember { mutableFloatStateOf(0f) }
    val happy = {
        if (!still) scope.launch {
            launch { spark.snapTo(0f); spark.animateTo(1f, tween(600)) }
            joy.animateTo(1f, tween(120))
            joy.animateTo(0f, spring(dampingRatio = .4f, stiffness = Spring.StiffnessLow))
        }
    }
    if (greet) LaunchedEffect(Unit) { happy() }
    // follow the word-timed signal outside composition, eased so the mouth doesn't jitter
    val talk = remember { Animatable(0f) }
    if (mouth != null) LaunchedEffect(mouth) {
        snapshotFlow { mouth.value }.collectLatest { talk.animateTo(if (it) 1f else 0f, tween(60)) }
    }

    Spacer(
        modifier
            .onVisibilityChanged(0, .01f) { visible = it }
            .pointerInput(still) {
                // observe taps without consuming them, so a clickable parent still gets the click
                awaitEachGesture {
                    awaitFirstDown()
                    if (waitForUpOrCancellation() != null) happy()
                }
            }
            .drawWithCache {
                val s = size.minDimension / 120f
                val dx = (size.width - 120f * s) / 2
                val dy = (size.height - 120f * s) / 2
                val skin = Brush.verticalGradient(
                    listOf(lerp(skinTone, Color.White, .12f), skinTone, lerp(skinTone, Color.Black, .1f)), 22f, 78f
                )
                val skinShade = lerp(skinTone, Color.Black, .14f)
                onDrawBehind {
                    translate(dx, dy) {
                        scale(s, Offset.Zero) {
                            drawAvatar(
                                look, skin, skinShade, clock.value, still, blinks,
                                listen.value, think.value, speak.value, joy.value, spark.value,
                                if (mouth != null) talk.value else -1f
                            )
                        }
                    }
                }
            }
    )
}

private fun DrawScope.drawAvatar(
    look: Look, skin: Brush, skinShade: Color, t: Float, still: Boolean, blinks: FloatArray,
    listen: Float, think: Float, speak: Float, joy: Float, spark: Float, talk: Float
) {
    val m = if (still) 0f else t // reduced motion: static pose, blinks still use t
    val breath = (1 - cos(TAU * m / 4000)) / 2
    val sway = 2f * sin(TAU * m / 6000) + 4f * listen
    val hairSway = 1.2f * sin(TAU * (m - 500) / 6000)
    val headY = 1.5f * listen * bump(m % 4000, 0f, 500f) + .8f * speak * sin(TAU * m / 400)
    val pivot = Offset(60f, 74f)

    drawRect(Glow, Offset.Zero, Size(120f, 120f))
    translate(0f, -1.5f * breath - 6f * joy) {
        scale(1f + .02f * breath, Offset(60f, 120f)) {
            look.back?.let { rotate(sway + hairSway, pivot) { drawPath(it, look.hair) } }
            drawRect(skinShade, Offset(53f, 68f), Size(14f, 24f))
            drawOval(Shade, Offset(50f, 74f), Size(20f, 8f))
            drawPath(BodyPath, look.body)
            drawOval(skinShade, Offset(48f, 106f), Size(13f, 10f))
            drawOval(skinShade, Offset(59f, 106f), Size(13f, 10f))
            rotate(sway, pivot) {
                translate(0f, headY) { drawHead(look, skin, skinShade, t, m, blinks, listen, think, speak, joy, hairSway, talk) }
            }
        }
    }
    if (think > 0f) for (i in 0..2) {
        val pulse = (1 - cos(TAU * (m / 900 - i * .2f))) / 2
        drawCircle(look.accent, 1.6f + .4f * i, Offset(86f + 6f * i, 28f - 4f * i), alpha = think * (.35f + .65f * pulse))
    }
    if (spark < 1f) for (a in SparkAngles) {
        val d = 26f + 16f * spark
        translate(60f + cos(a) * d, 44f + sin(a) * d) {
            scale(3f * (1 - spark) + 1f, Offset.Zero) { drawPath(Star, Spark, alpha = 1 - spark) }
        }
    }
}

private fun DrawScope.drawHead(
    look: Look, skin: Brush, skinShade: Color, t: Float, m: Float, blinks: FloatArray,
    listen: Float, think: Float, speak: Float, joy: Float, hairSway: Float, talk: Float
) {
    drawOval(skinShade, Offset(32f, 44f), Size(8f, 14f))
    drawOval(skinShade, Offset(80f, 44f), Size(8f, 14f))
    if (look.female) {
        drawCircle(Gold, 1.6f, Offset(36f, 58f))
        drawCircle(Gold, 1.6f, Offset(84f, 58f))
    }
    drawPath(if (look.female) FaceF else FaceM, skin)
    drawArc(Rim, -70f, 120f, false, Offset(36f, 22f), Size(48f, 56f), style = Line)
    drawCircle(BlushL, 6f, Offset(44f, 61f))
    drawCircle(BlushR, 6f, Offset(76f, 61f))
    if (!look.female) drawOval(Stubble, Offset(47f, 64f), Size(26f, 14f))

    val glance = bump(m, 8500f, 1600f) - bump(m, 3000f, 1600f)
    val gx = 1.4f * glance * (1 - think) + 1.3f * think
    val open = maxOf(eyeOpen(t, blinks), .1f)
    for (cx in EyeX) {
        if (look.serene) drawArc(Ink, 15f, 150f, false, Offset(cx - 5f, 49f), Size(10f, 6f), style = Line)
        else drawEye(cx, open, 1f + .12f * listen, gx, -1.3f * think, look.female)
    }
    val lift = 3f * joy
    val brow = if (look.female) BrowF else BrowM
    val bh = if (look.female) 6f else 4f
    drawArc(look.hair, 200f, 120f, false, Offset(44f, 41f - lift), Size(11f, bh), style = brow)
    drawArc(look.hair, 220f, 120f, false, Offset(65f, 41f - lift - 2.5f * think), Size(11f, bh), style = brow)
    drawArc(Shade, 20f, 140f, false, Offset(57f, 56f), Size(6f, 4f), style = Line)

    val w = 12f + 4f * joy
    drawArc(Lips, 20f, 140f, false, Offset(60f - w / 2, 60f), Size(w, 8f + 2f * joy), alpha = 1 - speak, style = Line)
    if (speak > 0f) {
        // openness 0 = closed, .5 = small, 1 = open; from the voice signal, else cycling ~6 shapes a second
        val o = if (talk >= 0f) talk else (m / 167).toInt() % 3 / 2f
        val k = if (o < .5f) o * 2 else o * 2 - 1
        val mw = if (o < .5f) 10f - 2f * k else 8f + 2f * k
        val mh = if (o < .5f) 1.8f + 2.2f * k else 4f + 3f * k
        drawOval(Lips, Offset(60f - mw / 2, 66f - mh / 2), Size(mw, mh), alpha = speak)
    }
    if (look.glasses) {
        drawCircle(Ink, 6.5f, Offset(49.5f, 52f), style = Thin)
        drawCircle(Ink, 6.5f, Offset(70.5f, 52f), style = Thin)
        drawLine(Ink, Offset(56f, 52f), Offset(64f, 52f), 1f)
    }

    rotate(hairSway, Offset(60f, 30f)) {
        translate(0f, 1.5f) { drawPath(look.front, Shade) }
        drawPath(look.front, look.hair)
        drawArc(look.shine, 205f, 50f, false, Offset(44f, 14f), Size(28f, 28f), style = Shine)
    }
    drawPath(BandPath, BandColor)
    look.extra?.let { rotate(hairSway, Offset(60f, 30f)) { drawPath(it, look.accent) } }
}

// sclera, iris with gradient, pupil and highlight, optional lashes; squashed vertically to blink
private fun DrawScope.drawEye(cx: Float, open: Float, widen: Float, gx: Float, gy: Float, lashes: Boolean) {
    scale(widen, widen * open, Offset(cx, 52f)) {
        drawOval(Sclera, Offset(cx - 5f, 46.5f), Size(10f, 11f))
        translate(cx + gx, 52f + gy) {
            drawCircle(Iris, 3.4f, Offset.Zero)
            drawCircle(Ink, 1.6f, Offset.Zero)
            drawCircle(Color.White, .9f, Offset(-1f, -1.2f))
        }
        drawArc(Ink, 200f, 140f, false, Offset(cx - 5f, 46.5f), Size(10f, 11f), style = Thin)
        if (lashes) {
            val d = if (cx < 60f) -1f else 1f // toward the outer corner
            drawLine(Ink, Offset(cx + 4f * d, 48.5f), Offset(cx + 6.5f * d, 46f), 1f, StrokeCap.Round)
            drawLine(Ink, Offset(cx + 2.5f * d, 47.2f), Offset(cx + 4f * d, 44.6f), 1f, StrokeCap.Round)
        }
    }
}
