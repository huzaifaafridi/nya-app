package com.nya.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nya.app.data.ble.BleClient
import com.nya.app.data.ble.SimulatedBleClient
import com.nya.app.data.local.Migration1To2
import com.nya.app.data.local.Migration2To3
import com.nya.app.data.local.Migration3To4
import com.nya.app.data.local.NyaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.userDataStore by preferencesDataStore("user")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val userStore: DataStore<Preferences> = appContext.userDataStore
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val appConfig: AppConfigRepository = FirestoreAppConfigRepository(appScope)
    private val ble: BleClient = SimulatedBleClient(appScope) { appConfig.config.value.ble.namePrefix }
    private val db = Room.databaseBuilder(appContext, NyaDatabase::class.java, "nya.db")
        .addMigrations(Migration1To2, Migration2To3, Migration3To4)
        .build()

    private val cloud: CloudSync = FirestoreCloudSync()

    val auth: AuthRepository = FirebaseAuthRepository()
    val settings: SettingsRepository = DataStoreSettingsRepository(userStore)
    val billing: BillingRepository = FakeBillingRepository(userStore, auth.uid)
    val usage: UsageRepository = DataStoreUsageRepository(userStore, auth.uid)
    val profile: ProfileRepository = RoomProfileRepository(db.profileDao(), cloud, auth.uid, userStore)
    val memories: MemoryRepository = RoomMemoryRepository(db.memoryDao(), cloud, auth.uid)
    val chat: ChatRepository = RoomChatRepository(
        db.chatDao(), cloud, auth.uid, profile, settings, GeminiReplyEngine(appConfig), appScope
    )
    val device: DeviceRepository = RoomDeviceRepository(db.deviceDao(), cloud, auth.uid, ble, ConfigFirmwareCatalog(appConfig), appScope)

    init {
        SyncManager(
            auth.uid, cloud, LocalMediaStore(), db.memoryDao(), db.chatDao(), db.deviceDao(), db.profileDao(), db.seedDao(), appScope
        ).start()
    }
}
