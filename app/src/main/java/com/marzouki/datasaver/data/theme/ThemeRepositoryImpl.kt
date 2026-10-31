package com.marzouki.datasaver.data.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marzouki.datasaver.domain.enums.ThemeMode
import com.marzouki.datasaver.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Thème clair, sombre ou celui du téléphone. AppCompat change le mode nuit de l'activité, que
 * Compose lit comme celui du système : `isSystemInDarkTheme()` suit donc ce choix.
 */
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> prefs[KEY_THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.LIGHT }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
        apply(mode)
    }

    override suspend fun applySavedThemeMode() {
        apply(themeMode.first())
    }

    private fun apply(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            },
        )
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
