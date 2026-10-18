package com.nya.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.R
import com.nya.app.data.AuthUser
import com.nya.app.ui.theme.LocalNya
import com.nya.app.ui.theme.NyaDark
import com.nya.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    val c = LocalNya.current
    Column(
        Modifier.fillMaxSize().nyaBg().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        content = content
    )
}

@Composable
fun Heading(text: String) {
    Text(text, color = LocalNya.current.tx, fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic, fontSize = 38.sp)
}

@Composable
fun SocialCircle(content: @Composable () -> Unit) {
    val c = LocalNya.current
    Box(
        Modifier.size(54.dp).pressScale { }.clip(CircleShape).background(c.sf)
            .border(1.dp, c.ln, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun SocialRow(onGoogle: () -> Unit) {
    val c = LocalNya.current
    val dark = c == NyaDark
    Text("or continue with", color = c.sb, fontSize = 13.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally)
    ) {
        SocialCircle {
            Image(painterResource(R.drawable.ic_apple), "Apple", Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(c.tx))
        }
        Image(
            painterResource(if (dark) R.drawable.ic_google_dark else R.drawable.ic_google_light),
            "Google", Modifier.size(54.dp).pressScale(onClick = onGoogle).clip(CircleShape)
        )
        Image(
            painterResource(R.drawable.ic_facebook),
            "Facebook", Modifier.size(54.dp).pressScale { }.clip(CircleShape)
        )
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onSignIn: () -> Unit) {
    val c = LocalNya.current
    Column(
        Modifier.fillMaxSize().nyaBg().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(150.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(c.b1, c.b2, c.sf)))
        )
        Spacer(Modifier.height(28.dp))
        Text("Nya", color = c.tx, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 64.sp)
        Text("Fashionable AI", color = c.sb, fontSize = 15.sp, letterSpacing = 2.sp)
        Spacer(Modifier.weight(1f))
        NyaButton("Get started", onStart)
        Spacer(Modifier.height(12.dp))
        NyaOutlineButton("Sign in", onSignIn)
    }
}

@Composable
fun SignInScreen(
    onSignUp: () -> Unit, onForgot: () -> Unit, onHome: () -> Unit, onVerify: () -> Unit,
    onGoogle: (AuthUser) -> Unit
) {
    val c = LocalNya.current
    val context = LocalContext.current
    val auth = viewModel<AuthViewModel>(factory = AuthViewModel.Factory)
    LaunchedEffect(Unit) { auth.clearMessages() }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    Page {
        Spacer(Modifier.height(40.dp))
        Heading("Welcome back")
        Spacer(Modifier.height(28.dp))
        NyaField("Email", email, { email = it; auth.clearError() })
        Spacer(Modifier.height(16.dp))
        NyaField("Password", pass, { pass = it; auth.clearError() }, password = true)
        Text("Forgot password?", color = c.ac, fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp).pressScale(onClick = onForgot))
        Spacer(Modifier.height(24.dp))
        NyaButton("Sign in", { auth.signIn(email, pass) { if (it.verified) onHome() else onVerify() } },
            loading = auth.busy)
        AuthMessage(auth.error, auth.info)
        SocialRow { auth.google(context, onGoogle) }
        Text("New here? Create account", color = c.ac, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).pressScale(onClick = onSignUp))
    }
}

private fun dobText(d: String) =
    if (d.length == 8) "${d.take(2)}/${d.substring(2, 4)}/${d.drop(4)}" else d

@Composable
fun SignUpScreen(onSignIn: () -> Unit, onVerify: () -> Unit, onGoogle: (AuthUser) -> Unit) {
    val c = LocalNya.current
    val context = LocalContext.current
    val auth = viewModel<AuthViewModel>(factory = AuthViewModel.Factory)
    LaunchedEffect(Unit) { auth.clearMessages() }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    Page {
        Spacer(Modifier.height(24.dp))
        Heading("Create account")
        Spacer(Modifier.height(24.dp))
        NyaField("Full name", name, { name = it; auth.clearError() })
        Spacer(Modifier.height(14.dp))
        NyaField("Email", email, { email = it; auth.clearError() })
        Spacer(Modifier.height(14.dp))
        NyaField("Password (min 8 characters)", pass, { pass = it; auth.clearError() }, password = true)
        Spacer(Modifier.height(14.dp))
        NyaField("Confirm password", confirm, { confirm = it; auth.clearError() }, password = true)
        Spacer(Modifier.height(14.dp))
        NyaField("Date of birth (DD/MM/YYYY)", dob, { dob = it.filter(Char::isDigit).take(8); auth.clearError() },
            keyboardType = KeyboardType.Number, transformation = DateTransformation)
        Text("You must be 18 or older to use Nya.", color = c.sb, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(terms, { terms = it; auth.clearError() }, colors = CheckboxDefaults.colors(checkedColor = c.ac))
            Text("I agree to the Terms and Privacy Policy", color = c.tx, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        NyaButton("Create account", { auth.signUp(name, email, pass, confirm, dobText(dob), terms) { onVerify() } },
            loading = auth.busy)
        AuthMessage(auth.error, auth.info)
        SocialRow { auth.google(context, onGoogle) }
        Text("Already have an account? Sign in", color = c.ac, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).pressScale(onClick = onSignIn))
    }
}

@Composable
fun GreetingScreen(name: String, onDone: () -> Unit) {
    val c = LocalNya.current
    LaunchedEffect(Unit) { delay(2000); onDone() }
    Box(Modifier.fillMaxSize().nyaBg(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(90.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(c.b1, c.b2, c.sf))))
            Spacer(Modifier.height(24.dp))
            Text("Welcome", color = c.sb, fontSize = 16.sp, letterSpacing = 2.sp, modifier = Modifier.staggerIn(1))
            Text(name, color = c.tx, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 44.sp,
                modifier = Modifier.staggerIn(3))
        }
    }
}

@Composable
fun HomePlaceholder() {
    val c = LocalNya.current
    Box(Modifier.fillMaxSize().nyaBg(), contentAlignment = Alignment.Center) {
        Text("Home (coming next)", color = c.sb, fontSize = 16.sp)
    }
}