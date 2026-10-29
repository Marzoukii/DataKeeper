package com.marzouki.datasaver.presentation.wifi

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.usecase.GetWifiAvailabilityUseCase
import com.marzouki.datasaver.domain.usecase.ObserveOpenWifiUseCase
import com.marzouki.datasaver.domain.usecase.RequestWifiScanUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Les réseaux ne sont suivis que lorsque tout est prêt : permission accordée, Wi-Fi et localisation activés. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WifiViewModel @Inject constructor(
    private val getAvailability: GetWifiAvailabilityUseCase,
    private val observeOpenWifi: ObserveOpenWifiUseCase,
    private val requestScan: RequestWifiScanUseCase,
) : MviViewModel<WifiState, WifiIntent, WifiEffect>(WifiState()) {

    private var scanTimeoutJob: Job? = null

    override fun observe(): List<Flow<*>> = listOf(
        // Branché dès que le Refresh de l'écran trouve tout prêt, débranché sinon.
        select { availability == WifiAvailability.READY }
            .flatMapLatest { ready -> if (ready) observeOpenWifi() else emptyFlow() }
            .onEach { scan ->
                setState {
                    copy(
                        networks = scan.networks,
                        scannedAtMillis = scan.scannedAtMillis,
                        connectedSsid = scan.connectedSsid,
                        // Le premier envoi reprend les anciens résultats : on attend ceux du scan demandé.
                        isScanning = isScanning && scan.scannedAtMillis == scannedAtMillis,
                    )
                }
            },
    )

    override fun onIntent(intent: WifiIntent) {
        when (intent) {
            WifiIntent.Refresh -> refresh()
            WifiIntent.RescanClicked -> scan()
            is WifiIntent.FilterSelected -> setState { copy(filter = intent.filter) }
            WifiIntent.ConnectClicked, WifiIntent.EnableWifiClicked -> sendEffect(WifiEffect.OpenWifiPanel)
            WifiIntent.EnableLocationClicked -> sendEffect(WifiEffect.OpenLocationSettings)
        }
    }

    private fun refresh() {
        val availability = getAvailability()
        setState { copy(availability = availability) }
        if (availability != WifiAvailability.READY) {
            setState { copy(networks = emptyList(), scannedAtMillis = null, connectedSsid = null, isScanning = false) }
            return
        }
        scan()
    }

    private fun scan() {
        if (currentState.availability != WifiAvailability.READY || !requestScan()) return
        setState { copy(isScanning = true) }
        // Un scan peut échouer sans nouveaux résultats : l'indicateur ne doit pas tourner indéfiniment.
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MILLIS)
            setState { copy(isScanning = false) }
        }
    }

    private companion object {
        const val SCAN_TIMEOUT_MILLIS = 10_000L
    }
}
