package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.MainDispatcherRule
import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.model.OpenWifiNetwork
import com.marzouki.datasaver.domain.model.WifiScan
import com.marzouki.datasaver.domain.usecase.GetWifiAvailabilityUseCase
import com.marzouki.datasaver.domain.usecase.ObserveOpenWifiUseCase
import com.marzouki.datasaver.domain.usecase.RequestWifiScanUseCase
import com.marzouki.datasaver.presentation.wifi.WifiIntent
import com.marzouki.datasaver.presentation.wifi.WifiViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WifiViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val wifi = FakeWifiRepository()
    private val viewModel = WifiViewModel(
        getAvailability = GetWifiAvailabilityUseCase(wifi),
        observeOpenWifi = ObserveOpenWifiUseCase(wifi),
        requestScan = RequestWifiScanUseCase(wifi),
    )
    private val cafe = OpenWifiNetwork(ssid = "Café", rssi = -60, frequencyMhz = 2_437)

    @Test
    fun `les réseaux ne sont suivis que lorsque tout est prêt`() = runTest(main.dispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }
        wifi.availability = WifiAvailability.WIFI_OFF
        viewModel.onIntent(WifiIntent.Refresh)
        assertEquals(0, wifi.scans.subscriptionCount.value)

        wifi.availability = WifiAvailability.READY
        viewModel.onIntent(WifiIntent.Refresh)
        assertEquals(1, wifi.scans.subscriptionCount.value)
        wifi.scans.value = WifiScan(listOf(cafe), scannedAtMillis = 5_000, connectedSsid = null)
        assertEquals(listOf(cafe), viewModel.state.value.networks)

        // Wi-Fi coupé depuis les réglages : suivi débranché, liste vidée.
        wifi.availability = WifiAvailability.WIFI_OFF
        viewModel.onIntent(WifiIntent.Refresh)
        assertEquals(0, wifi.scans.subscriptionCount.value)
        assertEquals(emptyList<OpenWifiNetwork>(), viewModel.state.value.networks)
    }
}
