package com.marzouki.datasaver.presentation.usage

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaStreakUseCase
import com.marzouki.datasaver.domain.usecase.GetTopMobileAppsUseCase
import com.marzouki.datasaver.domain.usecase.HasPhoneStatePermissionUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataNetworkGenerationUseCase
import com.marzouki.datasaver.domain.usecase.ObservePricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageViewModel @Inject constructor(
    observeQuota: ObserveQuotaUseCase,
    observePricePerGb: ObservePricePerGbUseCase,
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
    private val getDailyUsage: GetDailyUsageUseCase,
    private val getTopMobileApps: GetTopMobileAppsUseCase,
    private val getQuotaStreak: GetQuotaStreakUseCase,
    private val getDataOperator: GetDataOperatorUseCase,
    private val hasPhoneStatePermission: HasPhoneStatePermissionUseCase,
    observeDataNetworkGeneration: ObserveDataNetworkGenerationUseCase,
) : MviViewModel<UsageState, UsageIntent, UsageEffect>(UsageState()) {

    private var refreshJob: Job? = null

    /** Relue au retour sur l'écran : la permission a pu être accordée ou retirée dans les paramètres. */
    private val phonePermission = MutableStateFlow(hasPhoneStatePermission())

    init {
        phonePermission
            .onEach { granted -> setState { copy(hasPhonePermission = granted) } }
            .flatMapLatest { granted -> if (granted) observeDataNetworkGeneration() else flowOf(null) }
            .onEach { generation -> setState { copy(networkGeneration = generation) } }
            .launchIn(viewModelScope)
        observeQuota()
            .onEach { quota -> setState { copy(quota = quota) } }
            // La série dépend du quota : la première valeur est lue par le Refresh de l'écran.
            .drop(1)
            .onEach { refreshStreak() }
            .launchIn(viewModelScope)
        observePricePerGb()
            .onEach { price -> setState { copy(pricePerGb = price) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: UsageIntent) {
        when (intent) {
            UsageIntent.Refresh -> {
                phonePermission.value = hasPhoneStatePermission()
                refresh()
            }
            UsageIntent.GrantAccessClicked -> sendEffect(UsageEffect.OpenUsageAccessSettings)
            UsageIntent.PhonePermissionResult -> phonePermission.value = hasPhoneStatePermission()
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
            // La SIM des données peut changer entre deux visites (double SIM).
            setState { copy(period = period, operatorName = getDataOperator()?.name) }
            // Les trois lectures sont indépendantes ; seul le total du cycle est indispensable.
            val usage = async { getMonthlyUsage(period) }
            val daily = async { getDailyUsage() }
            val apps = async { getTopMobileApps(period) }
            val streak = async { getQuotaStreak(period) }
            usage.await()
                .onSuccess { result -> setState { copy(usage = result, loadFailed = false) } }
                .onFailure { setState { copy(loadFailed = true) } }
            val dailyResult = daily.await().getOrNull()
            val appsResult = apps.await().getOrNull()
            val streakResult = streak.await()
            setState {
                copy(
                    isLoading = false,
                    dailyUsage = dailyResult ?: dailyUsage,
                    topMobileApps = appsResult ?: topMobileApps,
                    streak = streakResult.getOrElse { this.streak },
                )
            }
        }
    }

    private fun refreshStreak() {
        val period = currentState.period ?: return
        if (!hasUsageAccess()) return
        viewModelScope.launch {
            getQuotaStreak(period).onSuccess { result -> setState { copy(streak = result) } }
        }
    }
}
