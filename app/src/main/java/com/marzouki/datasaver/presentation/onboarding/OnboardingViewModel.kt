package com.marzouki.datasaver.presentation.onboarding

import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val hasUsageAccess: HasUsageAccessUseCase,
) : MviViewModel<OnboardingState, OnboardingIntent, OnboardingEffect>(
    OnboardingState(hasUsageAccess = hasUsageAccess()),
) {

    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Refresh -> setState { copy(hasUsageAccess = hasUsageAccess()) }
            OnboardingIntent.OpenSettingsClicked -> {
                setState { copy(settingsOpened = true) }
                sendEffect(OnboardingEffect.OpenUsageAccessSettings)
            }
            OnboardingIntent.LaterClicked -> setState { copy(postponed = true) }
            OnboardingIntent.OpenAppInfoClicked -> sendEffect(OnboardingEffect.OpenAppDetailsSettings)
        }
    }
}
