package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.ForecastOutlook
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaForecast
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
        // (4 750 Mo - 3 200 Mo) / 11 jours.
        assertEquals(1_550 * mb / 11, forecast.safeDailyBytes)
    }

    @Test
    fun `comfortable under the alert threshold`() {
        assertEquals(ForecastOutlook.COMFORTABLE, forecast(100).outlook)
    }

    @Test
    fun `over quota when the projection exceeds the plan, advice just keeps within it`() {
        val forecast = forecast(200)
        assertEquals(ForecastOutlook.OVER_QUOTA, forecast.outlook)
        // (5 000 Mo - 4 000 Mo) / 11 jours.
        assertEquals(1_000 * mb / 11, forecast.safeDailyBytes)
    }

    @Test
    fun `exceeded once the plan is used up, with nothing left per day`() {
        val forecast = forecast(260)
        assertEquals(ForecastOutlook.EXCEEDED, forecast.outlook)
        assertEquals(0L, forecast.safeDailyBytes)
    }
}
