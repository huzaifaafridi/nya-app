package com.nya.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

@Composable
fun Para(text: String) {
    Text(text, color = LocalNya.current.sb, fontSize = 15.sp, lineHeight = 22.sp)
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.height(42.dp).clip(shape)
            .background(if (selected) c.ac else c.sf)
            .border(1.dp, if (selected) c.ac else c.ln, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) c.on else c.tx, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun VerifyScreen(onVerified: () -> Unit) {
    val c = LocalNya.current
    var code by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Page {
        Spacer(Modifier.height(40.dp))
        Heading("Check your email")
        Spacer(Modifier.height(12.dp))
        Para("We sent a 6-digit code to your email. Enter it below.")
        Spacer(Modifier.height(32.dp))
        BasicTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { inner ->
                Box {
                    Row(
                        Modifier.fillMaxWidth().clickable { focus.requestFocus() },
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(6) { i ->
                            val shape = RoundedCornerShape(14.dp)
                            Box(
                                Modifier.weight(1f).height(60.dp).clip(shape).background(c.sf)
                                    .border(1.dp, if (i == code.length) c.ac else c.ln, shape),
                                contentAlignment = Alignment.Center
                            ) { Text(code.getOrNull(i)?.toString() ?: "", color = c.tx, fontSize = 24.sp) }
                        }
                    }
                    Box(Modifier.size(1.dp).alpha(0f)) { inner() }
                }
            }
        )
        Spacer(Modifier.height(28.dp))
        NyaButton("Verify", onVerified)
        Text("Didn't get it? Resend code", color = c.ac, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).clickable { })
    }
}

@Composable
fun ResetScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    Page {
        Spacer(Modifier.height(40.dp))
        Heading("Reset password")
        Spacer(Modifier.height(12.dp))
        Para("Enter your email and we'll send you a link to set a new password.")
        Spacer(Modifier.height(28.dp))
        NyaField("Email", email, { email = it })
        Spacer(Modifier.height(24.dp))
        NyaButton("Send reset link", onBack)
        Spacer(Modifier.height(16.dp))
        NyaOutlineButton("Back to sign in", onBack)
    }
}

@Composable
fun CompanionScreen(onContinue: () -> Unit) {
    val c = LocalNya.current
    val items = listOf(
        "Wanderer" to "Curious about places and discoveries",
        "Muse" to "Style, taste and inspiration",
        "Guide" to "Calm, clear answers",
        "Yogi" to "Mindful and steady",
        "Creator" to "Ideas and making things"
    )
    var sel by remember { mutableStateOf(1) }
    Page {
        Spacer(Modifier.height(32.dp))
        Heading("Choose your companion")
        Spacer(Modifier.height(12.dp))
        Para("Each one thinks and talks a little differently. You can change this later in Settings.")
        Spacer(Modifier.height(24.dp))
        items.forEachIndexed { i, (name, desc) ->
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(shape).background(c.sf)
                    .border(if (i == sel) 2.dp else 1.dp, if (i == sel) c.ac else c.ln, shape)
                    .clickable { sel = i }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(c.b1, c.b2))),
                    contentAlignment = Alignment.Center
                ) { Text(name.take(1), color = c.on, fontSize = 20.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name, color = c.tx, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = c.sb, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NyaButton("Continue", onContinue)
    }
}

@Composable
fun AvatarScreen(onContinue: () -> Unit) {
    val c = LocalNya.current
    val skins = listOf(0xFFF3D5BC, 0xFFE0AC84, 0xFFC68A5E, 0xFF8D5A3B, 0xFF5A3A27)
    var skin by remember { mutableStateOf(1) }
    var voice by remember { mutableStateOf(0) }
    Page {
        Spacer(Modifier.height(32.dp))
        Heading("Make it yours")
        Spacer(Modifier.height(20.dp))
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.fillMaxWidth().height(240.dp).clip(shape)
                .background(Brush.verticalGradient(listOf(c.sf, c.bg))).border(1.dp, c.ln, shape),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(130.dp).clip(CircleShape).background(Color(skins[skin])))
            Spacer(Modifier.height(16.dp))
            Text("3D preview: drag to rotate", color = c.sb, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("Skin tone", color = c.sb, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            skins.forEachIndexed { i, col ->
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color(col))
                        .border(if (i == skin) 3.dp else 1.dp, if (i == skin) c.ac else c.ln, CircleShape)
                        .clickable { skin = i }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Voice", color = c.sb, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Soft", "Neutral", "Deep").forEachIndexed { i, t ->
                Chip(t, voice == i, { voice = i }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(32.dp))
        NyaButton("Continue", onContinue)
    }
}

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val c = LocalNya.current
    val items = listOf(
        "Bluetooth" to "Connect to your Nya headband",
        "Notifications" to "Know when clips finish syncing",
        "Photos" to "Save memories to your phone"
    )
    Page {
        Spacer(Modifier.height(32.dp))
        Heading("A few permissions")
        Spacer(Modifier.height(12.dp))
        Para("Nya needs these to work. You can change them any time in Settings.")
        Spacer(Modifier.height(24.dp))
        items.forEach { (t, d) ->
            val shape = RoundedCornerShape(20.dp)
            Column(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(shape).background(c.sf)
                    .border(1.dp, c.ln, shape).padding(18.dp)
            ) {
                Text(t, color = c.tx, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(d, color = c.sb, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        NyaButton("Allow and continue", onContinue)
    }
}

@Composable
fun PulseRing(offsetMs: Int) {
    val c = LocalNya.current
    val t = rememberInfiniteTransition(label = "ring$offsetMs")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart, StartOffset(offsetMs)),
        label = "p"
    )
    Box(
        Modifier.size(120.dp)
            .graphicsLayer { scaleX = 1f + p * 1.2f; scaleY = 1f + p * 1.2f; alpha = 1f - p }
            .border(2.dp, c.ac, CircleShape)
    )
}

@Composable
fun PairScreen(onConnect: () -> Unit) {
    val c = LocalNya.current
    Column(
        Modifier.fillMaxSize().nyaBg().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, c.ac, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) { Text("Required", color = c.ac, fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            PulseRing(0)
            PulseRing(1200)
            Box(Modifier.size(110.dp).clip(CircleShape).background(Brush.radialGradient(listOf(c.b1, c.b2, c.sf))))
        }
        Spacer(Modifier.weight(1f))
        Heading("Find your Nya")
        Spacer(Modifier.height(8.dp))
        Text("Turn on your headband and keep it close.", color = c.sb, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(c.sf).border(1.dp, c.ln, shape).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.radialGradient(listOf(c.b1, c.b2))))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("NYA F1", color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Ready to pair", color = c.sb, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        NyaButton("Connect", onConnect)
    }
}

