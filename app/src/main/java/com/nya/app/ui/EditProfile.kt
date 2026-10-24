package com.nya.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.EditProfileViewModel

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val c = LocalNya.current
    val vm = viewModel<EditProfileViewModel>(factory = EditProfileViewModel.Factory)
    val context = LocalContext.current
    DisposableEffect(Unit) {
        VoiceEngine.init(context)
        onDispose { VoiceEngine.stop() }
    }
    Column(Modifier.fillMaxSize().nyaBg().imePadding()) {
        XTop("Edit profile", onBack)
        val draft = vm.draft
        if (draft == null) LoadingState(Modifier.weight(1f))
        else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                NyaField("Name", draft.name, vm::setName)
                Spacer(Modifier.height(24.dp))
                Text("Companion", color = c.sb, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                CompanionList(draft.companion, vm::setCompanion)
                Spacer(Modifier.height(8.dp))
                VoicePicker(LocalPrefs.current.copy(
                    name = draft.name, companion = draft.companion, gender = draft.gender, voice = draft.voice
                ), vm::setVoice)
                Spacer(Modifier.height(16.dp))
            }
            Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                AuthMessage(vm.error, vm.info)
                Spacer(Modifier.height(12.dp))
                NyaButton("Save", vm::save, loading = vm.saving)
            }
        }
    }
}
