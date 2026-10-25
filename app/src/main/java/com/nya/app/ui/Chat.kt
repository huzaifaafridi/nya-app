package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.data.ModeLabels
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.ChatViewModel
import com.nya.app.viewmodel.Loadable
import com.nya.app.viewmodel.valueOrNull
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val prefs = LocalPrefs.current
    val features = LocalConfig.current.features
    val vm = viewModel<ChatViewModel>(factory = ChatViewModel.Factory)
    val state by vm.messages.collectAsState()
    val msgs = state.valueOrNull().orEmpty()
    val draft by vm.draft.collectAsState()
    val error by vm.error.collectAsState()
    val quota by vm.quota.collectAsState()
    val canChat = features.aiChat && !quota.reached
    val typing = draft == ""
    var input by remember { mutableStateOf("") }
    var speaking by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val shown = remember { mutableSetOf<Long>() }
    // messages already there when the history first loaded are neither animated nor spoken again
    val loaded = state is Loadable.Ready
    val lastOld = remember(loaded) { msgs.maxOfOrNull { it.id } ?: 0L }

    fun send(t: String = input) {
        if (canChat && loaded && draft == null && t.isNotBlank()) {
            vm.send(t)
            input = ""
        }
    }

    LaunchedEffect(msgs.size) {
        speaking = false
        val last = msgs.lastOrNull()
        if (last != null && last.id > lastOld && !last.mine) {
            val voice = VoiceEngine.chosen(prefs.gender, prefs.voice)
            if (features.voiceReplies && !prefs.quiet && voice != null) VoiceEngine.speak(last.text, voice)
            else {
                speaking = true
                delay(1600)
                speaking = false
            }
        }
    }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        VoiceEngine.init(context)
        onDispose { VoiceEngine.stop() }
    }
    LaunchedEffect(msgs.size, typing) {
        list.animateScrollToItem(msgs.size)
    }
    LaunchedEffect(draft) {
        list.scrollToItem(msgs.size)
    }

    Column(
        Modifier.fillMaxSize().nyaBg()
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        ChatHeader(onBack = onBack, name = "Nya", sub = if (typing) "typing..." else "The ${prefs.companion.name} · ${ModeLabels.getOrElse(prefs.mode) { ModeLabels[0] }}",
            state = when {
                typing -> AvatarState.Thinking
                speaking || VoiceEngine.speaking -> AvatarState.Speaking
                else -> AvatarState.Idle
            },
            mouth = if (VoiceEngine.speaking) VoiceEngine.mouth else null)

        if (!loaded) Box(Modifier.weight(1f).fillMaxWidth()) {
            LoadableContent(state, vm::retry) {}
        } else LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            items(msgs, key = { it.id }) { m ->
                // animate each message once, when it first arrives (not when scrolled back into view)
                val fresh = remember { shown.add(m.id) && m.id > lastOld }
                Box(Modifier.staggerIn(0, animate = fresh)) {
                    if (m.mine) UserBubble(m.text) else AiBubble(m.text)
                }
            }
            draft?.let { text ->
                item(key = "draft") {
                    if (typing) Box(Modifier.staggerIn(0)) { TypingBubble() } else AiBubble(text)
                }
            }
        }

        error?.let {
            Text(it, color = NyaDanger, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite })
        }

        if (!features.aiChat) Text("Chat is paused right now. Please try again later.", color = c.sb, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite })
        else if (quota.reached) Column(
            Modifier.padding(horizontal = 20.dp, vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text("You've used today's ${quota.limit} messages. Upgrade your plan or come back tomorrow.", color = c.sb, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            XGhost("See plans", { SubNav.open("Subscription") })
        } else {
            // quick replies
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 17.dp, vertical = 6.dp)) {
                listOf("Summarize my day", "Best shot today").forEach {
                    NyaChip(it, false, { send(it) })
                }
            }
            if (!quota.unlimited && quota.remaining <= 5) Text(
                "${quota.remaining} ${if (quota.remaining == 1) "message" else "messages"} left today", color = c.sb, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // input pill
        val pill = RoundedCornerShape(29.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                .heightIn(min = 58.dp).alpha(if (canChat) 1f else .5f).clip(pill).background(c.sf).border(1.dp, c.ln, pill)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (input.isEmpty()) Text("Ask anything…", color = c.sb, fontSize = 15.sp, modifier = Modifier.clearAndSetSemantics {})
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(color = c.tx, fontSize = 15.sp),
                    cursorBrush = SolidColor(c.ac),
                    singleLine = true,
                    enabled = canChat,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Message" }
                )
            }
            IconBtn(NyaIcons.Mic, "Microphone")
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.pressScale(description = "Send") { send() }.size(42.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(c.b1, c.b2))),
                contentAlignment = Alignment.Center
            ) { NyaIcon(NyaIcons.Up, tint = c.on, iconSize = 20.dp, strokeW = 2f) }
        }
    }
}
