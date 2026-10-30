package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.LiveMobileUsage
import com.marzouki.datasaver.domain.model.withHistory
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMobileUsageTest {

    private val cycle = 1_000L
    private val live = null.withHistory(cycle, historyBytes = 1_000, counterBytes = 50_000)

    @Test
    fun `le compteur avance le total entre deux lectures de l'historique`() {
        assertEquals(0L, live.extraBytes)
        assertEquals(300L, live.withCounter(50_300).extraBytes)
    }

    @Test
    fun `un historique relu encore en retard ne fait pas reculer le total`() {
        val relu = live.withCounter(50_300).withHistory(cycle, historyBytes = 1_100, counterBytes = 50_300)
        assertEquals(1_300L, relu.historyBytes + relu.extraBytes)
    }

    @Test
    fun `un historique qui a rattrapé le compteur devient la nouvelle ancre`() {
        val relu = live.withCounter(50_300).withHistory(cycle, historyBytes = 1_400, counterBytes = 50_300)
        assertEquals(LiveMobileUsage.anchoredAt(cycle, 1_400, 50_300), relu)
        assertEquals(0L, relu.extraBytes)
    }

    @Test
    fun `nouveau cycle ou compteur reparti à zéro, nouvelle ancre`() {
        assertEquals(0L, live.withCounter(50_300).withHistory(2_000L, historyBytes = 10, counterBytes = 50_300).extraBytes)
        val redemarre = live.withCounter(50_300).withCounter(200)
        assertEquals(0L, redemarre.extraBytes)
        assertEquals(100L, redemarre.withCounter(300).extraBytes)
    }
}
