package com.marzouki.datasaver.presentation.quota

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.domain.usecase.SetCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaAlertEnabledUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class QuotaViewModel @Inject constructor(
    observeQuota: ObserveQuotaUseCase,
    observeCycleStartDay: ObserveCycleStartDayUseCase,
    private val setQuota: SetQuotaUseCase,
    private val setQuotaAlertEnabled: SetQuotaAlertEnabledUseCase,
    private val setCycleStartDay: SetCycleStartDayUseCase,
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
) : MviViewModel<QuotaState, QuotaIntent, QuotaEffect>(QuotaState()) {

    init {
        observeQuota()
            .onEach { quota ->
                setState {
                    copy(quota = quota, input = input.ifEmpty { quota?.limitBytes?.let(::bytesToInput).orEmpty() })
                }
            }
            .launchIn(viewModelScope)
        observeCycleStartDay()
            .onEach { day -> setState { copy(cycleStartDay = day) } }
            // La première valeur est lue par le Refresh de l'écran ; un changement relit le nouveau cycle.
            .drop(1)
            .onEach { refreshUsage() }
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
        }
    }

    private fun refreshUsage() {
        viewModelScope.launch {
            val period = getCurrentPeriod()
            setState { copy(period = period) }
            if (!hasUsageAccess()) {
                setState { copy(hasUsageAccess = false, usedMobileBytes = null) }
                return@launch
            }
            setState { copy(hasUsageAccess = true) }
            getMonthlyUsage(period).onSuccess { usage -> setState { copy(usedMobileBytes = usage.mobileBytes) } }
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

        fun bytesToInput(bytes: Long): String =
            BigDecimal(bytes).divide(BigDecimal(BYTES_PER_GB)).stripTrailingZeros().toPlainString()
    }
}
