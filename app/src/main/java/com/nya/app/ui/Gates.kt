package com.nya.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.nya.app.ui.theme.LocalNya

/** Opens [url] in whatever app handles it; false when nothing does. */
fun Context.openUrl(url: String) = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}

@Composable
fun UpdateRequiredScreen() {
    val context = LocalContext.current
    GateScreen(NyaIcons.Download, "Update required", "This version of Nya is too old. Please update to keep going.") {
        XBtn("Update", {
            val id = context.packageName
            context.openUrl("market://details?id=$id") || context.openUrl("https://play.google.com/store/apps/details?id=$id")
        })
    }
}

@Composable
fun MaintenanceScreen(message: String) = GateScreen(NyaIcons.Gear, "Maintenance", message)

@Composable
private fun GateScreen(icon: String, title: String, body: String, action: (@Composable () -> Unit)? = null) {
    val c = LocalNya.current
    Column(
        Modifier.fillMaxSize().nyaBg().systemBarsPadding().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NyaOrb(96.dp) { NyaIcon(icon, tint = c.ac, iconSize = 48.dp, strokeW = 1.2f) }
        Spacer(Modifier.height(28.dp))
        Text(title, color = c.tx, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 34.sp,
            textAlign = TextAlign.Center, modifier = Modifier.asHeading())
        Spacer(Modifier.height(12.dp))
        Text(body, color = c.sb, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(32.dp))
            action()
        }
    }
}
