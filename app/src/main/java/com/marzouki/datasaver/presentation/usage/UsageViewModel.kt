package com.marzouki.datasaver.presentation.usage

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetTopMobileAppsUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    observeQuota: ObserveQuotaUseCase,
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
    private val getDailyUsage: GetDailyUsageUseCase,
    private val getTopMobileApps: GetTopMobileAppsUseCase,
) : MviViewModel<UsageState, UsageIntent, UsageEffect>(UsageState()) {

    private var refreshJob: Job? = null

    init {
        observeQuota()
            .onEach { quota -> setState { copy(quota = quota) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: UsageIntent) {
        when (intent) {
            UsageIntent.Refresh -> refresh()
            UsageIntent.GrantAccessClicked -> sendEffect(UsageEffect.OpenUsageAccessSettings)
        }
    }

    private fun refresh() {
        if (!hasUsageAccess()) {
            setState { copy(isLoading = false, hasUsageAccess = false, usage = null) }
            return
        }
        setState { copy(isLoading = true, hasUsageAccess = true) }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val period = getCurrentPeriod()
            setState { copy(period = period) }
            // Les trois lectures sont indépendantes ; seul le total du cycle est indispensable.
            val usage = async { getMonthlyUsage(period) }
            val daily = async { getDailyUsage() }
            val apps = async { getTopMobileApps(period) }
            usage.await()
                .onSuccess { result -> setState { copy(usage = result, loadFailed = false) } }
                .onFailure { setState { copy(loadFailed = true) } }
            val dailyResult = daily.await().getOrNull()
            val appsResult = apps.await().getOrNull()
            setState {
                copy(
                    isLoading = false,
                    dailyUsage = dailyResult ?: dailyUsage,
                    topMobileApps = appsResult ?: topMobileApps,
                )
            }
        }
    }
}
