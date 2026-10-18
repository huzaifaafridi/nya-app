package com.nya.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.userDataStore by preferencesDataStore("user")

class AppContainer(context: Context) {
    val userStore: DataStore<Preferences> = context.applicationContext.userDataStore
    val prefs: PrefsRepository = DataStorePrefsRepository(userStore)
    val memories: MemoryRepository = FakeMemoryRepository()
    val chat: ChatRepository = FakeChatRepository()
    val auth: AuthRepository = FirebaseAuthRepository()
    val device: DeviceRepository = FakeDeviceRepository()
}
