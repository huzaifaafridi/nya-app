package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

object SubNav {
    var open: (String) -> Unit = {}
    var home: () -> Unit = {}
    var signOut: () -> Unit = {}
}

object ThemePref {
    var mode by mutableStateOf(0) // 0 = Auto, 1 = Light, 2 = Dark
}

val NyaGreen = Color(0xFF6BCB8B)

@Composable
fun XTop(title: String, onBack: () -> Unit) {
    val c = LocalNya.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", color = c.tx, fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }.padding(end = 16.dp))
        Text(title, color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun XBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = LocalNya.current
    Box(
        modifier.height(54.dp).clip(RoundedCornerShape(27.dp))
            .background(Brush.horizontalGradient(listOf(c.b1, c.b2)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.on, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun XGhost(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.height(54.dp).clip(shape).border(1.dp, c.ln, shape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.tx, fontSize = 16.sp) }
}

@Composable
fun XCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(20.dp)
    var m = modifier.fillMaxWidth().clip(shape).background(c.sf).border(1.dp, c.ln, shape)
    if (onClick != null) m = m.clickable { onClick() }
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
    if (onClick != null) m = m.clickable { onClick() }
    Row(m.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.tx, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = c.sb, fontSize = 14.sp)
        if (onClick != null) Text("  ›", color = c.sb, fontSize = 18.sp)
    }
}

@Composable
fun XToggle(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalNya.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.tx, fontSize = 15.sp)
            if (sub.isNotEmpty()) Text(sub, color = c.sb, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.on, checkedTrackColor = c.ac,
                uncheckedThumbColor = c.sb, uncheckedTrackColor = c.sf, uncheckedBorderColor = c.ln
            )
        )
    }
}

@Composable
fun XLabel(text: String) {
    val c = LocalNya.current
    Text(text, color = c.ac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
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
        "Help" -> HelpScreen(onBack)
        "ReadOnly" -> ReadOnlyScreen(onBack)
        "PairFail" -> PairFailScreen(onBack)
        "Transfer" -> TransferScreen(onBack)
        "FindMy" -> FindMyScreen(onBack)
        "Capture" -> CaptureScreen(onBack)

        else -> {}
    }
}

