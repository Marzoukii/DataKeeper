package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.MainDispatcherRule
import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.usecase.GetCurrentPeriodUseCase
import com.marzouki.datasaver.domain.usecase.GetDailyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetMobileCounterUseCase
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetQuotaUsedBytesUseCase
import com.marzouki.datasaver.domain.usecase.GetTetheringUsageUseCase
import com.marzouki.datasaver.domain.usecase.GetTopMobileAppsUseCase
import com.marzouki.datasaver.domain.usecase.HasPhoneStatePermissionUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataNetworkGenerationUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveHotspotActiveUseCase
import com.marzouki.datasaver.domain.usecase.ObservePricePerGbUseCase
import com.marzouki.datasaver.domain.usecase.ObserveQuotaUseCase
import com.marzouki.datasaver.domain.usecase.ObserveOperatorBalanceReadingUseCase
import com.marzouki.datasaver.domain.usecase.ApplyOperatorBalanceReadingUseCase
import com.marzouki.datasaver.domain.usecase.DismissOperatorBalanceReadingUseCase
import com.marzouki.datasaver.domain.usecase.SetOperatorBalanceUseCase
import com.marzouki.datasaver.presentation.usage.UsageIntent
import com.marzouki.datasaver.presentation.usage.UsageViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UsageViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val quotas = FakeQuotaRepository()
    private val usage = FakeUsageRepository(mobileBytes = 3 * GB)
    private val operators = FakeOperatorRepository()
    private val tethering = FakeTetheringRepository()
    private val clock = FakeClock()

    private fun viewModel() = UsageViewModel(
        observeQuota = ObserveQuotaUseCase(quotas),
        observePricePerGb = ObservePricePerGbUseCase(quotas),
        hasUsageAccess = HasUsageAccessUseCase(usage),
        getCurrentPeriod = GetCurrentPeriodUseCase(quotas, clock),
        getMonthlyUsage = GetMonthlyUsageUseCase(usage),
        getDailyUsage = GetDailyUsageUseCase(usage, clock),
        getTopMobileApps = GetTopMobileAppsUseCase(usage),
        getQuotaUsedBytes = GetQuotaUsedBytesUseCase(quotas, usage),
        observeDataOperator = ObserveDataOperatorUseCase(operators),
        hasPhoneStatePermission = HasPhoneStatePermissionUseCase(operators),
        observeDataNetworkGeneration = ObserveDataNetworkGenerationUseCase(operators),
        observeHotspotActive = ObserveHotspotActiveUseCase(tethering),
        getTetheringUsage = GetTetheringUsageUseCase(tethering, clock),
        getMobileCounter = GetMobileCounterUseCase(tethering),
        observeOperatorBalanceReading = ObserveOperatorBalanceReadingUseCase(quotas),
        applyOperatorBalanceReading = ApplyOperatorBalanceReadingUseCase(quotas, SetOperatorBalanceUseCase(quotas, clock)),
        dismissOperatorBalanceReading = DismissOperatorBalanceReadingUseCase(quotas),
    )

    @Test
    fun `la 4G ou 5G n'est suivie qu'une fois la permission accordée`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        assertFalse(viewModel.state.value.hasPhonePermission)
        assertEquals(0, operators.generation.subscriptionCount.value)
        assertNull(viewModel.state.value.networkGeneration)

        operators.permission = true
        viewModel.onIntent(UsageIntent.PhonePermissionResult)

        assertTrue(viewModel.state.value.hasPhonePermission)
        assertEquals(1, operators.generation.subscriptionCount.value)
        assertEquals(NetworkGeneration.G5, viewModel.state.value.networkGeneration)
    }

    @Test
    fun `la carte du partage suit le point d'accès`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        assertFalse(viewModel.state.value.hotspotActive)
        assertNull(viewModel.state.value.tethering)

        tethering.counterBytes = 400_000_000
        tethering.hotspot.value = true
        assertTrue(viewModel.state.value.hotspotActive)
        assertEquals(400_000_000L, viewModel.state.value.tethering?.usedBytes)

        tethering.hotspot.value = false
        assertFalse(viewModel.state.value.hotspotActive)
        assertNull(viewModel.state.value.tethering)
    }

    @Test
    fun `le relevé lit le cycle de l'horloge injectée`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(UsageIntent.Refresh)

        val state = viewModel.state.value
        assertEquals("2026-10", state.period?.key)
        assertNotNull(state.usage)
        assertEquals(3 * GB, state.usage?.mobileBytes)
    }

    @Test
    fun `pendant un partage, le total et le quota du cycle suivent le compteur en direct`() = runTest(main.dispatcher) {
        quotas.quota.value = DataQuota(25 * GB)
        tethering.counterBytes = 10 * GB
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(UsageIntent.Refresh)
        assertEquals(3 * GB, viewModel.state.value.liveUsage?.mobileBytes)

        // L'historique d'Android n'a pas bougé ; le compteur du téléphone, si.
        tethering.hotspot.value = true
        tethering.counterBytes = 10 * GB + 500_000_000
        advanceTimeBy(2_001)

        assertEquals(3 * GB + 500_000_000, viewModel.state.value.liveUsage?.mobileBytes)
        assertEquals(3 * GB + 500_000_000, viewModel.state.value.quotaStatus?.usedBytes)
    }
}
