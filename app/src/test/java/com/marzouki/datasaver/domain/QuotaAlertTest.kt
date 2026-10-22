package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import com.marzouki.datasaver.domain.usecase.CheckQuotaAndAlertUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaUsedBytesUseCase
import com.marzouki.datasaver.domain.usecase.SetOperatorBalanceUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaUseCase
import com.marzouki.datasaver.presentation.quota.QuotaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `parses quota input in gigabytes`() {
        assertEquals(2_500_000_000L, QuotaViewModel.parseGigabytes("2,5"))
        assertEquals(5 * gb, QuotaViewModel.parseGigabytes(" 5 "))
        assertNull(QuotaViewModel.parseGigabytes("0"))
        assertNull(QuotaViewModel.parseGigabytes("abc"))
        assertEquals("2.5", QuotaViewModel.bytesToInput(2_500_000_000L))
    }
}
