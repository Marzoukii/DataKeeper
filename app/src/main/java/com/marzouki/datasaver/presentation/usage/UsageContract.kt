package com.marzouki.datasaver.presentation.usage

import com.marzouki.datasaver.domain.model.DataUsage

data class UsageState(
    val isLoading: Boolean = true,
    val hasUsageAccess: Boolean = true,
    val usage: DataUsage? = null,
    val loadFailed: Boolean = false,
)

sealed interface UsageIntent {
    /** Écran affiché/repris : l'utilisateur revient peut-être des réglages système. */
    data object Refresh : UsageIntent
    data object GrantAccessClicked : UsageIntent
}

sealed interface UsageEffect {
    data object OpenUsageAccessSettings : UsageEffect
}
