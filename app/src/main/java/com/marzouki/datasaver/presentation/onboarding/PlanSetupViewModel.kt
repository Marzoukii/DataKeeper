package com.marzouki.datasaver.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.usecase.CompletePlanSetupUseCase
import com.marzouki.datasaver.domain.usecase.ForgetSavedPlanUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.domain.usecase.ObserveSavedPlansUseCase
import com.marzouki.datasaver.domain.usecase.SkipPlanSetupUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import com.marzouki.datasaver.presentation.quota.QuotaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pas d'effet : une fois le forfait enregistré, la racine de l'appli affiche le tableau de bord. */
@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    observeDataOperator: ObserveDataOperatorUseCase,
    observePlanStatus: ObservePlanStatusUseCase,
    observeSavedPlans: ObserveSavedPlansUseCase,
    private val completePlanSetup: CompletePlanSetupUseCase,
    private val skipPlanSetup: SkipPlanSetupUseCase,
    private val forgetSavedPlan: ForgetSavedPlanUseCase,
) : MviViewModel<PlanSetupState, PlanSetupIntent, Nothing>(PlanSetupState()) {

    private var lastStatus: PlanStatus? = null

    init {
        // La SIM peut encore changer pendant la saisie : le titre suit l'opérateur du moment.
        observeDataOperator()
            .onEach { operator -> setState { copy(operatorName = operator?.name) } }
            .launchIn(viewModelScope)
        // Suit lui aussi la SIM : sur une carte Tunisie Telecom, jamais les forfaits Ooredoo.
        observeSavedPlans()
            .onEach { plans -> setState { copy(savedPlans = plans) } }
            .launchIn(viewModelScope)
        observePlanStatus()
            .onEach { status ->
                // Le ViewModel survit au tableau de bord : un changement de SIM ultérieur repart
                // d'un formulaire vierge, pas des chiffres (ni du « enregistrement… ») de l'ancienne.
                val reset = lastStatus == PlanStatus.CONFIGURED && status != PlanStatus.CONFIGURED
                lastStatus = status
                setState {
                    val form = if (reset) PlanSetupState(operatorName = operatorName, savedPlans = savedPlans) else this
                    form.copy(operatorChanged = status == PlanStatus.OPERATOR_CHANGED)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PlanSetupIntent) {
        when (intent) {
            is PlanSetupIntent.PlanChanged -> setState { copy(planInput = intent.value) }
            is PlanSetupIntent.PresetSelected -> setState { copy(planInput = intent.gigabytes.toString()) }
            is PlanSetupIntent.RemainingChanged -> setState { copy(remainingInput = intent.value) }
            is PlanSetupIntent.PriceChanged -> setState { copy(priceInput = intent.value) }
            is PlanSetupIntent.CycleChanged -> setState { copy(cycle = intent.cycle) }
            is PlanSetupIntent.SavedPlanSelected -> setState {
                val plan = intent.plan
                copy(
                    planInput = QuotaViewModel.bytesToInput(plan.limitBytes),
                    priceInput = plan.priceMillimes?.let(QuotaViewModel::millimesToInput).orEmpty(),
                    cycle = cycle.copy(
                        validityInput = plan.validityDays?.toString() ?: cycle.validityInput,
                        monthly = plan.validityDays == null,
                    ),
                )
            }
            is PlanSetupIntent.SavedPlanForgotten -> viewModelScope.launch { forgetSavedPlan(intent.plan) }
            PlanSetupIntent.Submit -> submit()
            PlanSetupIntent.Skip -> viewModelScope.launch { skipPlanSetup() }
        }
    }

    private fun submit() {
        val state = currentState
        val planBytes = state.planBytes
        if (!state.canSubmit || planBytes == null) {
            setState { copy(showErrors = true) }
            return
        }
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            // canSubmit garantit qu'un prix saisi donne un prix du Go valable, et une validité correcte.
            completePlanSetup(
                planBytes,
                purchaseMillis = state.cycle.purchaseMillis,
                validityDays = state.cycle.validityDays,
                remainingBytes = state.remainingBytes,
                planPriceMillimes = state.planPriceMillimes,
            )
        }
    }
}
