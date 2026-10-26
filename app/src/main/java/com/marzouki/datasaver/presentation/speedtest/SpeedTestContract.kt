package com.marzouki.datasaver.presentation.speedtest

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.SpeedTestProgress
import com.marzouki.datasaver.domain.model.SpeedTestResult

data class SpeedTestState(
    /** Réseau non facturé (Wi-Fi) : le test ne touche pas au forfait. */
    val isOnWifi: Boolean = false,
    val operatorName: String? = null,
    val generation: NetworkGeneration? = null,
    /** Test en cours ; null hors test. */
    val progress: SpeedTestProgress? = null,
    /** Test qui vient de se terminer, affiché en écran de résultat. */
    val result: SpeedTestResult? = null,
    val history: List<SpeedTestResult> = emptyList(),
    /** Demande de confirmation avant un test en données mobiles. */
    val showMobileWarning: Boolean = false,
)

sealed interface SpeedTestIntent {
    data object StartClicked : SpeedTestIntent
    data object MobileTestConfirmed : SpeedTestIntent
    data object MobileWarningDismissed : SpeedTestIntent
    data object CancelClicked : SpeedTestIntent
}

sealed interface SpeedTestEffect {
    data object Failed : SpeedTestEffect
}
