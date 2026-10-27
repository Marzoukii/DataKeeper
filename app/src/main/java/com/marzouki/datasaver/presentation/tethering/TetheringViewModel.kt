package com.marzouki.datasaver.presentation.tethering

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.model.TetheringSample
import com.marzouki.datasaver.domain.model.bytesPerSecond
import com.marzouki.datasaver.domain.usecase.GetTetheringUsageUseCase
import com.marzouki.datasaver.domain.usecase.HasPhoneStatePermissionUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataNetworkGenerationUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveHotspotActiveUseCase
import com.marzouki.datasaver.domain.usecase.ObserveTetheringAlertUseCase
import com.marzouki.datasaver.domain.usecase.SetTetheringAlertUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tant que le partage est allumé, le compteur est relu toutes les 2 s : le total et le rythme
 * horaire (5 dernières minutes) bougent en direct.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TetheringViewModel @Inject constructor(
    observeHotspotActive: ObserveHotspotActiveUseCase,
    observeAlert: ObserveTetheringAlertUseCase,
    observeDataOperator: ObserveDataOperatorUseCase,
    observeDataNetworkGeneration: ObserveDataNetworkGenerationUseCase,
    hasPhoneStatePermission: HasPhoneStatePermissionUseCase,
    private val getTetheringUsage: GetTetheringUsageUseCase,
    private val setAlert: SetTetheringAlertUseCase,
) : MviViewModel<TetheringState, TetheringIntent, TetheringEffect>(TetheringState()) {

    /** Relevés des dernières minutes, du plus ancien au plus récent. */
    private val samples = ArrayDeque<TetheringSample>()

    init {
        observeHotspotActive()
            .onEach { active ->
                setState { copy(active = active) }
                if (!active) samples.clear()
            }
            .flatMapLatest { active -> if (active) every(SAMPLE_MILLIS) else flowOf(Unit) }
            .onEach { sample() }
            .launchIn(viewModelScope)
        observeAlert()
            .onEach { alert -> setState { copy(alert = alert) } }
            .launchIn(viewModelScope)
        observeDataOperator()
            .onEach { operator -> setState { copy(operatorName = operator?.name) } }
            .launchIn(viewModelScope)
        // La 4G ou 5G n'est lisible qu'avec la permission « Téléphone », demandée dans Ma consommation.
        if (hasPhoneStatePermission()) {
            observeDataNetworkGeneration()
                .onEach { generation -> setState { copy(networkGeneration = generation) } }
                .launchIn(viewModelScope)
        }
    }

    override fun onIntent(intent: TetheringIntent) {
        when (intent) {
            TetheringIntent.DevicesClicked -> sendEffect(TetheringEffect.OpenHotspotSettings)
            TetheringIntent.Refresh -> sample()
            is TetheringIntent.AlertToggled -> {
                viewModelScope.launch { setAlert(currentState.alert.copy(enabled = intent.enabled)) }
                if (intent.enabled) sendEffect(TetheringEffect.RequestNotificationPermission)
            }
            // Choisir un seuil, c'est vouloir être prévenu : l'alerte s'active avec.
            is TetheringIntent.ThresholdSelected -> {
                val wasEnabled = currentState.alert.enabled
                viewModelScope.launch { setAlert(currentState.alert.copy(enabled = true, thresholdBytes = intent.thresholdBytes)) }
                if (!wasEnabled) sendEffect(TetheringEffect.RequestNotificationPermission)
            }
        }
    }

    private fun sample() {
        val active = currentState.active ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val usage = getTetheringUsage(active, now)
            if (usage == null) {
                samples.clear()
                setState { copy(usage = null, recentBytesPerHour = null) }
                return@launch
            }
            // Nouvelle session ou compteur reparti de zéro : les anciens relevés ne comptent plus.
            if (samples.lastOrNull()?.let { it.usedBytes > usage.usedBytes } == true) samples.clear()
            samples.addLast(TetheringSample(now, usage.usedBytes))
            while (samples.first().atMillis < now - RECENT_WINDOW_MILLIS) samples.removeFirst()

            val recent = samples.toList()
            val recentSpan = recent.last().atMillis - recent.first().atMillis
            setState {
                copy(
                    usage = usage,
                    recentBytesPerHour = recent.bytesPerSecond()
                        ?.takeIf { recentSpan >= MIN_RECENT_SPAN_MILLIS }
                        ?.let { it * 3_600 },
                )
            }
        }
    }

    private fun every(millis: Long) = flow {
        while (true) {
            emit(Unit)
            delay(millis)
        }
    }

    private companion object {
        const val SAMPLE_MILLIS = 2_000L
        const val RECENT_WINDOW_MILLIS = 5 * 60_000L
        const val MIN_RECENT_SPAN_MILLIS = 60_000L
    }
}
