package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.ForecastOutlook
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaForecast
import com.marzouki.datasaver.domain.model.alignDailyToUsed
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class QuotaForecastTest {

    private val mb = 1_000_000L
    // Jour 20 d'un cycle de 31 jours : 11 jours restants.
    private val period = MonthPeriod.current(
        Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 15, 0) }.timeInMillis,
    )
    private val quota = DataQuota(5_000 * mb)

    private fun forecast(mbPerDay: Long) = QuotaForecast(quota, period, List(20) { mbPerDay * mb })

    @Test
    fun `accumulates daily usage and projects it to the end of the cycle`() {
        val forecast = forecast(160)
        assertEquals(160 * mb, forecast.cumulativeBytes.first())
        assertEquals(3_200 * mb, forecast.usedBytes)
        assertEquals(4_960 * mb, forecast.projectedBytes)
    }

    @Test
    fun `tight between the alert threshold and the quota, advice keeps a 5 percent margin`() {
        val forecast = forecast(160)
        assertEquals(ForecastOutlook.TIGHT, forecast.outlook)
        // (4 750 Mo - 3 040 Mo consommés avant aujourd'hui) / 12 jours, aujourd'hui compris.
        assertEquals(1_710 * mb / 12, forecast.safeDailyBytes)
    }

    @Test
    fun `comfortable under the alert threshold`() {
        assertEquals(ForecastOutlook.COMFORTABLE, forecast(100).outlook)
    }

    @Test
    fun `over quota when the projection exceeds the plan, advice just keeps within it`() {
        val forecast = forecast(200)
        assertEquals(ForecastOutlook.OVER_QUOTA, forecast.outlook)
        // (5 000 Mo - 3 800 Mo consommés avant aujourd'hui) / 12 jours, comme le budget du jour.
        assertEquals(100 * mb, forecast.safeDailyBytes)
        assertEquals(100 * mb, forecast.dailyBudgetBytes)
    }

    @Test
    fun `exceeded once the plan is used up, with nothing left per day`() {
        val forecast = forecast(260)
        assertEquals(ForecastOutlook.EXCEEDED, forecast.outlook)
        assertEquals(0L, forecast.safeDailyBytes)
    }

    @Test
    fun `a recalibrated balance is not extrapolated as a consumption rate`() {
        // Cycle du 21 oct. au 20 nov., 3ᵉ jour : forfait de 25 Go recalé à 16 Go restants, donc
        // 9 Go décomptés dont 8 Go partis avant le relevé, et 1,5 Go réellement mesuré en 3 jours.
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 23, 9, 55) }.timeInMillis
        val cycle = MonthPeriod.current(now, cycleStartDay = 21)
        val forecast = QuotaForecast(
            DataQuota(25_000 * mb),
            cycle,
            dailyMobileBytes = listOf(8_000 * mb, 600 * mb, 400 * mb),
            measuredDailyBytes = listOf(500 * mb, 600 * mb, 400 * mb),
        )
        // 9 Go + 500 Mo/jour × 28 jours = 23 Go, et non 9 Go × 31 / 3 = 93 Go.
        assertEquals(23_000 * mb, forecast.projectedBytes)
        assertEquals(ForecastOutlook.TIGHT, forecast.outlook)
        // Ce matin il restait 25 - 8,6 = 16,4 Go pour 29 jours, aujourd'hui compris.
        assertEquals(400 * mb, forecast.todayBytes)
        assertEquals(16_400 * mb / 29, forecast.dailyBudgetBytes)
    }

    @Test
    fun `operator balance rescales the days before the reading so the curve ends on the plan usage`() {
        // 3 Go mesurés avant le relevé du jour 3 alors que l'opérateur n'en décompte que 1,5.
        val aligned = alignDailyToUsed(listOf(1_000 * mb, 2_000 * mb, 500 * mb, 300 * mb), 2_300 * mb, fromDayIndex = 2)
        assertEquals(listOf(500 * mb, 1_000 * mb, 500 * mb, 300 * mb), aligned)
        assertEquals(2_300 * mb, aligned.sum())
    }

    @Test
    fun `operator balance with nothing measured before puts the gap on the first day`() {
        assertEquals(listOf(8_000 * mb, 0, 200 * mb), alignDailyToUsed(listOf(0, 0, 200 * mb), 8_200 * mb, fromDayIndex = 2))
    }

    @Test
    fun `operator balance read on the first day adjusts that day without going negative`() {
        assertEquals(listOf(0, 100 * mb), alignDailyToUsed(listOf(900 * mb, 100 * mb), 50 * mb, fromDayIndex = 0))
    }
}
