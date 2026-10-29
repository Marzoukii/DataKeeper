package com.marzouki.datasaver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Remplace le fil principal (celui de viewModelScope) par un dispatcher de test : les coroutines
 * des ViewModels s'exécutent tout de suite, et les délais avancent en temps virtuel.
 * Passer [dispatcher] à runTest pour partager la même horloge virtuelle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
