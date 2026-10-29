package com.marzouki.datasaver.presentation.onboarding

import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val observePlanStatus: ObservePlanStatusUseCase,
) : MviViewModel<OnboardingState, OnboardingIntent, OnboardingEffect>(
    OnboardingState(hasUsageAccess = hasUsageAccess()),
) {

    override fun observe(): List<Flow<*>> = listOf(
        observePlanStatus().onEach { status -> setState { copy(planConfigured = status == PlanStatus.CONFIGURED) } },
    )

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
