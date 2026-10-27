package com.marzouki.datasaver.presentation.settings

import com.marzouki.datasaver.domain.enums.AppLanguageChoice
import com.marzouki.datasaver.domain.enums.ThemeMode

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguageChoice = AppLanguageChoice.SYSTEM,
)

/** Chaque choix s'applique tout de suite : l'écran se recrée dans le nouveau thème ou la nouvelle langue. */
sealed interface SettingsIntent {
    data class ThemeSelected(val mode: ThemeMode) : SettingsIntent
    data class LanguageSelected(val language: AppLanguageChoice) : SettingsIntent
}
