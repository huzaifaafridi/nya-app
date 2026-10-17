package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

object MemNav { var open: (Int) -> Unit = {} }

data class Memory(
    val title: String, val place: String, val time: String,
    val a: Color, val b: Color, val note: String
)

val memories = listOf(
    Memory("Sunset at the pier", "Karachi", "Today, 6:42 PM",
        Color(0xFFE8A86E), Color(0xFF7A3B1D), "Golden hour by the water. You said it felt like the day finally slowed down."),
    Memory("Coffee with Ali", "Clifton", "Yesterday, 11:10 AM",
        Color(0xFF8C6A4F), Color(0xFF2E1F16), "Talked about the app launch. Ali suggested a softer onboarding."),
    Memory("Morning walk", "Park", "Mon, 7:05 AM",
        Color(0xFF6E9E7A), Color(0xFF1F3A2A), "30 minutes, no phone. You noticed the birds near the east gate."),
    Memory("Late night idea", "Home", "Sun, 11:48 PM",
        Color(0xFF6C7FD8), Color(0xFF1B2250), "A voice note about making memories searchable by feeling."),
    Memory("Family dinner", "Home", "Sat, 8:30 PM",
        Color(0xFFD87C6C), Color(0xFF4A1F1A), "Everyone together. Biryani, lots of laughing."),
    Memory("Beach day", "Sea View", "Fri, 4:15 PM",
        Color(0xFF5FB7C9), Color(0xFF16414D), "Windy, cold water, and the best chai afterwards.")
)

@Composable
fun OldMemoriesScreen() {
    val c = LocalNya.current
    Column(Modifier.fillMaxSize().nyaBg().statusBarsPadding().padding(horizontal = 20.dp)) {
        Text("Memories", color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 24.dp))
        Text("${memories.size} moments saved", color = c.sb, fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            itemsIndexed(memories) { i, m -> MemoryCard(m) { MemNav.open(i) } }
        }
    }
}

@Composable
fun MemoryCard(m: Memory, onClick: () -> Unit) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().height(190.dp).pressScale { onClick() }.clip(shape)
            .background(Brush.verticalGradient(listOf(m.a, m.b)))
            .border(1.dp, c.ln, shape)
    ) {
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(m.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(m.time, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun MemoryDetailScreen(index: Int, onBack: () -> Unit) {
    val c = LocalNya.current
    val m = memories[index]
    Column(Modifier.fillMaxSize().nyaBg().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(340.dp)
            .background(Brush.verticalGradient(listOf(m.a, m.b)))) {
            Text("‹", color = Color.White, fontSize = 34.sp,
                modifier = Modifier.statusBarsPadding().padding(20.dp).pressScale { onBack() })
        }
        Column(Modifier.padding(24.dp)) {
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
        MemoriesGrid(onOpen = { MemNav.open(0) }, start = 4)
    }
}