package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import kotlinx.coroutines.delay

data class Msg(val text: String, val mine: Boolean)

val fakeReplies = listOf(
    "Got it. Want me to save that as a memory?",
    "That sounds like a good moment. I can pull up similar ones from this week.",
    "I'm here. Tell me more.",
    "Noted. I'll remind you tomorrow morning."
)

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val msgs = remember { mutableStateListOf(Msg("Hey, I'm here. What's on your mind?", false)) }
    var input by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val shown = remember { mutableSetOf<Int>() }

    fun send(t: String = input) {
        if (t.isNotBlank()) {
            msgs.add(Msg(t.trim(), true))
            input = ""
        }
    }

    // fake Nya reply (replace with Gemini call later)
    LaunchedEffect(msgs.size) {
        speaking = false
        if (msgs.isNotEmpty() && msgs.last().mine) {
            typing = true
            delay(1200)
            typing = false
            msgs.add(Msg(fakeReplies.random(), false))
        } else if (msgs.size > 1) {
            speaking = true
            delay(1600)
            speaking = false
        }
    }
    LaunchedEffect(msgs.size, typing) {
        list.animateScrollToItem(msgs.size)
    }

    Column(
        Modifier.fillMaxSize().nyaBg()
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        ChatHeader(onBack = onBack, name = "Nya", sub = if (typing) "typing..." else "The Muse · Focus",
            state = when {
                typing -> AvatarState.Thinking
                speaking -> AvatarState.Speaking
                else -> AvatarState.Idle
            })

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            itemsIndexed(msgs) { i, m ->
                // animate each message once, when it first arrives (not when scrolled back into view)
                val fresh = remember { shown.add(i) }
                Box(Modifier.staggerIn(0, animate = fresh)) {
                    if (m.mine) UserBubble(m.text) else AiBubble(m.text)
                }
            }
            if (typing) item { Box(Modifier.staggerIn(0)) { TypingBubble() } }
        }

        // quick replies
        Row(Modifier.padding(horizontal = 17.dp, vertical = 6.dp)) {
            listOf("Summarize my day", "Best shot today").forEach {
                NyaChip(it, false, { send(it) })
            }
        }

        // input pill
        val pill = RoundedCornerShape(29.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                .height(58.dp).clip(pill).background(c.sf).border(1.dp, c.ln, pill)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (input.isEmpty()) Text("Ask anything…", color = c.sb, fontSize = 15.sp)
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(color = c.tx, fontSize = 15.sp),
                    cursorBrush = SolidColor(c.ac),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            IconBtn(NyaIcons.Mic)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(42.dp).pressScale { send() }.clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(c.b1, c.b2))),
                contentAlignment = Alignment.Center
            ) { NyaIcon(NyaIcons.Up, tint = c.on, iconSize = 20.dp, strokeW = 2f) }
        }
    }
}
