package com.marzouki.datasaver.presentation.tethering

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringSample
import com.marzouki.datasaver.domain.model.TetheringUsage
import com.marzouki.datasaver.domain.model.bytesPerSecond

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
    /** Relevés des dernières minutes, du plus ancien au plus récent : d'où vient [recentBytesPerHour]. */
    val samples: List<TetheringSample> = emptyList(),
) {
    /** Rythme affiché et utilisé pour l'estimation : récent s'il est connu, sinon moyen. */
    val bytesPerHour: Long? get() = recentBytesPerHour ?: usage?.averageBytesPerHour
}

private const val RECENT_WINDOW_MILLIS = 5 * 60_000L
private const val MIN_RECENT_SPAN_MILLIS = 60_000L

/**
 * Ajoute le relevé [usage] pris à [nowMillis] : seuls les relevés des 5 dernières minutes sont
 * gardés, et le rythme récent n'est donné qu'après une minute de recul. Réducteur pur.
 */
internal fun TetheringState.withSample(usage: TetheringUsage?, nowMillis: Long): TetheringState {
    if (usage == null) return copy(usage = null, recentBytesPerHour = null, samples = emptyList())
    // Nouvelle session ou compteur reparti de zéro : les anciens relevés ne comptent plus.
    val kept = if (samples.lastOrNull()?.let { it.usedBytes > usage.usedBytes } == true) emptyList() else samples
    val recent = (kept + TetheringSample(nowMillis, usage.usedBytes))
        .filter { it.atMillis >= nowMillis - RECENT_WINDOW_MILLIS }
    val recentSpan = recent.last().atMillis - recent.first().atMillis
    return copy(
        usage = usage,
        samples = recent,
        recentBytesPerHour = recent.bytesPerSecond()
            ?.takeIf { recentSpan >= MIN_RECENT_SPAN_MILLIS }
            ?.let { it * 3_600 },
    )
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
