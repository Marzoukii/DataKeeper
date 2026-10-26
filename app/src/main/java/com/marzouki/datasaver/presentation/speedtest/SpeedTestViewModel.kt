package com.marzouki.datasaver.presentation.speedtest

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.model.SpeedTestEvent
import com.marzouki.datasaver.domain.usecase.ObserveDataNetworkGenerationUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveSpeedTestHistoryUseCase
import com.marzouki.datasaver.domain.usecase.ObserveWifiConnectionUseCase
import com.marzouki.datasaver.domain.usecase.RunSpeedTestUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    observeWifiConnection: ObserveWifiConnectionUseCase,
    observeDataOperator: ObserveDataOperatorUseCase,
    observeDataNetworkGeneration: ObserveDataNetworkGenerationUseCase,
    observeHistory: ObserveSpeedTestHistoryUseCase,
    private val runSpeedTest: RunSpeedTestUseCase,
) : MviViewModel<SpeedTestState, SpeedTestIntent, SpeedTestEffect>(SpeedTestState()) {

    private var testJob: Job? = null

    init {
        observeWifiConnection()
            .onEach { onWifi -> setState { copy(isOnWifi = onWifi) } }
            .launchIn(viewModelScope)
        observeDataOperator()
            .onEach { operator -> setState { copy(operatorName = operator?.name) } }
            .launchIn(viewModelScope)
        observeDataNetworkGeneration()
            .onEach { generation -> setState { copy(generation = generation) } }
            .launchIn(viewModelScope)
        observeHistory()
            .onEach { history -> setState { copy(history = history) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SpeedTestIntent) {
        when (intent) {
            SpeedTestIntent.StartClicked ->
                if (currentState.isOnWifi) start() else setState { copy(showMobileWarning = true) }
            SpeedTestIntent.MobileTestConfirmed -> {
                setState { copy(showMobileWarning = false) }
                start()
            }
            SpeedTestIntent.MobileWarningDismissed -> setState { copy(showMobileWarning = false) }
            SpeedTestIntent.CancelClicked -> testJob?.cancel()
        }
    }

    private fun start() {
        if (testJob?.isActive == true) return
        val state = currentState
        setState { copy(progress = null, result = null) }
        testJob = viewModelScope.launch {
            try {
                runSpeedTest(state.isOnWifi, state.operatorName, state.generation).collect { event ->
                    when (event) {
                        is SpeedTestEvent.Progress -> setState { copy(progress = event.progress) }
                        is SpeedTestEvent.Finished -> setState { copy(progress = null, result = event.result) }
                    }
                }
            } catch (_: IOException) {
                sendEffect(SpeedTestEffect.Failed)
            } finally {
                // Annulé ou en échec : retour à l'accueil du test.
                setState { copy(progress = null) }
            }
        }
    }
}
