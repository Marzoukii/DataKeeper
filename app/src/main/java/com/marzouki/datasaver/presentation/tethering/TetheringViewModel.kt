package com.marzouki.datasaver.presentation.tethering

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.repository.Clock
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val observeHotspotActive: ObserveHotspotActiveUseCase,
    private val observeAlert: ObserveTetheringAlertUseCase,
    private val observeDataOperator: ObserveDataOperatorUseCase,
    private val observeDataNetworkGeneration: ObserveDataNetworkGenerationUseCase,
    private val hasPhoneStatePermission: HasPhoneStatePermissionUseCase,
    private val getTetheringUsage: GetTetheringUsageUseCase,
    private val setAlert: SetTetheringAlertUseCase,
    private val clock: Clock,
) : MviViewModel<TetheringState, TetheringIntent, TetheringEffect>(TetheringState()) {

    override fun observe(): List<Flow<*>> = listOfNotNull(
        observeHotspotActive()
            // Partage éteint : les relevés de la session passée ne comptent plus.
            .onEach { active -> setState { copy(active = active, samples = if (active) samples else emptyList()) } }
            .flatMapLatest { active -> if (active) every(SAMPLE_MILLIS) else flowOf(Unit) }
            .onEach { sample() },
        observeAlert().onEach { alert -> setState { copy(alert = alert) } },
        observeDataOperator().onEach { operator -> setState { copy(operatorName = operator?.name) } },
        // La 4G ou 5G n'est lisible qu'avec la permission « Téléphone », demandée dans Ma consommation ;
        // relue à chaque retour sur l'écran.
        observeDataNetworkGeneration()
            .onEach { generation -> setState { copy(networkGeneration = generation) } }
            .takeIf { hasPhoneStatePermission() },
    )

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
            val now = clock.nowMillis()
            val usage = getTetheringUsage(active, now)
            setState { withSample(usage, now) }
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
    }
}
