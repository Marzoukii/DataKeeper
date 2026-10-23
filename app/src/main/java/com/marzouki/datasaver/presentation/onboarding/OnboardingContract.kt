package com.marzouki.datasaver.presentation.onboarding

data class OnboardingState(
    val hasUsageAccess: Boolean = true,
    /** "Plus tard" : l'écran n'est plus proposé jusqu'au prochain lancement. */
    val postponed: Boolean = false,
    /**
     * L'utilisateur est déjà passé par les paramètres : s'il revient sans l'accès, Android l'a
     * peut-être bloqué (paramètres restreints des applis installées hors boutique, Android 13+).
     */
    val settingsOpened: Boolean = false,
    /** Forfait configuré (ou « pas de forfait ») ; null tant que les préférences ne sont pas lues. */
    val planConfigured: Boolean? = null,
) {
    val showUsageAccessScreen: Boolean get() = !hasUsageAccess && !postponed

    /** Écran à afficher à la racine : accès aux données, puis configuration du forfait, puis l'appli. */
    val step: OnboardingStep
        get() = when {
            showUsageAccessScreen -> OnboardingStep.USAGE_ACCESS
            planConfigured == null -> OnboardingStep.LOADING
            !planConfigured -> OnboardingStep.PLAN_SETUP
            else -> OnboardingStep.DONE
        }
}

enum class OnboardingStep { USAGE_ACCESS, LOADING, PLAN_SETUP, DONE }

sealed interface OnboardingIntent {
    /** Écran repris : l'utilisateur revient peut-être des réglages système. */
    data object Refresh : OnboardingIntent
    data object OpenSettingsClicked : OnboardingIntent
    data object LaterClicked : OnboardingIntent
    /** Ouvrir les infos de l'appli, où se trouve « Autoriser les paramètres restreints ». */
    data object OpenAppInfoClicked : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object OpenUsageAccessSettings : OnboardingEffect
    data object OpenAppDetailsSettings : OnboardingEffect
}
