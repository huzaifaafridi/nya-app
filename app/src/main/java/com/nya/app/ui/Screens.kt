package com.nya.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.R
import com.nya.app.ui.theme.LocalNya
import com.nya.app.ui.theme.NyaDark
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
        Modifier.size(54.dp).clip(CircleShape).background(c.sf)
            .border(1.dp, c.ln, CircleShape).clickable { },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun SocialRow() {
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
            "Google", Modifier.size(54.dp).clip(CircleShape).clickable { }
        )
        Image(
            painterResource(R.drawable.ic_facebook),
            "Facebook", Modifier.size(54.dp).clip(CircleShape).clickable { }
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
fun SignInScreen(onSignUp: () -> Unit, onForgot: () -> Unit, onHome: () -> Unit) {
    val c = LocalNya.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    Page {
        Spacer(Modifier.height(40.dp))
        Heading("Welcome back")
        Spacer(Modifier.height(28.dp))
        NyaField("Email", email, { email = it })
        Spacer(Modifier.height(16.dp))
        NyaField("Password", pass, { pass = it }, password = true)
        Text("Forgot password?", color = c.ac, fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp).clickable(onClick = onForgot))
        Spacer(Modifier.height(24.dp))
        NyaButton("Sign in", onHome)
        SocialRow()
        Text("New here? Create account", color = c.ac, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).clickable(onClick = onSignUp))
    }
}

@Composable
fun SignUpScreen(onSignIn: () -> Unit, onVerify: () -> Unit) {
    val c = LocalNya.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    Page {
        Spacer(Modifier.height(24.dp))
        Heading("Create account")
        Spacer(Modifier.height(24.dp))
        NyaField("Full name", name, { name = it })
        Spacer(Modifier.height(14.dp))
        NyaField("Email", email, { email = it })
        Spacer(Modifier.height(14.dp))
        NyaField("Password (min 8 characters)", pass, { pass = it }, password = true)
        Spacer(Modifier.height(14.dp))
        NyaField("Date of birth (DD/MM/YYYY)", dob, { dob = it })
        Text("You must be 18 or older to use Nya.", color = c.sb, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(terms, { terms = it }, colors = CheckboxDefaults.colors(checkedColor = c.ac))
            Text("I agree to the Terms and Privacy Policy", color = c.tx, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        NyaButton("Create account", onVerify)
        SocialRow()
        Text("Already have an account? Sign in", color = c.ac, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).clickable(onClick = onSignIn))
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
            Text("Welcome", color = c.sb, fontSize = 16.sp, letterSpacing = 2.sp)
            Text(name, color = c.tx, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 44.sp)
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