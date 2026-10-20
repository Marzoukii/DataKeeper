package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.domain.model.MonthPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class HomeSummaryTest {

    private val mb = 1_000_000L
    private val period = MonthPeriod.current(
        Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 15, 0) }.timeInMillis,
    )

    @Test
    fun `projects mobile usage linearly to the end of the cycle`() {
        // 3,2 Go en 20 jours sur un cycle de 31 jours → 4,96 Go.
        val summary = HomeSummary(period, true, DataUsage(0, 3_200 * mb, period), DataQuota(5_000 * mb), 0)
        assertEquals(4_960 * mb, summary.projectedMobileBytes)
        assertEquals(3_200 * mb, summary.quotaStatus?.usedBytes)
    }

    @Test
    fun `no projection nor status without usage`() {
        val summary = HomeSummary(period, false, usage = null, quota = DataQuota(5_000 * mb), pendingDownloads = 3)
        assertNull(summary.projectedMobileBytes)
        assertNull(summary.quotaStatus)
    }
}
