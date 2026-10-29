package com.marzouki.datasaver.presentation.settings

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.GetAppLanguageUseCase
import com.marzouki.datasaver.domain.usecase.ObserveThemeModeUseCase
import com.marzouki.datasaver.domain.usecase.SetAppLanguageUseCase
import com.marzouki.datasaver.domain.usecase.SetThemeModeUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pas d'effet : un nouveau thème ou une nouvelle langue recrée l'écran, qui relit l'état. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeThemeMode: ObserveThemeModeUseCase,
    getAppLanguage: GetAppLanguageUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
) : MviViewModel<SettingsState, SettingsIntent, Nothing>(SettingsState(language = getAppLanguage())) {

    override fun observe(): List<Flow<*>> = listOf(
        observeThemeMode().onEach { mode -> setState { copy(themeMode = mode) } },
    )

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ThemeSelected -> viewModelScope.launch { setThemeMode(intent.mode) }
            is SettingsIntent.LanguageSelected -> {
                setState { copy(language = intent.language) }
                setAppLanguage(intent.language)
            }
        }
    }
}
