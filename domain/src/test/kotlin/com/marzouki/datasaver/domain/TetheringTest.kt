package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringAlert.Companion.GB
import com.marzouki.datasaver.domain.model.TetheringSample
import com.marzouki.datasaver.domain.model.TetheringSession
import com.marzouki.datasaver.domain.model.TetheringUsage
import com.marzouki.datasaver.domain.model.bytesPerSecond
import com.marzouki.datasaver.domain.model.since
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TetheringTest {

    private val hour = 60 * 60 * 1000L

    private fun usage(usedBytes: Long, elapsedMillis: Long) =
        TetheringUsage(TetheringSession(startMillis = 0, startKnown = true, baselineBytes = 0), usedBytes, nowMillis = elapsedMillis)

    @Test
    fun `rythme moyen depuis le début du partage`() {
        assertEquals(240_000_000L, usage(1_800_000_000, elapsedMillis = 7 * hour + hour / 2).averageBytesPerHour)
    }

    @Test
    fun `pas de rythme moyen les deux premières minutes ni sans conso`() {
        assertNull(usage(50_000_000, elapsedMillis = 90_000).averageBytesPerHour)
        assertNull(usage(0, elapsedMillis = hour).averageBytesPerHour)
    }

    @Test
    fun `prochain palier, le seuil d'alerte tant qu'il est devant`() {
        val alert = TetheringAlert(enabled = true, thresholdBytes = 2 * GB)
        assertEquals(2 * GB, usage(1_800_000_000, hour).nextTargetBytes(alert))
        assertEquals(3 * GB, usage(2_100_000_000, hour).nextTargetBytes(alert))
        assertEquals(2 * GB, usage(1_800_000_000, hour).nextTargetBytes(alert.copy(enabled = false)))
        assertEquals(2 * GB, usage(GB, hour).nextTargetBytes(alert.copy(enabled = false)))
    }

    @Test
    fun `temps restant avant le palier au rythme donné`() {
        // Il manque 1 Go à 500 Mo/h : 2 h.
        assertEquals(2 * hour, usage(GB, hour).millisToReach(2 * GB, bytesPerHour = GB / 2))
        assertNull(usage(GB, hour).millisToReach(2 * GB, bytesPerHour = null))
        assertNull(usage(GB, hour).millisToReach(2 * GB, bytesPerHour = 0))
    }

    @Test
    fun `débit entre deux relevés, sur les dernières secondes`() {
        val samples = listOf(
            TetheringSample(atMillis = 0, usedBytes = 0),
            TetheringSample(atMillis = 50_000, usedBytes = 10_000_000),
            TetheringSample(atMillis = 60_000, usedBytes = 22_000_000),
        )
        assertEquals(366_666L, samples.bytesPerSecond())
        assertEquals(1_200_000L, samples.since(nowMillis = 60_000, windowMillis = 10_000).bytesPerSecond())
        assertNull(samples.take(1).bytesPerSecond())
    }
}
