package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

val faqs = listOf(
    "How do I pair my headband?" to "Open Device, tap Pairing trouble? and follow the steps. Keep the headband within 1 metre and make sure Bluetooth is on.",
    "How do I charge it?" to "Place the headband on a Qi wireless charger. The red light shows charging, green means ready.",
    "Where are my memories stored?" to "Memories sync from your headband to your account, so you can see them on any device you sign in on.",
    "Can I use Nya without the headband?" to "You can chat with Nya, but capturing new memories needs the headband connected.",
    "How do I delete a memory?" to "Open a memory and tap Delete. It's removed from your account."
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    var open by remember { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Help & support", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            faqs.forEachIndexed { i, f ->
                XCard(Modifier.padding(bottom = 12.dp).semantics { stateDescription = if (open == i) "Expanded" else "Collapsed" }, onClick = { open = if (open == i) -1 else i }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.first, color = c.tx, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(if (open == i) "–" else "+", color = c.ac, fontSize = 20.sp, modifier = Modifier.clearAndSetSemantics {})
                    }
                    if (open == i) {
                        Text(f.second, color = c.sb, fontSize = 14.sp, lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            XBtn("Contact support", { })
        }
    }
}

