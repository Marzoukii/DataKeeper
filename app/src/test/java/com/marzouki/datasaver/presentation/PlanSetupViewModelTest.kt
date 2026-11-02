package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.MainDispatcherRule
import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.usecase.CompletePlanSetupUseCase
import com.marzouki.datasaver.domain.usecase.ForgetSavedPlanUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveOperatorChangeUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.domain.usecase.ObserveSavedPlansUseCase
import com.marzouki.datasaver.domain.usecase.RememberPlanUseCase
import com.marzouki.datasaver.domain.usecase.SetCycleStartDayUseCase
import com.marzouki.datasaver.domain.usecase.SetOperatorBalanceUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanCycleUseCase
import com.marzouki.datasaver.domain.usecase.SetPlanPriceUseCase
import com.marzouki.datasaver.domain.usecase.SetQuotaUseCase
import com.marzouki.datasaver.domain.usecase.SkipPlanSetupUseCase
import com.marzouki.datasaver.presentation.onboarding.PlanSetupIntent
import com.marzouki.datasaver.presentation.onboarding.PlanSetupViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlanSetupViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** Forfait de 25 Go déjà saisi pour la SIM Ooredoo en place. */
    private val quotas = FakeQuotaRepository(DataQuota(25 * GB)).apply {
        planSetupDone.value = true
        planOperatorKey.value = ooredoo.key
    }
    private val operators = FakeOperatorRepository(ooredoo)
    private val clock = FakeClock()

    private fun viewModel(): PlanSetupViewModel {
        val setQuota = SetQuotaUseCase(quotas, NoopQuotaScheduler)
        return PlanSetupViewModel(
            observeDataOperator = ObserveDataOperatorUseCase(operators),
            observePlanStatus = ObservePlanStatusUseCase(quotas, operators),
            observeSavedPlans = ObserveSavedPlansUseCase(quotas, operators),
            completePlanSetup = CompletePlanSetupUseCase(
                quotas,
                operators,
                setQuota,
                SetPlanCycleUseCase(quotas, SetCycleStartDayUseCase(quotas)),
                SetOperatorBalanceUseCase(quotas, clock),
                SetPlanPriceUseCase(quotas),
                RememberPlanUseCase(quotas),
                clock,
            ),
            skipPlanSetup = SkipPlanSetupUseCase(quotas, operators, setQuota),
            forgetSavedPlan = ForgetSavedPlanUseCase(quotas),
            observeOperatorChange = ObserveOperatorChangeUseCase(quotas),
        )
    }

    @Test
    fun `une nouvelle SIM repart d'un formulaire vierge`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        assertEquals(PlanStatus.CONFIGURED, viewModel.state.value.planStatus)
        viewModel.onIntent(PlanSetupIntent.PlanChanged("10"))

        operators.operator.value = tunisieTelecom

        val state = viewModel.state.value
        assertEquals(PlanStatus.OPERATOR_CHANGED, state.planStatus)
        assertTrue(state.operatorChanged)
        assertEquals("", state.planInput)
    }

    @Test
    fun `la saisie en cours est gardée tant que le forfait reste le même`() = runTest(main.dispatcher) {
        quotas.planSetupDone.value = false
        quotas.quota.value = null
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(PlanSetupIntent.PlanChanged("10"))

        // Toujours pas de forfait, mais la SIM change : rien à effacer.
        operators.operator.value = tunisieTelecom

        val state = viewModel.state.value
        assertEquals(PlanStatus.MISSING, state.planStatus)
        assertFalse(state.operatorChanged)
        assertEquals("10", state.planInput)
    }
}
