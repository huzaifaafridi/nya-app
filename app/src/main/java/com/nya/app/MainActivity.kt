package com.nya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.nya.app.ui.*
import com.nya.app.ui.theme.NyaTheme

enum class Screen {
    Welcome, SignIn, SignUp, Verify, Reset, Companion, Avatar, Permissions, Pair, Greeting, Home
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NyaTheme {
                var s by remember { mutableStateOf(Screen.Welcome) }
                Crossfade(targetState = s, animationSpec = tween(400), label = "nav") { screen ->
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
                        Screen.Greeting -> GreetingScreen("Mercy") { s = Screen.Home }
                        Screen.Home -> HomePlaceholder()
                    }
                }
            }
        }
    }
}