package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.MainDispatcherRule
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetMobileCounterUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaUsedBytesUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanValidityUseCase
import com.marzouki.datasaver.domain.usecase.ObservePricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.domain.usecase.SetCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.SetOperatorBalanceUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanCycleUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanPriceUseCase
import com.marzouki.datasaver.domain.usecase.SetPricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaAlertEnabledUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaUseCase
import com.marzouki.datasaver.presentation.quota.QuotaEffect
import com.marzouki.datasaver.presentation.quota.QuotaIntent
import com.marzouki.datasaver.presentation.quota.QuotaViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuotaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val quotas = FakeQuotaRepository()
    private val usage = FakeUsageRepository()
    private val tethering = FakeTetheringRepository()
    private val clock = FakeClock()

    private fun viewModel() = QuotaViewModel(
        observeQuota = ObserveQuotaUseCase(quotas),
        observeCycleStartDay = ObserveCycleStartDayUseCase(quotas),
        observePricePerGb = ObservePricePerGbUseCase(quotas),
        setPricePerGb = SetPricePerGbUseCase(quotas),
        setPlanPrice = SetPlanPriceUseCase(quotas),
        setQuota = SetQuotaUseCase(quotas, NoopQuotaScheduler),
        setQuotaAlertEnabled = SetQuotaAlertEnabledUseCase(quotas, NoopQuotaScheduler),
        setPlanCycle = SetPlanCycleUseCase(quotas, SetCycleStartDayUseCase(quotas)),
        observePlanValidity = ObservePlanValidityUseCase(quotas),
        hasUsageAccess = HasUsageAccessUseCase(usage),
        getCurrentPeriod = GetCurrentPeriodUseCase(quotas, clock),
        getMonthlyUsage = GetMonthlyUsageUseCase(usage),
        getDailyUsage = GetDailyUsageUseCase(usage, clock),
        getQuotaUsedBytes = GetQuotaUsedBytesUseCase(quotas, usage),
        setOperatorBalance = SetOperatorBalanceUseCase(quotas, clock),
        observeDataOperator = ObserveDataOperatorUseCase(FakeOperatorRepository()),
        getMobileCounter = GetMobileCounterUseCase(tethering),
        clock = clock,
    )

    @Test
    fun `avant la première lecture, le cycle affiché vient de l'horloge injectée`() {
        assertEquals(MonthPeriod.current(OCT_20), viewModel().state.value.displayPeriod)
    }

    @Test
    fun `une saisie invalide est signalée sans rien enregistrer`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(QuotaIntent.InputChanged("abc"))
        viewModel.onIntent(QuotaIntent.Save)

        assertTrue(viewModel.state.value.inputError)
        assertEquals(null, quotas.quota.value)
    }

    @Test
    fun `un quota valide est enregistré, puis la permission et le message sont demandés`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        val effects = mutableListOf<QuotaEffect>()
        backgroundScope.launch { viewModel.effects.toList(effects) }

        viewModel.onIntent(QuotaIntent.InputChanged("2,5"))
        viewModel.onIntent(QuotaIntent.Save)

        assertEquals(2_500_000_000L, quotas.quota.value?.limitBytes)
        assertEquals(
            listOf(QuotaEffect.RequestNotificationPermission, QuotaEffect.ShowMessage(R.string.quota_saved)),
            effects,
        )
    }

    @Test
    fun `une relecture lente dépassée par une plus récente n'écrase pas son résultat`() = runTest(main.dispatcher) {
        quotas.quota.value = DataQuota(25 * GB)
        val slow = CompletableDeferred<Unit>()
        var calls = 0
        usage.usageFor = { period ->
            // Première relecture bloquée sur un vieux chiffre ; la suivante répond tout de suite.
            if (++calls == 1) slow.await().let { DataUsage(0, 1 * GB, period) } else DataUsage(0, 2 * GB, period)
        }
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.onIntent(QuotaIntent.Refresh)
        viewModel.onIntent(QuotaIntent.Refresh)
        assertEquals(2 * GB, viewModel.state.value.usedMobileBytes)

        // La première relecture a été annulée : la libérer ne change plus rien.
        slow.complete(Unit)
        assertEquals(2 * GB, viewModel.state.value.usedMobileBytes)
    }

    @Test
    fun `la jauge suit le compteur en direct sans reculer à la relecture de l'historique`() = runTest(main.dispatcher) {
        quotas.quota.value = DataQuota(25 * GB)
        usage.mobileBytes = 3 * GB
        tethering.counterBytes = 10 * GB
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(QuotaIntent.Refresh)
        assertEquals(3 * GB, viewModel.state.value.status?.usedBytes)

        tethering.counterBytes = 11 * GB
        advanceTimeBy(2_001)
        assertEquals(4 * GB, viewModel.state.value.status?.usedBytes)

        // Historique relu mais encore en retard : le chiffre affiché ne recule pas.
        usage.mobileBytes = 3 * GB + 400_000_000
        viewModel.onIntent(QuotaIntent.Refresh)
        assertEquals(4 * GB, viewModel.state.value.status?.usedBytes)
    }
}
