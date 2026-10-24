package com.nya.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.data.Companion
import com.nya.app.data.Gender
import com.nya.app.data.Prefs
import com.nya.app.data.ble.LinkState
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.AppViewModel
import com.nya.app.viewmodel.AuthViewModel
import com.nya.app.viewmodel.DeviceViewModel
import com.nya.app.viewmodel.valueOrNull

@Composable
fun Para(text: String) {
    Text(text, color = LocalNya.current.sb, fontSize = 15.sp, lineHeight = 22.sp)
}

// back arrow plus the system back gesture for onboarding steps
@Composable
private fun StepBack(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    IconBtn(NyaIcons.Back, "Back", onClick = onBack)
}

@Composable
fun VerifyScreen(onVerified: () -> Unit) {
    val c = LocalNya.current
    val auth = viewModel<AuthViewModel>(factory = AuthViewModel.Factory)
    LaunchedEffect(Unit) { auth.clearMessages() }
    Page {
        Spacer(Modifier.height(40.dp))
        Heading("Check your email")
        Spacer(Modifier.height(12.dp))
        Para("We sent a verification link to your email. Open it, then come back and tap Continue.")
        Spacer(Modifier.height(32.dp))
        NyaButton("Continue", { auth.checkVerified(onVerified) }, loading = auth.busy)
        AuthMessage(auth.error, auth.info)
        Text("Didn't get it? Resend email", color = c.ac, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp).pressScale(onClick = auth::resend)
                .minimumInteractiveComponentSize())
    }
}

@Composable
fun ResetScreen(onBack: () -> Unit) {
    val auth = viewModel<AuthViewModel>(factory = AuthViewModel.Factory)
    var email by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { auth.clearMessages() }
    Page {
        Spacer(Modifier.height(40.dp))
        Heading("Reset password")
        Spacer(Modifier.height(12.dp))
        Para("Enter your email and we'll send you a link to set a new password.")
        Spacer(Modifier.height(28.dp))
        NyaField("Email", email, { email = it; auth.clearError() })
        Spacer(Modifier.height(24.dp))
        NyaButton("Send reset link", { auth.sendReset(email) }, loading = auth.busy)
        AuthMessage(auth.error, auth.info)
        Spacer(Modifier.height(16.dp))
        NyaOutlineButton("Back to sign in", onBack)
    }
}

@Composable
fun CompanionScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    val prefs = LocalPrefs.current
    val app = viewModel<AppViewModel>(factory = AppViewModel.Factory)
    var sel by remember { mutableIntStateOf(prefs.companion.ordinal) }
    Page {
        StepBack(onBack)
        Spacer(Modifier.height(20.dp))
        Heading("Choose your companion")
        Spacer(Modifier.height(12.dp))
        Para("Each one thinks and talks a little differently. You can change this later in Settings.")
        Spacer(Modifier.height(24.dp))
        CompanionList(Companion.entries[sel]) { sel = it.ordinal }
        Spacer(Modifier.height(12.dp))
        NyaButton("Continue", { app.setCompanion(Companion.entries[sel]); onContinue() })
    }
}

private val CompanionBlurbs = listOf(
    "Curious about places and discoveries",
    "Style, taste and inspiration",
    "Calm, clear answers",
    "Mindful and steady",
    "Ideas and making things"
)

/** The five companion cards as a radio group; the avatar uses the profile's skin and gender. */
@Composable
fun CompanionList(selected: Companion, onSelect: (Companion) -> Unit) {
    val c = LocalNya.current
    val prefs = LocalPrefs.current
    Companion.entries.forEach { companion ->
        val on = companion == selected
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).pressScale(role = Role.RadioButton, selected = on) { onSelect(companion) }.clip(shape).background(c.sf)
                .border(if (on) 2.dp else 1.dp, if (on) c.ac else c.ln, shape)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(c.b1, c.b2))),
                contentAlignment = Alignment.Center
            ) { Avatar2D(Modifier.fillMaxSize(), companion, Color(prefs.skin), prefs.gender) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(companion.name, color = c.tx, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(CompanionBlurbs[companion.ordinal], color = c.sb, fontSize = 13.sp)
            }
        }
    }
}

private val Skins = listOf(0xFFF3D5BC, 0xFFE0AC84, 0xFFC68A5E, 0xFF8D5A3B, 0xFF5A3A27)

private fun previewVoice(prefs: Prefs) {
    val voice = VoiceEngine.chosen(prefs.gender, prefs.voice) ?: return
    VoiceEngine.speak("Hello, ${prefs.name}! I'm ${prefs.companion.name}. Nice to meet you.", voice)
}

@Composable
fun AvatarScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    val c = LocalNya.current
    val prefs = LocalPrefs.current
    val app = viewModel<AppViewModel>(factory = AppViewModel.Factory)
    var skin by remember { mutableIntStateOf(Skins.indexOfFirst { Color(it).toArgb() == prefs.skin }.takeIf { it >= 0 } ?: 1) }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        VoiceEngine.init(context)
        onDispose { VoiceEngine.stop() }
    }
    Column(Modifier.fillMaxSize().nyaBg().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            StepBack(onBack)
            Spacer(Modifier.height(20.dp))
            Heading("Make it yours")
            Spacer(Modifier.height(20.dp))
            AvatarPreview(Color(Skins[skin]))
            Spacer(Modifier.height(24.dp))
            Text("Gender", color = c.sb, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            GenderPicker()
            Spacer(Modifier.height(20.dp))
            Text("Skin tone", color = c.sb, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Skins.forEachIndexed { i, col ->
                    Box(
                        Modifier.pressScale(role = Role.RadioButton, selected = i == skin, description = "Skin tone ${i + 1}") { skin = i }
                            .size(44.dp)
                            .clip(CircleShape).background(Color(col))
                            .border(if (i == skin) 3.dp else 1.dp, if (i == skin) c.ac else c.ln, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            VoicePicker(prefs, app::setVoice)
        }
        NyaButton(
            "Continue", { app.setSkin(Color(Skins[skin]).toArgb()); onContinue() },
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

// own recompose scopes, so speaking or chip changes don't redraw the whole screen
@Composable
private fun AvatarPreview(skin: Color) {
    val c = LocalNya.current
    val prefs = LocalPrefs.current
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().height(220.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(c.sf, c.bg))).border(1.dp, c.ln, shape),
        contentAlignment = Alignment.Center
    ) {
        NyaOrb(130.dp) {
            Crossfade(prefs.gender to skin, animationSpec = motionSpec(), label = "look") { (gender, tone) ->
                Avatar2D(
                    Modifier.fillMaxSize(), prefs.companion, tone, gender,
                    if (VoiceEngine.speaking) AvatarState.Speaking else AvatarState.Idle, mouth = VoiceEngine.mouth
                )
            }
        }
    }
}

@Composable
private fun GenderPicker() {
    val prefs = LocalPrefs.current
    val app = viewModel<AppViewModel>(factory = AppViewModel.Factory)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Gender.entries.forEach { g ->
            NyaChip(g.name, prefs.gender == g, { app.setGender(g); previewVoice(prefs.copy(gender = g)) }, Modifier.weight(1f))
        }
    }
}

/** Voice chips for [prefs].gender with a preview; [prefs] holds the values being edited, [onPick] gets the chosen index. */
@Composable
fun VoicePicker(prefs: Prefs, onPick: (Int) -> Unit) {
    val c = LocalNya.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Voice style", color = c.sb, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (VoiceEngine.available) IconBtn(
            if (VoiceEngine.speaking) NyaIcons.Stop else NyaIcons.Play,
            if (VoiceEngine.speaking) "Stop" else "Play voice preview", 36.dp
        ) {
            if (VoiceEngine.speaking) VoiceEngine.stop() else previewVoice(prefs)
        }
    }
    Spacer(Modifier.height(10.dp))
    if (VoiceEngine.ready && !VoiceEngine.available) {
        Text("Voices aren't available on this device.", color = c.sb, fontSize = 13.sp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val chosen = VoiceEngine.chosen(prefs.gender, prefs.voice)
        VoiceEngine.options(prefs.gender).forEachIndexed { i, v ->
            NyaChip(v.name, v == chosen, { onPick(i); previewVoice(prefs.copy(voice = i)) }, Modifier.weight(1f), hPad = 6.dp)
        }
    }
}

@Composable
fun PermissionsScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    val c = LocalNya.current
    val items = listOf(
        "Bluetooth" to "Connect to your Nya headband",
        "Notifications" to "Know when clips finish syncing",
        "Photos" to "Save memories to your phone"
    )
    Page {
        StepBack(onBack)
        Spacer(Modifier.height(20.dp))
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
fun PairScreen(onConnect: () -> Unit, onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<DeviceViewModel>(factory = DeviceViewModel.Factory)
    val connecting by vm.connecting.collectAsState()
    val linking = vm.device.collectAsState().value.valueOrNull()?.state == LinkState.Connecting
    // top, radar and bottom spread over the screen when they fit and scroll when they don't
    BoxWithConstraints(Modifier.fillMaxSize().nyaBg().statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth()) { StepBack(onBack) }
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, c.ac, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) { Text("Required", color = c.ac, fontSize = 12.sp) }
            }
            Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
                PulseRing(0)
                PulseRing(1200)
                Box(Modifier.size(110.dp).clip(CircleShape).background(Brush.radialGradient(listOf(c.b1, c.b2, c.sf))))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        Text(if (connecting || linking) "Connecting…" else "Ready to pair", color = c.sb, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                NyaButton("Connect", { vm.connect(onFail = { SubNav.open("PairFail") }, onSuccess = onConnect) }, loading = connecting)
            }
        }
    }
}

