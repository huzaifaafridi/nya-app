package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

data class Plan(val name: String, val price: String, val perks: List<String>)

val plans = listOf(
    Plan("Free", "$0", listOf("Basic memories", "Chat with Nya", "1 device")),
    Plan("Plus", "$4.99 / mo", listOf("Unlimited memories", "Voice replies", "Priority sync")),
    Plan("Pro", "$9.99 / mo", listOf("Everything in Plus", "Advanced insights", "Early features"))
)

@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    var sel by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Subscription", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            plans.forEachIndexed { i, p ->
                val shape = RoundedCornerShape(20.dp)
                val on = sel == i
                Column(
                    Modifier.padding(bottom = 14.dp).fillMaxWidth().clip(shape)
                        .background(c.sf).border(if (on) 2.dp else 1.dp, if (on) c.ac else c.ln, shape)
                        .clickable { sel = i }.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, color = c.tx, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        Text(p.price, color = c.ac, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    p.perks.forEach {
                        Text("•  $it", color = c.sb, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
            Text("Prices are placeholders until confirmed.", color = c.sb, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        Column(Modifier.padding(20.dp)) {
            XBtn(if (sel == 0) "Stay on Free" else "Choose ${plans[sel].name}", { onBack() })
        }
    }
}

