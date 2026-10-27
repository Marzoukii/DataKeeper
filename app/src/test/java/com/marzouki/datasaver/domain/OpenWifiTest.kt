package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.enums.WifiPortal
import com.marzouki.datasaver.domain.enums.WifiSignal
import com.marzouki.datasaver.domain.model.OpenWifiNetwork
import com.marzouki.datasaver.presentation.wifi.WifiFilter
import com.marzouki.datasaver.presentation.wifi.WifiState
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenWifiTest {

    private fun network(ssid: String, rssi: Int, frequencyMhz: Int = 2437, portal: WifiPortal = WifiPortal.UNKNOWN) =
        OpenWifiNetwork(ssid, rssi, frequencyMhz, portal)

    @Test
    fun `distance estimée, arrondie à 5 m et jamais nulle`() {
        assertEquals(5, network("a", rssi = -40).estimatedDistanceMeters)
        assertEquals(10, network("a", rssi = -67).estimatedDistanceMeters)
        assertEquals(100, network("a", rssi = -94).estimatedDistanceMeters)
    }

    @Test
    fun `le 5 GHz s'affaiblit plus vite, à signal égal la borne est plus proche`() {
        assertEquals(10, network("a", rssi = -73, frequencyMhz = 5180).estimatedDistanceMeters)
    }

    @Test
    fun `paliers de signal`() {
        assertEquals(WifiSignal.STRONG, network("a", rssi = -65).signal)
        assertEquals(WifiSignal.MEDIUM, network("a", rssi = -66).signal)
        assertEquals(WifiSignal.MEDIUM, network("a", rssi = -78).signal)
        assertEquals(WifiSignal.WEAK, network("a", rssi = -79).signal)
    }

    @Test
    fun `filtres de la liste`() {
        val networks = listOf(
            network("Café", rssi = -60, portal = WifiPortal.WITH_PORTAL),
            network("Hôtel", rssi = -70, portal = WifiPortal.NO_PORTAL),
            network("Librairie", rssi = -80),
        )
        val state = WifiState(networks = networks)

        assertEquals(networks, state.visibleNetworks)
        assertEquals(1, state.noAdsCount)
        assertEquals(listOf("Hôtel"), state.copy(filter = WifiFilter.NO_ADS).visibleNetworks.map { it.ssid })
        assertEquals(listOf("Café"), state.copy(filter = WifiFilter.NEAREST).visibleNetworks.map { it.ssid })
    }
}
