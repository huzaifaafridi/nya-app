package com.nya.app.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SubNav {
    var open: (String) -> Unit = {}
    var home: () -> Unit = {}
    var signOut: () -> Unit = {}
}

private val Context.userStore by preferencesDataStore("user")

// Prefs below live in Compose state for instant reads; every change is also persisted here.
private object Store {
    val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var data: DataStore<Preferences>? = null

    fun save(block: (MutablePreferences) -> Unit) {
        val s = data ?: return
        io.launch { s.edit(block) }
    }
}

// Call once before the first frame so saved values (especially the theme) apply immediately.
suspend fun loadPrefs(context: Context) {
    val s = context.applicationContext.userStore
    Store.data = s
    val p = runCatching { s.data.first() }.getOrNull() ?: return
    ThemePref.load(p)
    UserPref.load(p)
}

object ThemePref {
    private val THEME = intPreferencesKey("theme")

    private var _mode by mutableStateOf(0) // 0 = Auto, 1 = Light, 2 = Dark
    var mode: Int
        get() = _mode
        set(v) { _mode = v; Store.save { it[THEME] = v } }

    fun load(p: Preferences) { p[THEME]?.let { _mode = it } }
}

object UserPref {
    private val MODE = intPreferencesKey("mode")
    private val QUIET = booleanPreferencesKey("quiet")
    private val COMPANION = intPreferencesKey("companion")
    private val SKIN = intPreferencesKey("skin")
    private val VOICE = intPreferencesKey("voice")
    private val GENDER = intPreferencesKey("gender")

    var name by mutableStateOf("Mercy")

    private var _mode by mutableStateOf(2) // 0 = Travel, 1 = Focus, 2 = Wellness, 3 = Creator
    var mode: Int
        get() = _mode
        set(v) { _mode = v; Store.save { it[MODE] = v } }

    private var _quiet by mutableStateOf(false)
    var quiet: Boolean
        get() = _quiet
        set(v) { _quiet = v; Store.save { it[QUIET] = v } }

    private var _companion by mutableStateOf(Companion.Muse)
    var companion: Companion
        get() = _companion
        set(v) { _companion = v; Store.save { it[COMPANION] = v.ordinal } }

    private var _skin by mutableStateOf(DefaultSkin)
    var skin: Color
        get() = _skin
        set(v) { _skin = v; Store.save { it[SKIN] = v.toArgb() } }

    private var _gender by mutableStateOf(Gender.Female)
    var gender: Gender
        get() = _gender
        set(v) { _gender = v; Store.save { it[GENDER] = v.ordinal } }

    private var _voice by mutableStateOf(0) // index into VoiceEngine.options
    var voice: Int
        get() = _voice
        set(v) { _voice = v; Store.save { it[VOICE] = v } }

    fun load(p: Preferences) {
        p[MODE]?.let { _mode = it }
        p[QUIET]?.let { _quiet = it }
        p[COMPANION]?.let { i -> Companion.entries.getOrNull(i)?.let { _companion = it } }
        p[SKIN]?.let { _skin = Color(it) }
        p[VOICE]?.let { _voice = it }
        p[GENDER]?.let { i -> Gender.entries.getOrNull(i)?.let { _gender = it } }
    }
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
            modifier = Modifier.pressScale { onBack() }.padding(end = 16.dp))
        Text(title, color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun XBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = LocalNya.current
    Box(
        modifier.height(54.dp).pressScale { onClick() }.clip(RoundedCornerShape(27.dp))
            .background(Brush.horizontalGradient(listOf(c.b1, c.b2))),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.on, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun XGhost(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.height(54.dp).pressScale { onClick() }.clip(shape).border(1.dp, c.ln, shape),
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
        "Help" -> HelpScreen(onBack)
        "ReadOnly" -> ReadOnlyScreen(onBack)
        "PairFail" -> PairFailScreen(onBack)
        "Transfer" -> TransferScreen(onBack)
        "FindMy" -> FindMyScreen(onBack)
        "Capture" -> CaptureScreen(onBack)

        else -> {}
    }
}

