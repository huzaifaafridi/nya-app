package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.data.AppConfig
import com.nya.app.data.Prefs
import com.nya.app.ui.theme.LocalNya
import com.nya.app.ui.theme.NyaDark

object SubNav {
    var open: (String) -> Unit = {}
    var home: () -> Unit = {}
    var signOut: () -> Unit = {}
}

val LocalPrefs = compositionLocalOf { Prefs() }
val LocalConfig = compositionLocalOf { AppConfig() }

// status colors: lighter on dark, darker on light so the text keeps its contrast
val NyaGreen: Color @Composable get() = if (LocalNya.current == NyaDark) Color(0xFF6BCB8B) else Color(0xFF1E7A45)
val NyaDanger: Color @Composable get() = if (LocalNya.current == NyaDark) Color(0xFFE5736A) else Color(0xFFB3382E)

fun Modifier.asHeading(): Modifier = semantics { heading() }

/** One toggleable node (label + state read once); the Switch/Checkbox inside gets onCheckedChange = null. */
fun Modifier.toggleRow(checked: Boolean, role: Role, onChange: (Boolean) -> Unit): Modifier =
    toggleable(checked, null, null, role = role, onValueChange = onChange)

@Composable
fun XTop(title: String, onBack: () -> Unit) {
    val c = LocalNya.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 2.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).pressScale(description = "Back") { onBack() },
            contentAlignment = Alignment.Center) { Text("‹", color = c.tx, fontSize = 30.sp) }
        Text(title, color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.asHeading())
    }
}

@Composable
fun XBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = LocalNya.current
    Box(
        modifier.heightIn(min = 54.dp).pressScale { onClick() }.clip(RoundedCornerShape(27.dp))
            .background(Brush.horizontalGradient(listOf(c.b1, c.b2))).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.on, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
}

@Composable
fun XGhost(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.heightIn(min = 54.dp).pressScale { onClick() }.clip(shape).border(1.dp, c.ln, shape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.tx, fontSize = 16.sp, textAlign = TextAlign.Center) }
}

@Composable
fun XCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(20.dp)
    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.pressScale { onClick() }
    m = m.clip(shape).background(c.sf).border(1.dp, c.ln, shape)
    Column(m.padding(18.dp), content = content)
}

@Composable
fun XDivider() {
    val c = LocalNya.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.ln))
}

@Composable
fun XRow(title: String, value: String = "", onClick: (() -> Unit)? = null) {
    val c = LocalNya.current
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.pressScale { onClick() }
    Row(m.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.tx, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = c.sb, fontSize = 14.sp)
        if (onClick != null) Text("  ›", color = c.sb, fontSize = 18.sp, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
fun XToggle(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalNya.current
    Row(
        Modifier.fillMaxWidth().toggleRow(checked, Role.Switch, onChange).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.tx, fontSize = 15.sp)
            if (sub.isNotEmpty()) Text(sub, color = c.sb, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked, onCheckedChange = null, modifier = Modifier.minimumInteractiveComponentSize(),
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.on, checkedTrackColor = c.ac,
                uncheckedThumbColor = c.sb, uncheckedTrackColor = c.sf, uncheckedBorderColor = c.ln
            )
        )
    }
}

@Composable
fun XLabel(text: String, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    Text(text, color = c.ac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 24.dp, bottom = 10.dp))
}

@Composable
fun XBar(progress: Float, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.ln)) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(c.b1, c.b2)))
        )
    }
}

// every extra screen gets one line added here as we build it
@Composable
fun SubRouter(name: String, onBack: () -> Unit) {
    when (name) {
        "Settings" -> SettingsScreen(onBack)
        "Subscription" -> SubscriptionScreen(onBack)
        "EditProfile" -> EditProfileScreen(onBack)
        "Help" -> HelpScreen(onBack)
        "ReadOnly" -> ReadOnlyScreen(onBack)
        "PairFail" -> PairFailScreen(onBack)
        "Transfer" -> TransferScreen(onBack)
        "FindMy" -> FindMyScreen(onBack)
        "Capture" -> CaptureScreen(onBack)
        "HeadbandSettings" -> HeadbandSettingsScreen(onBack)
        "Firmware" -> FirmwareScreen(onBack)

        else -> {}
    }
}

