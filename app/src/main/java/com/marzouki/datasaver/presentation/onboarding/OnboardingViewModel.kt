package com.marzouki.datasaver.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.model.PlanStatus
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val hasUsageAccess: HasUsageAccessUseCase,
    observePlanStatus: ObservePlanStatusUseCase,
) : MviViewModel<OnboardingState, OnboardingIntent, OnboardingEffect>(
    OnboardingState(hasUsageAccess = hasUsageAccess()),
) {

    init {
        observePlanStatus()
            .onEach { status -> setState { copy(planConfigured = status == PlanStatus.CONFIGURED) } }
            .launchIn(viewModelScope)
    }

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
