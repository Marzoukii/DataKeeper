package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.MainDispatcherRule
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** Recopie [source] dans l'état ; chaque intention envoie un effet. */
    private class EchoViewModel(private val source: Flow<Int>) : MviViewModel<Int, String, String>(0) {
        override fun observe(): List<Flow<*>> = listOf(source.onEach { value -> setState { value } })
        override fun onIntent(intent: String) = sendEffect(intent)
    }

    @Test
    fun `les flux ne tournent que tant que l'écran collecte l'état, puis 5 s de plus`() = runTest(main.dispatcher) {
        val source = MutableStateFlow(1)
        val viewModel = EchoViewModel(source)
        assertEquals(0, source.subscriptionCount.value)

        val screen = backgroundScope.launch { viewModel.state.collect {} }
        assertEquals(1, source.subscriptionCount.value)
        assertEquals(1, viewModel.state.value)
        source.value = 2
        assertEquals(2, viewModel.state.value)

        // Écran quitté : une rotation (moins de 5 s) ne coupe rien…
        screen.cancel()
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, source.subscriptionCount.value)
        // … au-delà, le flux est débranché.
        advanceTimeBy(2)
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)
    }

    @Test
    fun `un retour sur l'écran rebranche les flux et garde l'état`() = runTest(main.dispatcher) {
        val source = MutableStateFlow(1)
        val viewModel = EchoViewModel(source)
        backgroundScope.launch { viewModel.state.collect {} }.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)

        source.value = 3
        backgroundScope.launch { viewModel.state.collect {} }
        assertEquals(1, source.subscriptionCount.value)
        assertEquals(3, viewModel.state.value)
    }

    @Test
    fun `un effet envoyé sans écran est livré quand l'écran revient`() = runTest(main.dispatcher) {
        val viewModel = EchoViewModel(MutableStateFlow(0))
        viewModel.onIntent("snackbar")
        assertEquals("snackbar", viewModel.effects.first())
    }
}
