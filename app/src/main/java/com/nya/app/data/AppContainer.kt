package com.nya.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nya.app.data.ble.BleClient
import com.nya.app.data.ble.SimulatedBleClient
import com.nya.app.data.local.NyaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.userDataStore by preferencesDataStore("user")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val userStore: DataStore<Preferences> = appContext.userDataStore
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ble: BleClient = SimulatedBleClient(appScope)
    private val db = Room.databaseBuilder(appContext, NyaDatabase::class.java, "nya.db").build()

    val auth: AuthRepository = FirebaseAuthRepository()
    val settings: SettingsRepository = DataStoreSettingsRepository(userStore)
    val profile: ProfileRepository = DataStoreProfileRepository(userStore)
    val memories: MemoryRepository = RoomMemoryRepository(db.memoryDao(), db.seedDao(), auth.uid)
    val chat: ChatRepository = RoomChatRepository(
        db.chatDao(), auth.uid, profile, settings, GeminiReplyEngine(), appScope
    )
    val device: DeviceRepository = RoomDeviceRepository(db.deviceDao(), db.seedDao(), auth.uid, ble, appScope)
}
