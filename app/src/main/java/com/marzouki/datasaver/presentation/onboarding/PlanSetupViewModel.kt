package com.marzouki.datasaver.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.model.ForfaitSnapshot
import com.marzouki.datasaver.domain.usecase.CompletePlanSetupUseCase
import com.marzouki.datasaver.domain.usecase.FetchForfaitUseCase
import com.marzouki.datasaver.domain.usecase.ForfaitLookup
import com.marzouki.datasaver.domain.usecase.ForgetSavedPlanUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDataOperatorUseCase
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
    private val fetchForfait: FetchForfaitUseCase,
) : MviViewModel<PlanSetupState, PlanSetupIntent, Nothing>(PlanSetupState()) {

    override fun observe(): List<Flow<*>> = listOf(
        // La SIM peut encore changer pendant la saisie : le titre et la clé suivent l'opérateur du moment.
        observeDataOperator().onEach { operator ->
            setState { copy(operatorName = operator?.name, operatorKey = operator?.key) }
        },
        // Suit lui aussi la SIM : sur une carte Tunisie Telecom, jamais les forfaits Ooredoo.
        observeSavedPlans().onEach { plans -> setState { copy(savedPlans = plans) } },
        observePlanStatus().onEach { status ->
            setState {
                // Le ViewModel survit au tableau de bord : un changement de SIM ultérieur repart
                // d'un formulaire vierge, pas des chiffres (ni du « enregistrement… ») de l'ancienne.
                val reset = planStatus == PlanStatus.CONFIGURED && status != PlanStatus.CONFIGURED
                val form = if (reset) {
                    PlanSetupState(operatorName = operatorName, operatorKey = operatorKey, savedPlans = savedPlans)
                } else {
                    this
                }
                form.copy(planStatus = status, operatorChanged = status == PlanStatus.OPERATOR_CHANGED)
            }
        },
    )

    override fun onIntent(intent: PlanSetupIntent) {
        when (intent) {
            // Toute édition manuelle efface la notice de récupération auto (elle ne reflète plus la saisie).
            is PlanSetupIntent.PlanChanged -> setState { copy(planInput = intent.value, fetchNotice = null) }
            is PlanSetupIntent.PresetSelected -> setState { copy(planInput = intent.gigabytes.toString(), customPlan = false, fetchNotice = null) }
            PlanSetupIntent.CustomPlanSelected -> setState { copy(customPlan = true, fetchNotice = null) }
            is PlanSetupIntent.RemainingChanged -> setState { copy(remainingInput = intent.value, fetchNotice = null) }
            is PlanSetupIntent.PriceChanged -> setState { copy(priceInput = intent.value, fetchNotice = null) }
            is PlanSetupIntent.CycleChanged -> setState { copy(cycle = intent.cycle, fetchNotice = null) }
            is PlanSetupIntent.SavedPlanSelected -> setState {
                val plan = intent.plan
                copy(
                    planInput = QuotaViewModel.bytesToInput(plan.limitBytes),
                    customPlan = false,
                    priceInput = plan.priceMillimes?.let(QuotaViewModel::millimesToInput).orEmpty(),
                    cycle = cycle.copy(
                        validityInput = plan.validityDays?.toString() ?: cycle.validityInput,
                        monthly = plan.validityDays == null,
                        customValidity = false,
                    ),
                    fetchNotice = null,
                )
            }
            is PlanSetupIntent.SavedPlanForgotten -> viewModelScope.launch { forgetSavedPlan(intent.plan) }
            PlanSetupIntent.AutoFetch -> autoFetch()
            PlanSetupIntent.Submit -> submit()
            PlanSetupIntent.Skip -> viewModelScope.launch { skipPlanSetup() }
        }
    }

    /** Interroge l'API forfait et pré-remplit les champs ; une panne reste sans effet sur la saisie. */
    private fun autoFetch() {
        if (!currentState.canAutoFetch) return
        val operatorKey = currentState.operatorKey
        setState { copy(isFetching = true, fetchNotice = null) }
        viewModelScope.launch {
            when (val lookup = fetchForfait(operatorKey)) {
                is ForfaitLookup.Found -> setState { prefill(lookup.snapshot) }
                ForfaitLookup.Unsupported -> setState { copy(isFetching = false, fetchNotice = ForfaitFetchNotice.UNSUPPORTED) }
                ForfaitLookup.Failed -> setState { copy(isFetching = false, fetchNotice = ForfaitFetchNotice.FAILED) }
            }
        }
    }

    /** Recopie le forfait récupéré dans les champs du formulaire ; le jour d'achat reste celui saisi. */
    private fun PlanSetupState.prefill(snapshot: ForfaitSnapshot): PlanSetupState =
        copy(
            planInput = QuotaViewModel.bytesToInput(snapshot.limitBytes),
            customPlan = false,
            remainingInput = snapshot.remainingBytes?.let(QuotaViewModel::bytesToInput).orEmpty(),
            priceInput = snapshot.priceMillimes?.let(QuotaViewModel::millimesToInput).orEmpty(),
            cycle = cycle.copy(
                validityInput = snapshot.validityDays?.toString() ?: cycle.validityInput,
                monthly = snapshot.validityDays == null,
                customValidity = false,
            ),
            isFetching = false,
            fetchNotice = ForfaitFetchNotice.FILLED,
        )

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
