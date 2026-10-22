package com.marzouki.datasaver.domain

import android.telephony.TelephonyManager
import com.marzouki.datasaver.data.operator.generationOf
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.CycleOutcome
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.NetworkGeneration
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import com.marzouki.datasaver.domain.usecase.GetQuotaStreakUseCase
import com.marzouki.datasaver.ui.components.formatDinars
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class SavingsTest {

    private val gb = 1_000_000_000L
    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 15, 0) }.timeInMillis

    @Test
    fun `parses dinar prices with comma or dot`() {
        assertEquals(2_000, PricePerGb.parse("2")?.millimes)
        assertEquals(1_500, PricePerGb.parse("1,5")?.millimes)
        assertEquals(1_250, PricePerGb.parse("1.250 DT")?.millimes)
        assertNull(PricePerGb.parse("0"))
        assertNull(PricePerGb.parse("101"))
        assertNull(PricePerGb.parse("abc"))
    }

    @Test
    fun `wifi savings are priced per gigabyte`() {
        // 18,4 Go × 1,5 DT = 27,6 DT.
        assertEquals(27_600L, PricePerGb(1_500).costMillimes(18_400_000_000L))
        assertEquals("27,6 DT", formatDinars(27_600))
        assertEquals("1,25 DT", formatDinars(1_250))
        assertEquals("2 DT", formatDinars(2_000))
    }

    @Test
    fun `previous cycle is complete and keeps the start day`() {
        val previous = MonthPeriod.current(now, cycleStartDay = 25).previous()
        assertEquals("2026-08", previous.key)
        assertEquals(31, previous.totalDays)
        assertEquals(previous.nextStartMillis, previous.endMillis)
    }

    @Test
    fun `streak counts cycles under quota until the first overrun`() = runTest {
        // Du dernier cycle terminé au plus ancien : sept., août, juillet sous le quota, juin dépassé.
        val mobileByCycle = mapOf("2026-09" to 3 * gb, "2026-08" to 4 * gb, "2026-07" to 2 * gb, "2026-06" to 6 * gb)
        val streak = GetQuotaStreakUseCase(FakeQuota(DataQuota(5 * gb)), FakeUsage(mobileByCycle))(MonthPeriod.current(now))
            .getOrThrow()!!
        assertEquals(3, streak.months)
        assertEquals(List(3) { CycleOutcome.WITHIN_QUOTA }, streak.recentOutcomes)
    }

    @Test
    fun `missing history stops the streak`() = runTest {
        val streak = GetQuotaStreakUseCase(FakeQuota(DataQuota(5 * gb)), FakeUsage(mapOf("2026-09" to 1 * gb)))(
            MonthPeriod.current(now),
        ).getOrThrow()!!
        assertEquals(1, streak.months)
        assertEquals(listOf(CycleOutcome.UNKNOWN, CycleOutcome.UNKNOWN, CycleOutcome.WITHIN_QUOTA), streak.recentOutcomes)
    }

    @Test
    fun `no streak without quota`() = runTest {
        assertNull(GetQuotaStreakUseCase(FakeQuota(null), FakeUsage(emptyMap()))(MonthPeriod.current(now)).getOrThrow())
    }

    @Test
    fun `recognises tunisian operators from the sim code`() {
        assertEquals("Ooredoo", MobileOperator.from("60503", "TN TUNISIANA")?.name)
        assertEquals("Tunisie Telecom", MobileOperator.from("60502", "")?.name)
        assertEquals("Orange", MobileOperator.from("60501", null)?.name)
        assertEquals("Free", MobileOperator.from("20815", "Free")?.name)
        assertNull(MobileOperator.from("", " "))
    }

    @Test
    fun `maps telephony network types to generations`() {
        assertEquals(NetworkGeneration.G2, generationOf(TelephonyManager.NETWORK_TYPE_EDGE))
        assertEquals(NetworkGeneration.G3, generationOf(TelephonyManager.NETWORK_TYPE_HSPAP))
        assertEquals(NetworkGeneration.G4, generationOf(TelephonyManager.NETWORK_TYPE_LTE))
        assertEquals(NetworkGeneration.G5, generationOf(TelephonyManager.NETWORK_TYPE_NR))
        assertNull(generationOf(TelephonyManager.NETWORK_TYPE_IWLAN))
        assertNull(generationOf(TelephonyManager.NETWORK_TYPE_UNKNOWN))
    }

    private class FakeQuota(quota: DataQuota?) : QuotaRepository {
        override val quota = MutableStateFlow(quota)
        override val lastAlertedMonth = MutableStateFlow<String?>(null)
        override val cycleStartDay = MutableStateFlow(1)
        override val pricePerGb = MutableStateFlow<PricePerGb?>(null)
        override suspend fun setQuota(quota: DataQuota?) { this.quota.value = quota }
        override suspend fun setLastAlertedMonth(monthKey: String?) { lastAlertedMonth.value = monthKey }
        override suspend fun setCycleStartDay(day: Int) { cycleStartDay.value = day }
        override suspend fun setPricePerGb(price: PricePerGb?) { pricePerGb.value = price }
        override val balanceReminderAt = MutableStateFlow<Long?>(null)
        override suspend fun setBalanceReminderAt(millis: Long?) { balanceReminderAt.value = millis }
    }

    /** Data mobile par cycle (clé du cycle) ; un cycle absent n'a aucun trafic. */
    private class FakeUsage(private val mobileByCycle: Map<String, Long>) : UsageRepository {
        override fun hasUsageAccess() = true
        override suspend fun getUsage(period: MonthPeriod) = DataUsage(0, mobileByCycle[period.key] ?: 0, period)
        override suspend fun getDailyUsage(days: Int, nowMillis: Long) = emptyList<DayUsage>()
        override suspend fun getTopMobileApps(period: MonthPeriod, limit: Int) = emptyList<AppDataUsage>()
    }
}
