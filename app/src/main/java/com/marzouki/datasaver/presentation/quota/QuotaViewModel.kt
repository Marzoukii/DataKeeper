package com.marzouki.datasaver.presentation.quota

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.alignDailyToUsed
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaUsedBytesUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.ObservePricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.domain.usecase.SetCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.SetOperatorBalanceUseCase
import com.marzouki.datasaver.domain.usecase.SetPricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaAlertEnabledUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class QuotaViewModel @Inject constructor(
    private val observeQuota: ObserveQuotaUseCase,
    observeCycleStartDay: ObserveCycleStartDayUseCase,
    observePricePerGb: ObservePricePerGbUseCase,
    private val setPricePerGb: SetPricePerGbUseCase,
    private val setQuota: SetQuotaUseCase,
    private val setQuotaAlertEnabled: SetQuotaAlertEnabledUseCase,
    private val setCycleStartDay: SetCycleStartDayUseCase,
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
    private val getDailyUsage: GetDailyUsageUseCase,
    private val getQuotaUsedBytes: GetQuotaUsedBytesUseCase,
    private val setOperatorBalance: SetOperatorBalanceUseCase,
    private val getDataOperator: GetDataOperatorUseCase,
): MviViewModel<QuotaState, QuotaIntent, QuotaEffect>(QuotaState()) {

    init {
        observeQuota()
            .onEach { quota ->
                setState {
                    copy(quota = quota, input = input.ifEmpty { quota?.limitBytes?.let(::bytesToInput).orEmpty() })
                }
            }
            // La première valeur est lue par le Refresh de l'écran ; un nouveau solde change le décompte.
            .drop(1)
            .onEach { refreshUsage() }
            .launchIn(viewModelScope)
        observeCycleStartDay()
            .onEach { day -> setState { copy(cycleStartDay = day) } }
            // La première valeur est lue par le Refresh de l'écran ; un changement relit le nouveau cycle.
            .drop(1)
            .onEach { refreshUsage() }
            .launchIn(viewModelScope)
        observePricePerGb()
            .onEach { price -> setState { copy(pricePerGb = price) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: QuotaIntent) {
        when (intent) {
            QuotaIntent.Refresh -> refreshUsage()
            is QuotaIntent.InputChanged -> setState { copy(input = intent.value, inputError = false) }
            is QuotaIntent.PresetSelected -> setState { copy(input = intent.gigabytes.toString(), inputError = false) }
            QuotaIntent.Save -> save()
            QuotaIntent.RemoveQuota -> viewModelScope.launch {
                setQuota(null)
                setState { copy(input = "") }
                sendEffect(QuotaEffect.ShowMessage(R.string.quota_removed))
            }
            QuotaIntent.GrantAccessClicked -> sendEffect(QuotaEffect.OpenUsageAccessSettings)
            is QuotaIntent.AlertToggled -> viewModelScope.launch {
                setQuotaAlertEnabled(intent.enabled)
                if (intent.enabled) sendEffect(QuotaEffect.RequestNotificationPermission)
            }
            is QuotaIntent.CycleStartDaySelected -> viewModelScope.launch { setCycleStartDay(intent.day) }
            is QuotaIntent.BalanceSaved -> viewModelScope.launch {
                setOperatorBalance(intent.remainingBytes)
                sendEffect(
                    QuotaEffect.ShowMessage(
                        if (intent.remainingBytes != null) R.string.quota_balance_saved else R.string.quota_balance_removed,
                    ),
                )
            }
            is QuotaIntent.PricePerGbSaved -> viewModelScope.launch {
                setPricePerGb(intent.price)
                if (intent.price != null) sendEffect(QuotaEffect.ShowMessage(R.string.quota_price_saved))
            }
        }
    }

    private fun refreshUsage() {
        viewModelScope.launch {
            val period = getCurrentPeriod()
            // La SIM des données peut changer entre deux visites (double SIM).
            setState { copy(period = period, operatorName = getDataOperator()?.name) }
            if (!hasUsageAccess()) {
                setState { copy(hasUsageAccess = false, usedMobileBytes = null, dailyMobileBytes = null, measuredDailyMobileBytes = null) }
                return@launch
            }
            setState { copy(hasUsageAccess = true) }
            val quota = observeQuota().first()
            // Recalée sur le solde opérateur s'il a été relevé ce cycle ; sinon tout le mobile du cycle.
            val used = getQuotaUsedBytes(period).getOrNull() ?: getMonthlyUsage(period).getOrNull()?.mobileBytes
            if (used != null) setState { copy(usedMobileBytes = used) }
            // Un point par jour écoulé du cycle : du premier jour à aujourd'hui.
            getDailyUsage(days = period.currentDay, nowMillis = period.endMillis).onSuccess { days ->
                val daily = days.map { it.mobileBytes }
                val balance = quota?.balanceIn(period)
                // La courbe doit finir sur le même total que la jauge.
                val aligned = if (balance != null && used != null) {
                    alignDailyToUsed(daily, used, period.dayIndexOf(balance.atMillis))
                } else {
                    daily
                }
                setState { copy(dailyMobileBytes = aligned, measuredDailyMobileBytes = daily) }
            }
        }
    }

    private fun save() {
        val bytes = parseGigabytes(currentState.input)
        if (bytes == null) {
            setState { copy(inputError = true) }
            return
        }
        viewModelScope.launch {
            setQuota(bytes)
            sendEffect(QuotaEffect.RequestNotificationPermission)
            sendEffect(QuotaEffect.ShowMessage(R.string.quota_saved))
        }
    }

    companion object {
        private const val BYTES_PER_GB = 1_000_000_000L
        private const val MAX_GB = 10_000.0

        /** "2,5" ou "2.5" Go → octets ; null si invalide. */
        fun parseGigabytes(input: String): Long? {
            val gb = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
            if (gb <= 0.0 || gb > MAX_GB) return null
            return (gb * BYTES_PER_GB).roundToLong()
        }

        /** Solde restant en Go, de 0 au forfait compris → octets ; null si invalide. */
        fun parseBalance(input: String, limitBytes: Long): Long? {
            val gb = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
            val bytes = (gb * BYTES_PER_GB).roundToLong()
            return bytes.takeIf { gb >= 0.0 && it <= limitBytes }
        }

        fun bytesToInput(bytes: Long): String =
            BigDecimal(bytes).divide(BigDecimal(BYTES_PER_GB)).stripTrailingZeros().toPlainString()
    }
}
