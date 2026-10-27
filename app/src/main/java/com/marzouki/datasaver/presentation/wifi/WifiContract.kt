package com.marzouki.datasaver.presentation.wifi

import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.enums.WifiPortal
import com.marzouki.datasaver.domain.model.OpenWifiNetwork

/** Filtres de la liste : tous, ceux où internet passe sans portail, ou seulement le plus proche. */
enum class WifiFilter { ALL, NO_ADS, NEAREST }

data class WifiState(
    /** Null le temps de la première vérification. */
    val availability: WifiAvailability? = null,
    /** Réseaux ouverts, du plus proche au plus lointain. */
    val networks: List<OpenWifiNetwork> = emptyList(),
    val scannedAtMillis: Long? = null,
    val connectedSsid: String? = null,
    val filter: WifiFilter = WifiFilter.ALL,
    /** Scan demandé, en attente de résultats. */
    val isScanning: Boolean = false,
) {
    val noAdsCount: Int get() = networks.count { it.portal == WifiPortal.NO_PORTAL }

    val visibleNetworks: List<OpenWifiNetwork>
        get() = when (filter) {
            WifiFilter.ALL -> networks
            WifiFilter.NO_ADS -> networks.filter { it.portal == WifiPortal.NO_PORTAL }
            WifiFilter.NEAREST -> networks.take(1)
        }
}

sealed interface WifiIntent {
    /** Écran affiché ou revenu au premier plan : la permission, le Wi-Fi ou la localisation ont pu changer. */
    data object Refresh : WifiIntent
    data object RescanClicked : WifiIntent
    data class FilterSelected(val filter: WifiFilter) : WifiIntent
    data object ConnectClicked : WifiIntent
    data object EnableWifiClicked : WifiIntent
    data object EnableLocationClicked : WifiIntent
}

sealed interface WifiEffect {
    /** Liste des Wi-Fi d'Android, où l'on touche le réseau à rejoindre. */
    data object OpenWifiPanel : WifiEffect
    data object OpenLocationSettings : WifiEffect
}
