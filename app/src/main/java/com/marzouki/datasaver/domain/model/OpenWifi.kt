package com.marzouki.datasaver.domain.model

import com.marzouki.datasaver.domain.enums.WifiPortal
import com.marzouki.datasaver.domain.enums.WifiSignal
import kotlin.math.pow
import kotlin.math.roundToInt

/** Wi-Fi sans mot de passe capté au dernier scan ; un même nom vu par plusieurs bornes n'apparaît qu'une fois. */
data class OpenWifiNetwork(
    val ssid: String,
    /** Puissance reçue en dBm, de la borne la mieux captée. */
    val rssi: Int,
    val frequencyMhz: Int,
    val portal: WifiPortal = WifiPortal.UNKNOWN,
) {
    val signal: WifiSignal
        get() = when {
            rssi >= -65 -> WifiSignal.STRONG
            rssi >= -78 -> WifiSignal.MEDIUM
            else -> WifiSignal.WEAK
        }

    /**
     * Distance estimée d'après l'affaiblissement du signal, arrondie à 5 m : un ordre de grandeur,
     * les murs et la puissance de la borne la faussent facilement. Le 5 GHz s'affaiblit plus vite à
     * distance égale, d'où une référence à 1 m plus basse.
     */
    val estimatedDistanceMeters: Int
        get() {
            val rssiAtOneMeter = if (frequencyMhz >= 4_900) -46.0 else -40.0
            val meters = 10.0.pow((rssiAtOneMeter - rssi) / (10 * PATH_LOSS_EXPONENT))
            return ((meters / 5).roundToInt() * 5).coerceAtLeast(5)
        }

    private companion object {
        /** Affaiblissement en ville, entre l'espace libre (2) et l'intérieur d'un bâtiment (3 à 4). */
        const val PATH_LOSS_EXPONENT = 2.7
    }
}

/** Résultat d'un scan : réseaux ouverts du plus proche au plus lointain. */
data class WifiScan(
    val networks: List<OpenWifiNetwork>,
    val scannedAtMillis: Long?,
    /** Wi-Fi auquel le téléphone est connecté, s'il est connu. */
    val connectedSsid: String?,
)
