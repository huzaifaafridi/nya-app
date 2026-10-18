package com.nya.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nya.app.data.local.NyaDatabase

private val Context.userDataStore by preferencesDataStore("user")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val userStore: DataStore<Preferences> = appContext.userDataStore
    private val db = Room.databaseBuilder(appContext, NyaDatabase::class.java, "nya.db").build()

    val auth: AuthRepository = FirebaseAuthRepository()
    val settings: SettingsRepository = DataStoreSettingsRepository(userStore)
    val profile: ProfileRepository = DataStoreProfileRepository(userStore)
    val memories: MemoryRepository = RoomMemoryRepository(db.memoryDao(), db.seedDao(), auth.uid)
    val chat: ChatRepository = RoomChatRepository(db.chatDao(), auth.uid, FakeReplyEngine())
    val device: DeviceRepository = RoomDeviceRepository(db.deviceDao(), db.seedDao(), auth.uid)
}
