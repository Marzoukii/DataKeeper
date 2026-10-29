package com.marzouki.datasaver.presentation.usage

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaUsedBytesUseCase
import com.marzouki.datasaver.domain.usecase.GetTetheringUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetTopMobileAppsUseCase
import com.marzouki.datasaver.domain.usecase.HasPhoneStatePermissionUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataNetworkGenerationUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveHotspotActiveUseCase
import com.marzouki.datasaver.domain.usecase.ObservePricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageViewModel @Inject constructor(
    private val observeQuota: ObserveQuotaUseCase,
    private val observePricePerGb: ObservePricePerGbUseCase,
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
    private val getDailyUsage: GetDailyUsageUseCase,
    private val getTopMobileApps: GetTopMobileAppsUseCase,
    private val getQuotaUsedBytes: GetQuotaUsedBytesUseCase,
    private val observeDataOperator: ObserveDataOperatorUseCase,
    private val hasPhoneStatePermission: HasPhoneStatePermissionUseCase,
    private val observeDataNetworkGeneration: ObserveDataNetworkGenerationUseCase,
    private val observeHotspotActive: ObserveHotspotActiveUseCase,
    private val getTetheringUsage: GetTetheringUsageUseCase,
) : MviViewModel<UsageState, UsageIntent, UsageEffect>(UsageState()) {

    private var refreshJob: Job? = null
    private var hotspotActive = false

    /** Relue au retour sur l'écran : la permission a pu être accordée ou retirée dans les paramètres. */
    private val phonePermission = MutableStateFlow(hasPhoneStatePermission())

    override fun observe(): List<Flow<*>> = listOf(
        phonePermission
            .onEach { granted -> setState { copy(hasPhonePermission = granted) } }
            .flatMapLatest { granted -> if (granted) observeDataNetworkGeneration() else flowOf(null) }
            .onEach { generation -> setState { copy(networkGeneration = generation) } },
        observeQuota()
            .onEach { quota -> setState { copy(quota = quota) } }
            // Le décompte du forfait dépend du quota : la première valeur est lue par le Refresh de l'écran.
            .drop(1)
            .onEach { refreshQuota() },
        observePricePerGb().onEach { price -> setState { copy(pricePerGb = price) } },
        // Suit la SIM des données en direct : carte changée ou data basculée sur l'autre SIM.
        observeDataOperator().onEach { operator -> setState { copy(operatorName = operator?.name) } },
        // Carte du partage de connexion : affichée dès qu'il s'allume, sa conso relue toutes les 5 s.
        observeHotspotActive()
            .onEach { active -> hotspotActive = active }
            .flatMapLatest { active ->
                if (active) {
                    flow {
                        while (true) {
                            emit(Unit)
                            delay(TETHERING_REFRESH_MILLIS)
                        }
                    }
                } else {
                    flowOf(Unit)
                }
            }
            .onEach { refreshTethering() },
    )

    private fun refreshTethering() {
        if (!hotspotActive) {
            setState { copy(tethering = null) }
            return
        }
        viewModelScope.launch {
            val usage = getTetheringUsage(active = true)
            setState { copy(tethering = usage) }
        }
    }

    private companion object {
        const val TETHERING_REFRESH_MILLIS = 5_000L
    }

    override fun onIntent(intent: UsageIntent) {
        when (intent) {
            UsageIntent.Refresh -> {
                phonePermission.value = hasPhoneStatePermission()
                refresh()
                refreshTethering()
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
            setState { copy(period = period) }
            // Les lectures sont indépendantes ; seul le total du cycle est indispensable.
            val usage = async { getMonthlyUsage(period) }
            val daily = async { getDailyUsage() }
            val apps = async { getTopMobileApps(period) }
            val quotaUsed = async { getQuotaUsedBytes(period) }
            usage.await()
                .onSuccess { result -> setState { copy(usage = result, loadFailed = false) } }
                .onFailure { setState { copy(loadFailed = true) } }
            val dailyResult = daily.await().getOrNull()
            val appsResult = apps.await().getOrNull()
            val quotaUsedResult = quotaUsed.await()
            setState {
                copy(
                    isLoading = false,
                    dailyUsage = dailyResult ?: dailyUsage,
                    topMobileApps = appsResult ?: topMobileApps,
                    quotaUsedBytes = quotaUsedResult.getOrElse { quotaUsedBytes },
                )
            }
        }
    }

    private fun refreshQuota() {
        val period = currentState.period ?: return
        if (!hasUsageAccess()) return
        viewModelScope.launch {
            getQuotaUsedBytes(period).onSuccess { used -> setState { copy(quotaUsedBytes = used) } }
        }
    }
}
