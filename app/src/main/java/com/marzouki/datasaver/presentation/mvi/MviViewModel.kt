package com.marzouki.datasaver.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base MVI : l'UI envoie des intentions ([onIntent]), le ViewModel produit un état
 * immuable unique ([state]) et des effets ponctuels ([effects]) — navigation, snackbar, permissions.
 */
abstract class MviViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /**
     * Les flux de [observe] ne tournent que tant que l'écran collecte l'état, et [STOP_TIMEOUT_MILLIS]
     * de plus pour passer une rotation : rien ne reste branché (SIM, partage, Wi-Fi) en arrière-plan.
     * Paresseux : [observe] lit des propriétés de la sous-classe, prêtes seulement après sa construction.
     */
    val state: StateFlow<State> by lazy {
        channelFlow {
            observe().forEach { flow -> launch { flow.collect() } }
            _state.collect { send(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), _state.value)
    }

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    protected val currentState: State get() = _state.value

    abstract fun onIntent(intent: Intent)

    /** Flux qui alimentent l'état ; relancés à chaque retour sur l'écran, arrêtés quand il n'est plus visible. */
    protected open fun observe(): List<Flow<*>> = emptyList()

    /** Une partie de l'état, à chaque changement : pour brancher un flux de [observe] sur l'état lui-même. */
    protected fun <T> select(selector: State.() -> T): Flow<T> = _state.map(selector).distinctUntilChanged()

    protected fun setState(reducer: State.() -> State) = _state.update(reducer)

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
