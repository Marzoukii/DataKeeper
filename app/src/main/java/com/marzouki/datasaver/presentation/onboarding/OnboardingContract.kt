package com.marzouki.datasaver.presentation.onboarding

data class OnboardingState(
    val hasUsageAccess: Boolean = true,
    /** "Plus tard" : l'écran n'est plus proposé jusqu'au prochain lancement. */
    val postponed: Boolean = false,
) {
    val showUsageAccessScreen: Boolean get() = !hasUsageAccess && !postponed
}

sealed interface OnboardingIntent {
    /** Écran repris : l'utilisateur revient peut-être des réglages système. */
    data object Refresh : OnboardingIntent
    data object OpenSettingsClicked : OnboardingIntent
    data object LaterClicked : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object OpenUsageAccessSettings : OnboardingEffect
}
