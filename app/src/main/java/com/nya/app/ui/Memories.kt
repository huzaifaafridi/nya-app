package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.MemoriesViewModel

object MemNav { var open: (String) -> Unit = {} }

@Composable
fun MemoryDetailScreen(id: String, onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<MemoriesViewModel>(factory = MemoriesViewModel.Factory)
    val m = vm.memories.collectAsState().value.firstOrNull { it.id == id }
    Column(Modifier.fillMaxSize().nyaBg().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(340.dp).then(if (m != null) Modifier.background(m.scene.brush()) else Modifier)) {
            Text("‹", color = Color.White, fontSize = 34.sp,
                modifier = Modifier.statusBarsPadding().padding(20.dp).pressScale { onBack() })
        }
        if (m != null) Column(Modifier.padding(24.dp)) {
            Text(m.title, color = c.tx, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("${m.time}  ·  ${m.place}", color = c.sb, fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp))
            Text("What Nya remembers", color = c.ac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 26.dp, bottom = 8.dp))
            Text(m.note, color = c.tx, fontSize = 16.sp, lineHeight = 24.sp)
            Row(Modifier.padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NyaButton("Share", onClick = { }, modifier = Modifier.weight(1f))
                NyaOutlineButton("Delete", onClick = { }, modifier = Modifier.weight(1f))
            }
        }
    }
}
@Composable
fun MemoriesScreen() {
    val c = LocalNya.current
    val vm = viewModel<MemoriesViewModel>(factory = MemoriesViewModel.Factory)
    val items by vm.memories.collectAsState()
    var f by remember { mutableStateOf(0) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
    ) {
        Row(Modifier.fillMaxWidth().staggerIn(0), verticalAlignment = Alignment.CenterVertically) {
            Text("Memories", color = c.tx, fontSize = 34.sp, fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).border(1.dp, c.ln, CircleShape),
                contentAlignment = Alignment.Center) { NyaIcon(NyaIcons.Filter, tint = c.tx, iconSize = 20.dp) }
        }

        Spacer(Modifier.height(14.dp))
        val bar = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().height(48.dp).staggerIn(1).clip(bar).background(c.sf).border(1.dp, c.ln, bar)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NyaIcon(NyaIcons.Search, tint = c.sb, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            Text("Try \"sunset by the sea\"", color = c.sb, fontSize = 15.sp)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.staggerIn(2).horizontalScroll(rememberScrollState())) {
            listOf("All", "Photos", "Clips", "Today", "Travel").forEachIndexed { i, t ->
                NyaChip(t, f == i, { f = i })
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("YESTERDAY", color = c.sb, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.staggerIn(3))
        Spacer(Modifier.height(10.dp))
        MemoriesGrid(items, { MemNav.open(it) }, start = 4)
    }
}