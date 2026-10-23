package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.usecase.ForgetSavedPlanUseCase
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.ObserveSavedPlansUseCase
import com.marzouki.datasaver.domain.usecase.RememberPlanUseCase
import com.marzouki.datasaver.domain.usecase.SetCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanCycleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class PlanCycleTest {

    private val gb = 1_000_000_000L

    private fun day(month: Int, dayOfMonth: Int, hour: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month, dayOfMonth, hour, 0)
        }.timeInMillis

    @Test
    fun `a 30-day pass bought on October 21 resets on November 20, then chains`() {
        val pass = MonthPeriod.pass(nowMillis = day(Calendar.OCTOBER, 23, 9), purchaseMillis = day(Calendar.OCTOBER, 21, 18), validityDays = 30)
        assertEquals(day(Calendar.OCTOBER, 21), pass.startMillis)
        assertEquals(day(Calendar.NOVEMBER, 20), pass.nextStartMillis)
        assertEquals(30, pass.totalDays)
        assertEquals(3, pass.currentDay)

        // Pas racheté : le suivant prend le relais avec la même durée.
        val next = MonthPeriod.pass(day(Calendar.NOVEMBER, 25), day(Calendar.OCTOBER, 21), 30)
        assertEquals(day(Calendar.NOVEMBER, 20), next.startMillis)
        assertEquals(pass.copy(endMillis = pass.nextStartMillis), next.previous())
    }

    @Test
    fun `a 4-day pass lasts four days, not a month`() {
        val pass = MonthPeriod.pass(day(Calendar.OCTOBER, 23), day(Calendar.OCTOBER, 22), validityDays = 4)
        assertEquals(day(Calendar.OCTOBER, 26), pass.nextStartMillis)
        assertEquals(4, pass.totalDays)
        // Jour 2 sur 4 : restent les 24 et 25 octobre.
        assertEquals(2, pass.remainingDays)
        // Les clés distinguent les cycles, pour que l'alerte reparte à chaque pass.
        assertEquals("2026-10-22+4", pass.key)
    }

    @Test
    fun `current period follows the pass when there is one, the monthly cycle otherwise`() = runTest {
        val repository = FakeQuotaRepository()
        val setCycle = SetPlanCycleUseCase(repository, SetCycleStartDayUseCase(repository))
        val now = day(Calendar.OCTOBER, 23)

        setCycle(day(Calendar.OCTOBER, 21), validityDays = 30)
        assertEquals(day(Calendar.NOVEMBER, 20), GetCurrentPeriodUseCase(repository)(now).nextStartMillis)

        // Abonnement : chaque mois le jour d'achat, et le pass est oublié.
        setCycle(day(Calendar.OCTOBER, 21), validityDays = null)
        assertNull(repository.planValidity.value)
        assertEquals(21, repository.cycleStartDay.value)
        assertEquals(day(Calendar.NOVEMBER, 21), GetCurrentPeriodUseCase(repository)(now).nextStartMillis)
    }

    @Test
    fun `plans are remembered per operator, latest price first, without duplicates`() = runTest {
        val repository = FakeQuotaRepository()
        val remember = RememberPlanUseCase(repository)
        val flexi25 = SavedPlan("60503", 25 * gb, 30_000, 30)
        val flexi220 = SavedPlan("60503", 220_000_000L, 950, 4)
        val tt = SavedPlan("60502", 10 * gb, 15_000, 30)

        remember(flexi25)
        remember(tt)
        remember(flexi220)
        // Promo : la même offre revient en tête avec son nouveau prix, sans doublon.
        remember(flexi25.copy(priceMillimes = 28_500))

        assertEquals(
            listOf(flexi25.copy(priceMillimes = 28_500), flexi220, tt),
            repository.savedPlans.value,
        )

        val operators = FakeOperatorRepository(MobileOperator("Ooredoo", "60503"))
        val observe = ObserveSavedPlansUseCase(repository, operators)
        assertEquals(listOf(flexi25.copy(priceMillimes = 28_500), flexi220), observe().first())

        // Sur une SIM Tunisie Telecom, seulement ses forfaits.
        operators.operator.value = MobileOperator("Tunisie Telecom", "60502")
        assertEquals(listOf(tt), observe().first())

        ForgetSavedPlanUseCase(repository)(tt)
        assertEquals(emptyList<SavedPlan>(), observe().first())
    }

    @Test
    fun `only the most recent plans of an operator are kept`() = runTest {
        val repository = FakeQuotaRepository()
        val remember = RememberPlanUseCase(repository)
        (1..SavedPlan.MAX_PER_OPERATOR + 2).forEach { remember(SavedPlan("60503", it * gb, null, 30)) }
        val kept = repository.savedPlans.value
        assertEquals(SavedPlan.MAX_PER_OPERATOR, kept.size)
        assertEquals((SavedPlan.MAX_PER_OPERATOR + 2) * gb, kept.first().limitBytes)
    }

    private class FakeQuotaRepository : QuotaRepository {
        override val quota = MutableStateFlow<DataQuota?>(null)
        override val lastAlertedMonth = MutableStateFlow<String?>(null)
        override val cycleStartDay = MutableStateFlow(1)
        override val pricePerGb = MutableStateFlow<PricePerGb?>(null)
        override suspend fun setQuota(quota: DataQuota?) { this.quota.value = quota }
        override suspend fun setLastAlertedMonth(monthKey: String?) { lastAlertedMonth.value = monthKey }
        override suspend fun setCycleStartDay(day: Int) { cycleStartDay.value = day }
        override suspend fun setPricePerGb(price: PricePerGb?) { pricePerGb.value = price }
        override val balanceReminderAt = MutableStateFlow<Long?>(null)
        override suspend fun setBalanceReminderAt(millis: Long?) { balanceReminderAt.value = millis }
        override val lastRenewalReminderCycle = MutableStateFlow<String?>(null)
        override suspend fun setLastRenewalReminderCycle(cycleKey: String?) { lastRenewalReminderCycle.value = cycleKey }
        override val planSetupDone = MutableStateFlow(false)
        override suspend fun setPlanSetupDone(done: Boolean) { planSetupDone.value = done }
        override val planOperatorKey = MutableStateFlow<String?>(null)
        override suspend fun setPlanOperatorKey(key: String?) { planOperatorKey.value = key }
        override val planValidity = MutableStateFlow<PlanValidity?>(null)
        override suspend fun setPlanValidity(validity: PlanValidity?) { planValidity.value = validity }
        override val savedPlans = MutableStateFlow<List<SavedPlan>>(emptyList())
        override suspend fun setSavedPlans(plans: List<SavedPlan>) { savedPlans.value = plans }
    }

    private class FakeOperatorRepository(operator: MobileOperator?) : OperatorRepository {
        val operator = MutableStateFlow(operator)
        override fun dataOperator() = operator.value
        override fun observeDataOperator() = operator
        override fun hasPhoneStatePermission() = false
        override fun observeDataNetworkGeneration() = flowOf<NetworkGeneration?>(null)
    }
}
