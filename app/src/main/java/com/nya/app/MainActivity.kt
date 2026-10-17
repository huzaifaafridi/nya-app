package com.nya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.nya.app.ui.*
import com.nya.app.ui.theme.NyaTheme

enum class Screen {
    Welcome, SignIn, SignUp, Verify, Reset, Companion, Avatar, Permissions, Pair, Greeting,Home,Chat ,Detail,Sub
}

// what's on screen; sub screens are keyed by name and stack depth so push/pop animate too
data class Dest(val screen: Screen, val sub: String, val depth: Int) {
    // forward = later in the flow, or one level deeper into sub screens
    fun isAfter(o: Dest) =
        if (screen == Screen.Sub && o.screen == Screen.Sub) depth >= o.depth
        else screen.ordinal > o.screen.ordinal
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            loadPrefs(this@MainActivity)
            showContent()
        }
    }

    private fun showContent() {
        setContent {
            NyaTheme(dark = when (ThemePref.mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }) {
                var s by remember { mutableStateOf(Screen.Welcome) }
                var memIdx by remember { mutableStateOf(0) }
                var sub by remember { mutableStateOf("") }
                val subStack = remember { mutableStateListOf<String>() }
                Nav.toChat = { s = Screen.Chat }
                SubNav.open = { name ->
                    if (s == Screen.Sub) subStack.add(sub)
                    sub = name
                    s = Screen.Sub
                }
                SubNav.home = { subStack.clear(); s = Screen.Home }
                SubNav.signOut = { s = Screen.Welcome }
                MemNav.open = { memIdx = it; s = Screen.Detail }
                val reduce = rememberReduceMotion()
                val slide = with(LocalDensity.current) { 24.dp.roundToPx() }
                val dest = Dest(s, if (s == Screen.Sub) sub else "", if (s == Screen.Sub) subStack.size else 0)
                CompositionLocalProvider(LocalReduceMotion provides reduce) {
                    AnimatedContent(
                        targetState = dest,
                        transitionSpec = { screenTransition(targetState.isAfter(initialState), slide, reduce) },
                        label = "nav"
                    ) { (screen, subName) ->
                        when (screen) {
                            Screen.Welcome -> WelcomeScreen({ s = Screen.SignUp }, { s = Screen.SignIn })
                            Screen.SignIn -> SignInScreen({ s = Screen.SignUp }, { s = Screen.Reset }, { s = Screen.Greeting })
                            Screen.SignUp -> SignUpScreen({ s = Screen.SignIn }, { s = Screen.Verify })
                            Screen.Verify -> VerifyScreen { s = Screen.Companion }
                            Screen.Reset -> ResetScreen { s = Screen.SignIn }
                            Screen.Companion -> CompanionScreen { s = Screen.Avatar }
                            Screen.Avatar -> AvatarScreen { s = Screen.Permissions }
                            Screen.Permissions -> PermissionsScreen { s = Screen.Pair }
                            Screen.Pair -> PairScreen { s = Screen.Greeting }
                            Screen.Greeting -> GreetingScreen(UserPref.name) { s = Screen.Home }
                            Screen.Home -> MainTabs()
                            Screen.Chat -> ChatScreen(onBack = { s = Screen.Home })
                            Screen.Detail -> MemoryDetailScreen(memIdx, onBack = { s = Screen.Home })
                            Screen.Sub -> SubRouter(subName, onBack = {
                                if (subStack.isNotEmpty()) sub = subStack.removeAt(subStack.lastIndex)
                                else s = Screen.Home
                            })
                        }
                    }
                }
            }
        }
    }
}