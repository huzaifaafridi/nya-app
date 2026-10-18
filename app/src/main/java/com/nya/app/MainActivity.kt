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
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nya.app.data.Prefs
import com.nya.app.ui.*
import com.nya.app.ui.theme.NyaTheme
import com.nya.app.viewmodel.AppViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    private val app: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val first = app.prefs.filterNotNull().first()
            showContent(first)
        }
    }

    override fun onDestroy() {
        if (isFinishing) VoiceEngine.shutdown()
        super.onDestroy()
    }

    private fun showContent(first: Prefs) {
        setContent {
            val prefs = app.prefs.collectAsState().value ?: first
            NyaTheme(dark = when (prefs.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }) {
                var s by remember { mutableStateOf(if (first.onboarded && app.isSignedIn) Screen.Home else Screen.Welcome) }
                var memId by remember { mutableLongStateOf(0L) }
                var sub by remember { mutableStateOf("") }
                val subStack = remember { mutableStateListOf<String>() }
                Nav.toChat = { s = Screen.Chat }
                SubNav.open = { name ->
                    if (s == Screen.Sub) subStack.add(sub)
                    sub = name
                    s = Screen.Sub
                }
                SubNav.home = { subStack.clear(); s = Screen.Home }
                SubNav.signOut = { app.signOut(this@MainActivity); subStack.clear(); s = Screen.Welcome }
                MemNav.open = { memId = it; s = Screen.Detail }
                val reduce = rememberReduceMotion()
                val slide = with(LocalDensity.current) { 24.dp.roundToPx() }
                val dest = Dest(s, if (s == Screen.Sub) sub else "", if (s == Screen.Sub) subStack.size else 0)
                CompositionLocalProvider(LocalPrefs provides prefs, LocalReduceMotion provides reduce) {
                    AnimatedContent(
                        targetState = dest,
                        transitionSpec = { screenTransition(targetState.isAfter(initialState), slide, reduce) },
                        label = "nav"
                    ) { (screen, subName) ->
                        when (screen) {
                            Screen.Welcome -> WelcomeScreen({ s = Screen.SignUp }, { s = Screen.SignIn })
                            Screen.SignIn -> SignInScreen({ s = Screen.SignUp }, { s = Screen.Reset }, { s = Screen.Greeting },
                                { s = Screen.Verify }, { s = if (it.isNew) Screen.Companion else Screen.Greeting })
                            Screen.SignUp -> SignUpScreen({ s = Screen.SignIn }, { s = Screen.Verify },
                                { s = if (it.isNew) Screen.Companion else Screen.Greeting })
                            Screen.Verify -> VerifyScreen { s = Screen.Companion }
                            Screen.Reset -> ResetScreen { s = Screen.SignIn }
                            Screen.Companion -> CompanionScreen({ s = Screen.Avatar }, { s = Screen.Verify })
                            Screen.Avatar -> AvatarScreen({ s = Screen.Permissions }, { s = Screen.Companion })
                            Screen.Permissions -> PermissionsScreen({ s = Screen.Pair }, { s = Screen.Avatar })
                            Screen.Pair -> PairScreen({ s = Screen.Greeting }, { s = Screen.Permissions })
                            Screen.Greeting -> GreetingScreen(prefs.name) { app.completeOnboarding(); s = Screen.Home }
                            Screen.Home -> MainTabs()
                            Screen.Chat -> ChatScreen(onBack = { s = Screen.Home })
                            Screen.Detail -> MemoryDetailScreen(memId, onBack = { s = Screen.Home })
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