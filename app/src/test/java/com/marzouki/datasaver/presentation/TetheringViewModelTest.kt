package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.MainDispatcherRule
import com.marzouki.datasaver.domain.model.TetheringSample
import com.marzouki.datasaver.domain.model.TetheringSession
import com.marzouki.datasaver.domain.model.TetheringUsage
import com.marzouki.datasaver.domain.usecase.GetTetheringUsageUseCase
import com.marzouki.datasaver.domain.usecase.HasPhoneStatePermissionUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataNetworkGenerationUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveHotspotActiveUseCase
import com.marzouki.datasaver.domain.usecase.ObserveTetheringAlertUseCase
import com.marzouki.datasaver.domain.usecase.SetTetheringAlertUseCase
import com.marzouki.datasaver.presentation.tethering.TetheringState
import com.marzouki.datasaver.presentation.tethering.TetheringViewModel
import com.marzouki.datasaver.presentation.tethering.withSample
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TetheringViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val minute = 60_000L
    private val session = TetheringSession(startMillis = 0, startKnown = true, baselineBytes = 0)
    private fun usage(usedBytes: Long, atMillis: Long) = TetheringUsage(session, usedBytes, atMillis)

    @Test
    fun `le rythme récent n'est donné qu'après une minute de relevés`() {
        val after30s = TetheringState().withSample(usage(0, 0), 0).withSample(usage(30_000_000, minute / 2), minute / 2)
        assertNull(after30s.recentBytesPerHour)

        // 60 Mo en une minute : 1 Mo/s, soit 3,6 Go par heure.
        val after1min = after30s.withSample(usage(60_000_000, minute), minute)
        assertEquals(3_600_000_000L, after1min.recentBytesPerHour)
    }

    @Test
    fun `seuls les relevés des 5 dernières minutes comptent`() {
        val state = (0..7).fold(TetheringState()) { state, i -> state.withSample(usage(i * GB, i * minute), i * minute) }
        assertEquals((2..7).map { TetheringSample(it * minute, it * GB) }, state.samples)
    }

    @Test
    fun `un compteur reparti de zéro oublie les anciens relevés`() {
        val state = TetheringState()
            .withSample(usage(5 * GB, 0), 0)
            .withSample(usage(GB, minute), minute)
        assertEquals(listOf(TetheringSample(minute, GB)), state.samples)
    }

    @Test
    fun `les relevés sont datés par l'horloge injectée et oubliés quand le partage s'éteint`() = runTest(main.dispatcher) {
        val tethering = FakeTetheringRepository().apply { counterBytes = 700_000_000 }
        val operators = FakeOperatorRepository()
        val clock = FakeClock(nowMillis = 42_000)
        val viewModel = TetheringViewModel(
            observeHotspotActive = ObserveHotspotActiveUseCase(tethering),
            observeAlert = ObserveTetheringAlertUseCase(tethering),
            observeDataOperator = ObserveDataOperatorUseCase(operators),
            observeDataNetworkGeneration = ObserveDataNetworkGenerationUseCase(operators),
            hasPhoneStatePermission = HasPhoneStatePermissionUseCase(operators),
            getTetheringUsage = GetTetheringUsageUseCase(tethering, clock),
            setAlert = SetTetheringAlertUseCase(tethering, NoopTetheringScheduler),
            clock = clock,
        )
        backgroundScope.launch { viewModel.state.collect {} }

        tethering.hotspot.value = true
        assertEquals(listOf(TetheringSample(42_000, 700_000_000)), viewModel.state.value.samples)

        tethering.hotspot.value = false
        assertEquals(emptyList<TetheringSample>(), viewModel.state.value.samples)
        assertNull(viewModel.state.value.usage)
    }
}
