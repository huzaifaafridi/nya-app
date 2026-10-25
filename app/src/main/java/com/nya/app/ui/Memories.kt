package com.nya.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.data.Memory
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.MemoriesViewModel
import com.nya.app.viewmodel.MemoryFilter
import com.nya.app.viewmodel.valueOrNull

object MemNav { var open: (Long) -> Unit = {} }

@Composable
fun MemoryDetailScreen(id: Long, onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<MemoriesViewModel>(factory = MemoriesViewModel.Factory)
    val state = vm.memories.collectAsState().value
    val m = state.valueOrNull().orEmpty().firstOrNull { it.id == id }
    val context = LocalContext.current
    fun share(memory: Memory) {
        try {
            context.startActivity(Intent.createChooser(shareIntent(memory), "Share memory"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can share this", Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize().nyaBg().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(340.dp).then(if (m != null) Modifier.background(m.scene.brush()) else Modifier)) {
            Text("‹", color = Color.White, fontSize = 34.sp,
                modifier = Modifier.statusBarsPadding().pressScale(description = "Back") { onBack() }.padding(20.dp))
        }
        if (m == null) LoadableContent(state, vm::retry) {
            EmptyState("Memory not found", "It may have been deleted.", Modifier.padding(24.dp))
        } else Column(Modifier.padding(24.dp)) {
            Text(m.title, color = c.tx, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("${memoryTime(m.createdAt)}  ·  ${m.place}", color = c.sb, fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp))
            Text("What Nya remembers", color = c.ac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 26.dp, bottom = 8.dp))
            Text(m.note, color = c.tx, fontSize = 16.sp, lineHeight = 24.sp)
            Row(Modifier.padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (LocalConfig.current.features.share) NyaButton("Share", onClick = { share(m) }, modifier = Modifier.weight(1f))
                NyaOutlineButton("Delete", onClick = { vm.delete(id); onBack() }, modifier = Modifier.weight(1f))
            }
        }
    }
}
@Composable
fun MemoriesScreen() {
    val c = LocalNya.current
    val vm = viewModel<MemoriesViewModel>(factory = MemoriesViewModel.Factory)
    val hasAny = vm.memories.collectAsState().value.valueOrNull().orEmpty().isNotEmpty()
    val visible = vm.visible.collectAsState().value
    val query = vm.query.collectAsState().value
    val filter = vm.filter.collectAsState().value
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
    ) {
        Text("Memories", color = c.tx, fontSize = 34.sp, fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic, modifier = Modifier.fillMaxWidth().staggerIn(0).asHeading())

        Spacer(Modifier.height(14.dp))
        val bar = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).staggerIn(1).clip(bar).background(c.sf).border(1.dp, c.ln, bar)
                .padding(start = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NyaIcon(NyaIcons.Search, tint = c.sb, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Try \"sunset by the sea\"", color = c.sb, fontSize = 15.sp,
                    modifier = Modifier.clearAndSetSemantics {})
                BasicTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    textStyle = TextStyle(color = c.tx, fontSize = 15.sp),
                    cursorBrush = SolidColor(c.ac),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search memories" }
                )
            }
            if (query.isNotEmpty()) Box(
                Modifier.pressScale(description = "Clear search") { vm.setQuery("") }.minimumInteractiveComponentSize(),
                contentAlignment = Alignment.Center
            ) { NyaIcon(NyaIcons.Clear, tint = c.sb, iconSize = 18.dp) }
            else Spacer(Modifier.width(18.dp))
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.staggerIn(2).horizontalScroll(rememberScrollState())) {
            MemoryFilter.entries.forEach { NyaChip(it.label, filter == it, { vm.setFilter(it) }) }
        }

        Spacer(Modifier.height(14.dp))
        LoadableContent(visible, vm::retry) { sections ->
            if (sections.isEmpty()) {
                if (hasAny) EmptyState("No matches", "Try a different word or filter.", Modifier.padding(vertical = 40.dp))
                else EmptyState("No memories yet", "Capture a moment and it will show up here.", Modifier.padding(vertical = 40.dp))
            }
            sections.forEachIndexed { i, section ->
                if (i > 0) Spacer(Modifier.height(18.dp))
                Text(section.label, color = c.sb, fontSize = 11.sp, letterSpacing = 2.sp,
                    modifier = Modifier.staggerIn(3).asHeading())
                Spacer(Modifier.height(10.dp))
                // the grid lays out up to five thumbs at a time
                section.items.chunked(5).forEachIndexed { n, chunk ->
                    if (n > 0) Spacer(Modifier.height(10.dp))
                    MemoriesGrid(chunk, { MemNav.open(it) }, start = 4)
                }
            }
        }
    }
}

private fun shareIntent(m: Memory): Intent {
    val text = "${m.title}\n${memoryTime(m.createdAt)} · ${m.place}\n\n${m.note}"
    val send = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, text)
    if (m.mediaUri == null) return send.setType("text/plain")
    return send.setType(if (m.clipSeconds != null) "video/*" else "image/*")
        .putExtra(Intent.EXTRA_STREAM, m.mediaUri.toUri())
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
