package com.marzouki.datasaver.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.usecase.CompletePlanSetupUseCase
import com.marzouki.datasaver.domain.usecase.ForgetSavedPlanUseCase
import com.marzouki.datasaver.domain.model.OperatorChange
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
import com.marzouki.datasaver.domain.usecase.ObserveOperatorChangeUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.domain.usecase.ObserveSavedPlansUseCase
import com.marzouki.datasaver.domain.usecase.SkipPlanSetupUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import com.marzouki.datasaver.presentation.quota.QuotaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pas d'effet : une fois le forfait enregistré, la racine de l'appli affiche le tableau de bord. */
@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val observeDataOperator: ObserveDataOperatorUseCase,
    private val observePlanStatus: ObservePlanStatusUseCase,
    private val observeSavedPlans: ObserveSavedPlansUseCase,
    private val completePlanSetup: CompletePlanSetupUseCase,
    private val skipPlanSetup: SkipPlanSetupUseCase,
    private val forgetSavedPlan: ForgetSavedPlanUseCase,
    private val observeOperatorChange: ObserveOperatorChangeUseCase,
) : MviViewModel<PlanSetupState, PlanSetupIntent, Nothing>(PlanSetupState()) {

    override fun observe(): List<Flow<*>> = listOf(
        // La SIM peut encore changer pendant la saisie : le titre suit l'opérateur du moment.
        observeDataOperator().onEach { operator -> setState { copy(operatorName = operator?.name) } },
        // Suit lui aussi la SIM : sur une carte Tunisie Telecom, jamais les forfaits Ooredoo.
        observeSavedPlans().onEach { plans -> setState { copy(savedPlans = plans).prefilled() } },
        // Nouvelle SIM : le forfait part de l'heure où elle a été vue, pas de celle de la saisie.
        observeOperatorChange().onEach { change -> setState { copy(operatorChange = change).prefilled() } },
        observePlanStatus().onEach { status ->
            setState {
                // Le ViewModel survit au tableau de bord : un changement de SIM ultérieur repart
                // d'un formulaire vierge, pas des chiffres (ni du « enregistrement… ») de l'ancienne.
                val reset = planStatus == PlanStatus.CONFIGURED && status != PlanStatus.CONFIGURED
                val form = if (reset) {
                    PlanSetupState(operatorName = operatorName, savedPlans = savedPlans, operatorChange = operatorChange)
                } else {
                    this
                }
                form.copy(planStatus = status, operatorChanged = status == PlanStatus.OPERATOR_CHANGED).prefilled()
            }
        },
    )

    override fun onIntent(intent: PlanSetupIntent) {
        when (intent) {
            is PlanSetupIntent.PlanChanged -> setState { copy(planInput = intent.value) }
            is PlanSetupIntent.PresetSelected -> setState { copy(planInput = intent.gigabytes.toString(), customPlan = false) }
            PlanSetupIntent.CustomPlanSelected -> setState { copy(customPlan = true) }
            is PlanSetupIntent.RemainingChanged -> setState { copy(remainingInput = intent.value) }
            is PlanSetupIntent.PriceChanged -> setState { copy(priceInput = intent.value) }
            is PlanSetupIntent.CycleChanged -> setState { copy(cycle = intent.cycle, cycleEdited = true) }
            is PlanSetupIntent.SavedPlanSelected -> setState { withPlan(intent.plan) }
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

/** Taille, prix et validité d'un forfait déjà utilisé ; le jour d'achat reste celui du formulaire. */
private fun PlanSetupState.withPlan(plan: SavedPlan): PlanSetupState = copy(
    planInput = QuotaViewModel.bytesToInput(plan.limitBytes),
    customPlan = false,
    priceInput = plan.priceMillimes?.let(QuotaViewModel::millimesToInput).orEmpty(),
    cycle = cycle.copy(
        validityInput = plan.validityDays?.toString() ?: cycle.validityInput,
        monthly = plan.validityDays == null,
        customValidity = false,
    ),
)

/**
 * Après un changement de SIM, le formulaire arrive prérempli : achat le jour du changement (tant
 * que le cycle n'a pas été touché) et dernier forfait connu pour cet opérateur (tant que rien n'est saisi).
 */
private fun PlanSetupState.prefilled(): PlanSetupState {
    if (!operatorChanged) return this
    val change: OperatorChange? = operatorChange
    var state = this
    // Jour du changement de SIM, à la journée comme tout forfait.
    val changeDay = change?.let { com.marzouki.datasaver.presentation.quota.PlanCycleInput.startOfDay(it.atMillis) }
    if (changeDay != null && !cycleEdited && cycle.purchaseMillis != changeDay) {
        state = state.copy(cycle = cycle.copy(purchaseMillis = changeDay))
    }
    val lastPlan = savedPlans.firstOrNull()
    if (lastPlan != null && planInput.isBlank()) state = state.withPlan(lastPlan)
    return state
}
