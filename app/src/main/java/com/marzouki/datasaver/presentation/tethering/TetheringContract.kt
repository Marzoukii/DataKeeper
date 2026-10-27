package com.marzouki.datasaver.presentation.tethering

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringUsage

data class TetheringState(
    /** Null le temps de la première lecture de l'état du partage. */
    val active: Boolean? = null,
    /** Conso de la session en cours ; null partage éteint ou pas encore lue. */
    val usage: TetheringUsage? = null,
    /** Rythme des dernières minutes, plus parlant que la moyenne depuis le début. */
    val recentBytesPerHour: Long? = null,
    val alert: TetheringAlert = TetheringAlert.DEFAULT,
    val operatorName: String? = null,
    val networkGeneration: NetworkGeneration? = null,
) {
    /** Rythme affiché et utilisé pour l'estimation : récent s'il est connu, sinon moyen. */
    val bytesPerHour: Long? get() = recentBytesPerHour ?: usage?.averageBytesPerHour
}

sealed interface TetheringIntent {
    /** Écran affiché ou revenu au premier plan. */
    data object Refresh : TetheringIntent
    data class AlertToggled(val enabled: Boolean) : TetheringIntent
    data class ThresholdSelected(val thresholdBytes: Long) : TetheringIntent
    data object DevicesClicked : TetheringIntent
}

sealed interface TetheringEffect {
    /** L'alerte vient d'être activée : sur Android 13+, il faut la permission de notifier. */
    data object RequestNotificationPermission : TetheringEffect

    /** Réglages du point d'accès d'Android, seuls à lister les appareils connectés. */
    data object OpenHotspotSettings : TetheringEffect
}
