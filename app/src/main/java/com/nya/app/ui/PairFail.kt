package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

@Composable
fun PairFailScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val tips = listOf(
        "Turn Bluetooth on in your phone settings",
        "Make sure the headband is charged",
        "Keep it within 1 metre of your phone",
        "Hold the button for 5 seconds to reset pairing"
    )
    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Pairing", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.size(96.dp).border(3.dp, c.ac, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("!", color = c.ac, fontSize = 40.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(22.dp))
            Text("Couldn't connect", color = c.tx, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text("Let's try a few things.", color = c.sb, fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
            XCard {
                tips.forEachIndexed { i, tip ->
                    Row(Modifier.padding(vertical = 8.dp)) {
                        Text("${i + 1}", color = c.ac, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(26.dp))
                        Text(tip, color = c.tx, fontSize = 15.sp)
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            XBtn("Try again", { onBack() })
            Spacer(Modifier.height(12.dp))
            XGhost("Get help", { SubNav.open("Help") })
        }
    }
}