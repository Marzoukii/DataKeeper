package com.marzouki.datasaver.presentation.usage

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.model.withHistory
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetMobileCounterUseCase
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
    private val getMobileCounter: GetMobileCounterUseCase,
) : MviViewModel<UsageState, UsageIntent, UsageEffect>(
    UsageState(hasPhonePermission = hasPhoneStatePermission()),
) {

    private var refreshJob: Job? = null

    override fun observe(): List<Flow<*>> = listOf(
        // La permission est relue au retour sur l'écran : elle a pu être accordée ou retirée dans les paramètres.
        select { hasPhonePermission }
            .flatMapLatest { granted -> if (granted) observeDataNetworkGeneration() else flowOf(null) }
            .onEach { generation -> setState { copy(networkGeneration = generation) } },
        observeQuota()
            .onEach { quota -> setState { copy(quota = quota) } }
            // Le décompte du forfait dépend du quota : la première valeur est lue par le Refresh de l'écran.
            // Un nouveau forfait ou un nouveau solde change aussi le cycle (pass, heure d'activation) :
            // on relit tout, cycle compris, sinon le solde saisi tombe hors de l'ancien cycle et le reste
            // affiché ignore la data déjà partie jusqu'au prochain rafraîchissement.
            .drop(1)
            .onEach { refresh() },
        observePricePerGb().onEach { price -> setState { copy(pricePerGb = price) } },
        // Suit la SIM des données en direct : carte changée ou data basculée sur l'autre SIM.
        observeDataOperator().onEach { operator -> setState { copy(operatorName = operator?.name) } },
        // Carte du partage de connexion : affichée dès qu'il s'allume, sa conso relue toutes les 5 s.
        observeHotspotActive()
            .onEach { active -> setState { copy(hotspotActive = active) } }
            .flatMapLatest { active -> if (active) every(TETHERING_REFRESH_MILLIS) else flowOf(Unit) }
            .onEach { refreshTethering() },
        // L'historique d'Android est en retard de plusieurs minutes : sans le compteur en direct,
        // le total et le quota resteraient figés pendant un partage de connexion.
        every(LIVE_MOBILE_MILLIS).onEach {
            val counter = getMobileCounter() ?: return@onEach
            setState { copy(liveMobile = liveMobile?.withCounter(counter)) }
        },
    )

    private fun every(millis: Long) = flow {
        while (true) {
            emit(Unit)
            delay(millis)
        }
    }

    private fun refreshTethering() {
        if (!currentState.hotspotActive) {
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
        const val LIVE_MOBILE_MILLIS = 2_000L
    }

    override fun onIntent(intent: UsageIntent) {
        when (intent) {
            UsageIntent.Refresh -> {
                setState { copy(hasPhonePermission = hasPhoneStatePermission()) }
                refresh()
                refreshTethering()
            }
            UsageIntent.GrantAccessClicked -> sendEffect(UsageEffect.OpenUsageAccessSettings)
            UsageIntent.PhonePermissionResult -> setState { copy(hasPhonePermission = hasPhoneStatePermission()) }
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
            // Relevé avant l'historique : ce qui passe pendant la lecture compte comme du direct.
            val counter = getMobileCounter()
            // Les lectures sont indépendantes ; seul le total du cycle est indispensable.
            val usage = async { getMonthlyUsage(period) }
            val daily = async { getDailyUsage() }
            val apps = async { getTopMobileApps(period) }
            val quotaUsed = async { getQuotaUsedBytes(period) }
            usage.await()
                .onSuccess { result ->
                    setState {
                        copy(
                            usage = result,
                            loadFailed = false,
                            liveMobile = counter?.let { liveMobile.withHistory(period.startMillis, result.mobileBytes, it) },
                        )
                    }
                }
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
}
