package com.marzouki.datasaver.domain.model

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.enums.SpeedTestPhase

/** Test de débit terminé, conservé dans l'historique. */
data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Int,
    val atMillis: Long,
    /** Mesuré sur un réseau non facturé (Wi-Fi) plutôt qu'en données mobiles. */
    val onWifi: Boolean,
    /** Opérateur et génération de la SIM des données, pour un test en données mobiles. */
    val operatorName: String? = null,
    val generation: NetworkGeneration? = null,
    /** Serveur de mesure retenu, le plus proche de l’appareil. */
    val server: SpeedTestServer? = null,
)

/**
 * Avancement d'un test en cours. Les mesures des étapes terminées restent renseignées ;
 * [currentMbps] est le débit moyen de l'étape en cours, depuis son début.
 */
data class SpeedTestProgress(
    val phase: SpeedTestPhase,
    /** Avancement global, de 0 à 1. */
    val fraction: Float,
    val currentMbps: Double? = null,
    val pingMs: Int? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val server: SpeedTestServer? = null,
)

/** Ce que voit l'écran pendant un test : son avancement, puis son résultat une fois enregistré. */
sealed interface SpeedTestEvent {
    data class Progress(val progress: SpeedTestProgress) : SpeedTestEvent
    data class Finished(val result: SpeedTestResult) : SpeedTestEvent
}

/** Serveur de mesure : son exploitant ([sponsor]) et son emplacement ([name]), ex. « Agence Tunisienne d'Internet » à « Kasbah ». */
data class SpeedTestServer(val sponsor: String, val name: String) {
    /** « Agence Tunisienne d'Internet · Kasbah ». */
    val label: String get() = listOf(sponsor, name).filter { it.isNotBlank() }.joinToString(" · ")
}
