package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.AppKind
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.NetworkGeneration
import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.model.PlanStatus
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.model.heavyAppFor
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import com.marzouki.datasaver.domain.usecase.CheckQuotaAndAlertUseCase
import com.marzouki.datasaver.domain.usecase.ClearStaleBalanceUseCase
import com.marzouki.datasaver.domain.usecase.CompletePlanSetupUseCase
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaUsedBytesUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.domain.usecase.RememberPlanUseCase
import com.marzouki.datasaver.domain.usecase.RemindBalanceRecalibrationUseCase
import com.marzouki.datasaver.domain.usecase.RemindPlanRenewalUseCase
import com.marzouki.datasaver.domain.usecase.SetCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.SetOperatorBalanceUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanCycleUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanPriceUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaUseCase
import com.marzouki.datasaver.domain.usecase.SkipPlanSetupUseCase
import com.marzouki.datasaver.presentation.quota.QuotaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class QuotaAlertTest {

    private val gb = 1_000_000_000L
    private val period = MonthPeriod(startMillis = 0, endMillis = 1, key = "2026-10")

    private class FakeQuotaRepository(quota: DataQuota?) : QuotaRepository {
        override val quota = MutableStateFlow(quota)
        override val lastAlertedMonth = MutableStateFlow<String?>(null)
        override suspend fun setQuota(quota: DataQuota?) { this.quota.value = quota }
        override suspend fun setLastAlertedMonth(monthKey: String?) { lastAlertedMonth.value = monthKey }
        override val cycleStartDay = MutableStateFlow(1)
        override suspend fun setCycleStartDay(day: Int) { cycleStartDay.value = day }
        override val pricePerGb = MutableStateFlow<PricePerGb?>(null)
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

    private class FakeUsageRepository(var mobileBytes: Long) : UsageRepository {
        override fun hasUsageAccess() = true
        override suspend fun getUsage(period: MonthPeriod) = DataUsage(0, mobileBytes, period)
        override suspend fun getDailyUsage(days: Int, nowMillis: Long) = emptyList<DayUsage>()
        override suspend fun getTopMobileApps(period: MonthPeriod, limit: Int) = emptyList<AppDataUsage>()
    }

    @Test
    fun `does not notify when alert is disabled`() = runTest {
        val notifier = FakeNotifier()
        val quota = DataQuota(10 * gb, alertEnabled = false)
        assertFalse(CheckQuotaAndAlertUseCase(FakeQuotaRepository(quota), FakeUsageRepository(9 * gb), notifier)(period))
        assertTrue(notifier.notified.isEmpty())
    }

    @Test
    fun `cycle starts on chosen day and counts days`() {
        val now = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 20, 15, 0) }.timeInMillis
        val calendarMonth = MonthPeriod.current(now)
        assertEquals("2026-10", calendarMonth.key)
        assertEquals(31, calendarMonth.totalDays)
        assertEquals(20, calendarMonth.currentDay)
        assertEquals(11, calendarMonth.remainingDays)

        val fromThe25th = MonthPeriod.current(now, cycleStartDay = 25)
        assertEquals("2026-09", fromThe25th.key)
        assertEquals(30, fromThe25th.totalDays)
        assertEquals(26, fromThe25th.currentDay)
    }

    private class FakeNotifier : QuotaAlertNotifier {
        val notified = mutableListOf<QuotaStatus>()
        override fun notifyThresholdReached(status: QuotaStatus) { notified += status }
        val recalibrations = mutableListOf<Long>()
        override fun notifyBalanceRecalibration(remainingBytes: Long, sinceMillis: Long) { recalibrations += remainingBytes }
        val renewals = mutableListOf<Long>()
        override fun notifyPlanRenewal(renewalMillis: Long) { renewals += renewalMillis }
    }

    @Test
    fun `alert is reached at 80 percent`() {
        assertFalse(QuotaStatus(DataQuota(10 * gb), 7_999_999_999).isAlertReached)
        assertTrue(QuotaStatus(DataQuota(10 * gb), 8 * gb).isAlertReached)
    }

    @Test
    fun `notifies only once per month`() = runTest {
        val notifier = FakeNotifier()
        val useCase = CheckQuotaAndAlertUseCase(
            FakeQuotaRepository(DataQuota(10 * gb)), FakeUsageRepository(9 * gb), notifier,
        )

        assertTrue(useCase(period))
        assertFalse(useCase(period))
        assertEquals(1, notifier.notified.size)
        assertTrue(useCase(period.copy(key = "2026-11")))
    }

    @Test
    fun `does not notify below threshold or without quota`() = runTest {
        val notifier = FakeNotifier()
        assertFalse(CheckQuotaAndAlertUseCase(FakeQuotaRepository(DataQuota(10 * gb)), FakeUsageRepository(gb), notifier)(period))
        assertFalse(CheckQuotaAndAlertUseCase(FakeQuotaRepository(null), FakeUsageRepository(99 * gb), notifier)(period))
        assertTrue(notifier.notified.isEmpty())
    }

    /** 42,8 Go de mobile depuis le début du cycle, dont 1 Go depuis le relevé du solde à 500. */
    private class CycleUsageRepository : UsageRepository {
        override fun hasUsageAccess() = true
        override suspend fun getUsage(period: MonthPeriod) =
            DataUsage(0, if (period.startMillis == 500L) 1_000_000_000L else 42_800_000_000L, period)
        override suspend fun getDailyUsage(days: Int, nowMillis: Long) = emptyList<DayUsage>()
        override suspend fun getTopMobileApps(period: MonthPeriod, limit: Int) = emptyList<AppDataUsage>()
    }

    @Test
    fun `operator balance counts the plan from the reading, not from the cycle start`() = runTest {
        val cycle = MonthPeriod(startMillis = 0, endMillis = 1_000, key = "2026-10")
        // Pass de 25 Go, l'opérateur annonce 17 Go restants : 8 Go partis, puis 1 Go mesuré depuis.
        val quota = DataQuota(25 * gb, balance = OperatorBalance(remainingBytes = 17 * gb, atMillis = 500))
        val used = GetQuotaUsedBytesUseCase(FakeQuotaRepository(quota), CycleUsageRepository())(cycle).getOrThrow()
        assertEquals(9 * gb, used)
        assertEquals(16 * gb, QuotaStatus(quota, used!!).remainingBytes)

        // Un solde d'un cycle précédent ne vaut plus : retour au décompte depuis le début du cycle.
        val nextCycle = cycle.copy(startMillis = 600, key = "2026-11")
        assertEquals(42_800_000_000L, GetQuotaUsedBytesUseCase(FakeQuotaRepository(quota), CycleUsageRepository())(nextCycle).getOrThrow())
    }

    @Test
    fun `operator balance is kept when the plan still holds it and capped to the plan`() = runTest {
        val repository = FakeQuotaRepository(DataQuota(25 * gb))
        SetOperatorBalanceUseCase(repository)(30 * gb, nowMillis = 42)
        assertEquals(OperatorBalance(25 * gb, 42), repository.quota.value?.balance)

        val scheduler = object : QuotaMonitorScheduler {
            override fun start() {}
            override fun stop() {}
        }
        SetQuotaUseCase(repository, scheduler)(30 * gb)
        assertEquals(OperatorBalance(25 * gb, 42), repository.quota.value?.balance)
        SetQuotaUseCase(repository, scheduler)(20 * gb)
        assertNull(repository.quota.value?.balance)
    }

    @Test
    fun `parses balance input between zero and the plan`() {
        assertEquals(17 * gb, QuotaViewModel.parseBalance("17", 25 * gb))
        assertEquals(0L, QuotaViewModel.parseBalance("0", 25 * gb))
        assertNull(QuotaViewModel.parseBalance("26", 25 * gb))
        assertNull(QuotaViewModel.parseBalance("-1", 25 * gb))
    }

    @Test
    fun `clears a balance relevé lors d'un cycle précédent, garde celui du cycle courant`() = runTest {
        // Solde relevé à l'époque 0, bien avant le début du cycle courant.
        val stale = FakeQuotaRepository(DataQuota(10 * gb, balance = OperatorBalance(5 * gb, atMillis = 0L)))
        assertTrue(ClearStaleBalanceUseCase(stale, GetCurrentPeriodUseCase(stale))())
        assertNull(stale.quota.value?.balance)

        val fresh = FakeQuotaRepository(DataQuota(10 * gb, balance = OperatorBalance(5 * gb, atMillis = System.currentTimeMillis())))
        assertFalse(ClearStaleBalanceUseCase(fresh, GetCurrentPeriodUseCase(fresh))())
        assertNotNull(fresh.quota.value?.balance)
    }

    @Test
    fun `reminds once the balance is older than the interval, then not again`() = runTest {
        val day = 24L * 60 * 60 * 1000
        val now = 100 * day
        val notifier = FakeNotifier()
        val repo = FakeQuotaRepository(DataQuota(10 * gb, balance = OperatorBalance(5 * gb, atMillis = now - 8 * day)))
        val useCase = RemindBalanceRecalibrationUseCase(repo, notifier)

        assertTrue(useCase(nowMillis = now))
        assertEquals(1, notifier.recalibrations.size)
        assertEquals(now, repo.balanceReminderAt.value)
        // Déjà rappelé dans l'intervalle : pas de second rappel.
        assertFalse(useCase(nowMillis = now + day))
    }

    @Test
    fun `does not remind a fresh balance nor without balance`() = runTest {
        val day = 24L * 60 * 60 * 1000
        val notifier = FakeNotifier()
        val fresh = FakeQuotaRepository(DataQuota(10 * gb, balance = OperatorBalance(5 * gb, atMillis = 100 * day)))
        assertFalse(RemindBalanceRecalibrationUseCase(fresh, notifier)(nowMillis = 100 * day + day))
        assertFalse(RemindBalanceRecalibrationUseCase(FakeQuotaRepository(DataQuota(10 * gb)), notifier)(nowMillis = 100 * day))
        assertTrue(notifier.recalibrations.isEmpty())
    }

    @Test
    fun `reminds to renew the plan two days before its end, once per cycle`() = runTest {
        val day = 24L * 60 * 60 * 1000
        val notifier = FakeNotifier()
        val repo = FakeQuotaRepository(DataQuota(25 * gb))
        val useCase = RemindPlanRenewalUseCase(repo, GetCurrentPeriodUseCase(repo), notifier)
        val renewal = 30 * day
        fun cycleAt(now: Long, key: String = "2026-10") = MonthPeriod(0, endMillis = now, key = key, nextStartMillis = renewal)

        assertFalse(useCase(cycleAt(renewal - 3 * day)))
        assertTrue(useCase(cycleAt(renewal - 2 * day)))
        assertEquals(listOf(renewal), notifier.renewals)
        assertFalse(useCase(cycleAt(renewal - day)))
        // Nouveau cycle : nouveau rappel possible.
        assertTrue(useCase(cycleAt(renewal - day, key = "2026-11")))
        // Sans forfait, rien à recharger.
        assertFalse(RemindPlanRenewalUseCase(FakeQuotaRepository(null), GetCurrentPeriodUseCase(repo), notifier)(cycleAt(renewal - day)))
    }

    @Test
    fun `flags the first app eating half of the plan`() {
        val quota = DataQuota(20 * gb)
        val system = AppDataUsage(AppKind.SYSTEM, null, null, 15 * gb)
        val chrome = AppDataUsage(AppKind.APP, "com.android.chrome", "Chrome", 10 * gb)
        val instagram = AppDataUsage(AppKind.APP, "com.instagram.android", "Instagram", 9 * gb)
        assertEquals(chrome, listOf(system, chrome, instagram).heavyAppFor(quota))
        assertNull(listOf(system, instagram).heavyAppFor(quota))
        assertNull(listOf(chrome).heavyAppFor(null))
    }

    private val noopScheduler = object : QuotaMonitorScheduler {
        override fun start() = Unit
        override fun stop() = Unit
    }

    private val ooredoo = MobileOperator("Ooredoo", "60503")
    private val tunisieTelecom = MobileOperator("Tunisie Telecom", "60502")

    /** SIM des données, que le test change comme on change de carte. */
    private class FakeOperatorRepository(operator: MobileOperator?) : OperatorRepository {
        val operator = MutableStateFlow(operator)
        override fun dataOperator() = operator.value
        override fun observeDataOperator() = operator
        override fun hasPhoneStatePermission() = false
        override fun observeDataNetworkGeneration() = flowOf<NetworkGeneration?>(null)
    }

    private fun completePlanSetup(repository: QuotaRepository, operators: OperatorRepository) = CompletePlanSetupUseCase(
        repository,
        operators,
        SetQuotaUseCase(repository, noopScheduler),
        SetPlanCycleUseCase(repository, SetCycleStartDayUseCase(repository)),
        SetOperatorBalanceUseCase(repository),
        SetPlanPriceUseCase(repository),
        RememberPlanUseCase(repository),
    )

    /** 21 octobre 2026 à minuit, jour d'achat des forfaits de ces tests. */
    private val oct21 = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 21) }.timeInMillis

    @Test
    fun `plan setup stores the plan, the operator balance, the cycle and the price at once`() = runTest {
        val repository = FakeQuotaRepository(null)
        val operators = FakeOperatorRepository(ooredoo)
        assertEquals(PlanStatus.MISSING, ObservePlanStatusUseCase(repository, operators)().first())

        // Abonnement de 25 Go à 28 DT renouvelé le 21 de chaque mois, l'opérateur annonce 19 Go restants.
        completePlanSetup(repository, operators)(
            25 * gb, purchaseMillis = oct21, validityDays = null, remainingBytes = 19 * gb, planPriceMillimes = 28_000,
            nowMillis = 42,
        )

        val quota = repository.quota.value!!
        assertEquals(25 * gb, quota.limitBytes)
        assertEquals(OperatorBalance(19 * gb, atMillis = 42), quota.balance)
        assertEquals(21, repository.cycleStartDay.value)
        assertEquals(28_000L, quota.priceMillimes)
        assertEquals(PricePerGb(1_120), quota.pricePerGb)
        assertEquals("60503", repository.planOperatorKey.value)
        assertNull(repository.planValidity.value)
        assertEquals(listOf(SavedPlan("60503", 25 * gb, 28_000, validityDays = null)), repository.savedPlans.value)
        assertEquals(PlanStatus.CONFIGURED, ObservePlanStatusUseCase(repository, operators)().first())
    }

    @Test
    fun `dashboard is reachable without a plan once the setup is skipped, or with a plan set earlier`() = runTest {
        val skipped = FakeQuotaRepository(null)
        val operators = FakeOperatorRepository(ooredoo)
        SkipPlanSetupUseCase(skipped, operators, SetQuotaUseCase(skipped, noopScheduler))()
        assertEquals(PlanStatus.CONFIGURED, ObservePlanStatusUseCase(skipped, operators)().first())

        // Quota d'avant le suivi de la SIM : rattaché à la SIM du moment, sans redemander le forfait.
        val legacy = FakeQuotaRepository(DataQuota(10 * gb))
        assertEquals(PlanStatus.CONFIGURED, ObservePlanStatusUseCase(legacy, operators)().first())
        assertEquals("60503", legacy.planOperatorKey.value)
    }

    @Test
    fun `switching the data SIM to another operator asks for its plan again`() = runTest {
        val repository = FakeQuotaRepository(null)
        val operators = FakeOperatorRepository(ooredoo)
        completePlanSetup(repository, operators)(25 * gb, oct21, validityDays = 30, remainingBytes = null, planPriceMillimes = null)
        val status = ObservePlanStatusUseCase(repository, operators)

        operators.operator.value = tunisieTelecom
        assertEquals(PlanStatus.OPERATOR_CHANGED, status().first())

        // Sans SIM prête (mode avion, redémarrage), le forfait n'est pas redemandé pour rien.
        operators.operator.value = null
        assertEquals(PlanStatus.CONFIGURED, status().first())

        operators.operator.value = tunisieTelecom
        completePlanSetup(repository, operators)(10 * gb, oct21, validityDays = 30, remainingBytes = null, planPriceMillimes = null)
        assertEquals(10 * gb, repository.quota.value!!.limitBytes)
        assertEquals(PlanStatus.CONFIGURED, status().first())

        // Retour sur Ooredoo : le forfait Tunisie Telecom ne vaut pas pour elle non plus.
        operators.operator.value = ooredoo
        assertEquals(PlanStatus.OPERATOR_CHANGED, status().first())
    }

    @Test
    fun `skipping after a SIM change drops the previous operator's plan`() = runTest {
        val repository = FakeQuotaRepository(DataQuota(25 * gb))
        repository.planOperatorKey.value = "60503"
        val operators = FakeOperatorRepository(tunisieTelecom)

        SkipPlanSetupUseCase(repository, operators, SetQuotaUseCase(repository, noopScheduler))()

        assertNull(repository.quota.value)
        assertEquals(PlanStatus.CONFIGURED, ObservePlanStatusUseCase(repository, operators)().first())
    }

    @Test
    fun `each plan keeps its own price, which does not follow a change of size`() = runTest {
        val repository = FakeQuotaRepository(null)
        val setQuota = SetQuotaUseCase(repository, noopScheduler)
        // Pass de 1,4 Go à 4,8 DT : 3,429 DT le Go.
        setQuota(1_400_000_000L)
        SetPlanPriceUseCase(repository)(4_800)
        assertEquals(PricePerGb(3_429), repository.quota.value!!.pricePerGb)

        // Même forfait réenregistré : le prix reste.
        setQuota(1_400_000_000L)
        assertEquals(4_800L, repository.quota.value!!.priceMillimes)

        // Passage au pass de 25 Go : l'ancien prix ne vaut pas, il est à ressaisir.
        setQuota(25 * gb)
        assertNull(repository.quota.value!!.priceMillimes)
        assertNull(repository.quota.value!!.pricePerGb)
    }

    @Test
    fun `parses quota input in gigabytes`() {
        assertEquals(2_500_000_000L, QuotaViewModel.parseGigabytes("2,5"))
        assertEquals(5 * gb, QuotaViewModel.parseGigabytes(" 5 "))
        assertNull(QuotaViewModel.parseGigabytes("0"))
        assertNull(QuotaViewModel.parseGigabytes("abc"))
        assertEquals("2.5", QuotaViewModel.bytesToInput(2_500_000_000L))
    }
}
