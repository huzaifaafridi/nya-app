package com.nya.app.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

fun <T> Loadable<T>.valueOrNull(): T? = (this as? Loadable.Ready)?.value

fun <T, R> Loadable<T>.map(transform: (T) -> R): Loadable<R> = when (this) {
    Loadable.Loading -> Loadable.Loading
    is Loadable.Ready -> Loadable.Ready(transform(value))
    is Loadable.Failed -> this
}

/** Shares [source] as a [Loadable] that can be re-collected with [retry]. */
@OptIn(ExperimentalCoroutinesApi::class)
class Retryable<T>(scope: CoroutineScope, source: Flow<T>) {
    private val attempt = MutableStateFlow(0)

    val state: StateFlow<Loadable<T>> = attempt
        .flatMapLatest {
            source.map<T, Loadable<T>> { Loadable.Ready(it) }
                .onStart { emit(Loadable.Loading) }
                .catch { emit(Loadable.Failed("Couldn't load your data.")) }
        }
        .stateIn(scope, SharingStarted.Eagerly, Loadable.Loading)

    fun retry() = attempt.update { it + 1 }
}
