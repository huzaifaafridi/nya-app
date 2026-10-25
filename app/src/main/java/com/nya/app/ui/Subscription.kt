package com.nya.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<SubscriptionViewModel>(factory = SubscriptionViewModel.Factory)
    val current by vm.plan.collectAsState()
    val limits = LocalConfig.current.plans
    Column(Modifier.fillMaxSize().nyaBg()) {
        XTop("Subscription", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            vm.products.forEach { p ->
                val shape = RoundedCornerShape(20.dp)
                val on = p.plan == current
                val daily = limits.getValue(p.plan).dailyMessages
                Column(
                    Modifier.padding(bottom = 14.dp).fillMaxWidth().clip(shape)
                        .background(c.sf).border(if (on) 2.dp else 1.dp, if (on) c.ac else c.ln, shape)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.plan.name, color = c.tx, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        Text(p.price, color = c.ac, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    (listOf(if (daily == 0) "Unlimited AI messages" else "$daily AI messages a day") + p.perks).forEach {
                        Text("•  $it", color = c.sb, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    if (on) Text("Current plan", color = c.ac, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    else NyaButton("Choose ${p.plan.name}", { vm.choose(p.plan) }, loading = vm.choosing == p.plan)
                }
            }
            AuthMessage(vm.error, vm.info)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale { vm.restore() },
                contentAlignment = Alignment.Center
            ) { Text("Restore purchases", color = c.ac, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Text("Test mode: no real payment is taken.", color = c.sb, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
    }
}
