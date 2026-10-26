package com.marzouki.datasaver.domain

import com.marzouki.datasaver.data.speedtest.SpeedTestHistoryCodec
import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.enums.SpeedTestPhase
import com.marzouki.datasaver.domain.enums.SpeedTestRating
import com.marzouki.datasaver.domain.model.SpeedTestEvent
import com.marzouki.datasaver.domain.model.SpeedTestProgress
import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.model.SpeedTestServer
import com.marzouki.datasaver.domain.repository.SpeedTestRepository
import com.marzouki.datasaver.domain.usecase.RunSpeedTestUseCase
import com.marzouki.datasaver.ui.components.formatMbps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val server = SpeedTestServer("Agence Tunisienne d'Internet", "Kasbah")

class SpeedTestTest {

    private val wifiResult = SpeedTestResult(48.6, 12.3, 18, 1_000L, onWifi = true, server = server)
    private val mobileResult = SpeedTestResult(
        82.7, 24.0, 32, 2_000L, onWifi = false, operatorName = "Ooredoo", generation = NetworkGeneration.G5,
    )

    @Test
    fun `l'historique se relit tel qu'il a été écrit`() {
        val history = listOf(mobileResult, wifiResult)
        assertEquals(history, SpeedTestHistoryCodec.decode(SpeedTestHistoryCodec.encode(history)))
    }

    @Test
    fun `une ligne illisible de l'historique est ignorée`() {
        val encoded = SpeedTestHistoryCodec.encode(listOf(wifiResult)) + "\nabc|déchet"
        assertEquals(listOf(wifiResult), SpeedTestHistoryCodec.decode(encoded))
        assertTrue(SpeedTestHistoryCodec.decode(null).isEmpty())
    }

    @Test
    fun `les séparateurs sont retirés des noms`() {
        val odd = wifiResult.copy(server = SpeedTestServer("Ooredoo|5G", "Sfax\nNord"))
        assertEquals(SpeedTestServer("Ooredoo 5G", "Sfax Nord"), SpeedTestHistoryCodec.decode(SpeedTestHistoryCodec.encode(listOf(odd))).single().server)
    }

    @Test
    fun `l'appréciation suit le débit descendant`() {
        assertEquals(SpeedTestRating.EXCELLENT, SpeedTestRating.from(48.6))
        assertEquals(SpeedTestRating.GOOD, SpeedTestRating.from(10.0))
        assertEquals(SpeedTestRating.FAIR, SpeedTestRating.from(4.2))
        assertEquals(SpeedTestRating.SLOW, SpeedTestRating.from(0.8))
    }

    @Test
    fun `le débit s'écrit à la française`() {
        assertEquals("48,6", formatMbps(48.62))
        assertEquals("0,0", formatMbps(-1.0))
        assertEquals("312", formatMbps(312.4))
    }

    @Test
    fun `en données mobiles le test est plafonné et enregistré avec le réseau`() = runTest {
        val repository = FakeSpeedTestRepository()
        val events = RunSpeedTestUseCase(repository)(onWifi = false, "Ooredoo", NetworkGeneration.G4) { 5_000L }.toList()

        assertEquals(RunSpeedTestUseCase.MOBILE_MAX_BYTES_PER_PHASE, repository.requestedMaxBytes)
        val finished = events.last() as SpeedTestEvent.Finished
        assertEquals(SpeedTestResult(50.0, 10.0, 20, 5_000L, false, "Ooredoo", NetworkGeneration.G4, server), finished.result)
        assertEquals(listOf(finished.result), repository.history.value)
    }

    @Test
    fun `en Wi-Fi le test n'est pas plafonné ni rattaché à l'opérateur`() = runTest {
        val repository = FakeSpeedTestRepository()
        val events = RunSpeedTestUseCase(repository)(onWifi = true, "Ooredoo", NetworkGeneration.G4) { 5_000L }.toList()

        assertEquals(null, repository.requestedMaxBytes)
        val result = (events.last() as SpeedTestEvent.Finished).result
        assertEquals(null, result.operatorName)
        assertEquals(null, result.generation)
    }

    private class FakeSpeedTestRepository : SpeedTestRepository {
        override val history = MutableStateFlow<List<SpeedTestResult>>(emptyList())
        var requestedMaxBytes: Long? = -1L

        override suspend fun addToHistory(result: SpeedTestResult) {
            history.value = listOf(result) + history.value
        }

        override fun measure(maxBytesPerPhase: Long?): Flow<SpeedTestProgress> {
            requestedMaxBytes = maxBytesPerPhase
            return flowOf(
                SpeedTestProgress(SpeedTestPhase.PING, 0f),
                SpeedTestProgress(SpeedTestPhase.DOWNLOAD, 0.1f, pingMs = 20, server = server),
                SpeedTestProgress(SpeedTestPhase.UPLOAD, 1f, 10.0, 20, 50.0, 10.0, server),
            )
        }
    }
}
