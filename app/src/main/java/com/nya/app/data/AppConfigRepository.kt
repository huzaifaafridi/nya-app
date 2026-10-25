package com.nya.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

private const val Tag = "NyaConfig"

interface AppConfigRepository {
    /** Starts as the built-in defaults and follows the remote document live. */
    val config: StateFlow<AppConfig>
}

/**
 * Listens to the `appConfig/main` document (readable without sign-in). Firestore's default on-disk
 * persistence serves the last known document on a cold start without network. A missing document
 * or an error keeps the current value.
 */
class FirestoreAppConfigRepository(
    scope: CoroutineScope,
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AppConfigRepository {
    override val config: StateFlow<AppConfig> = callbackFlow {
        val registration = db.collection("appConfig").document("main").addSnapshotListener { snapshot, error ->
            val data = snapshot?.data
            when {
                error != null -> Log.w(Tag, "Config listener failed: ${error.message}")
                data == null -> Log.w(Tag, "No appConfig/main document, keeping current config")
                else -> trySend(parseAppConfig(data))
            }
        }
        awaitClose { registration.remove() }
    }.stateIn(scope, SharingStarted.Eagerly, AppConfig())
}
