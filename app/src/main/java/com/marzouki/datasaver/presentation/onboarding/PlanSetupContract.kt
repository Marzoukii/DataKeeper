package com.marzouki.datasaver.presentation.onboarding

import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.presentation.quota.PlanCycleInput
import com.marzouki.datasaver.presentation.quota.QuotaViewModel

/**
 * Configuration du forfait, avant le premier tableau de bord : forfait (ex. 25 Go), data restante
 * annoncée par l'opérateur (ex. 19 Go), jour d'achat et validité (30 jours), prix du forfait (28 DT).
 */
data class PlanSetupState(
    val planInput: String = "",
    val remainingInput: String = "",
    val priceInput: String = "",
    /** Jour d'achat et validité ; un pass de 30 jours acheté aujourd'hui par défaut. */
    val cycle: PlanCycleInput = PlanCycleInput(),
    /** Opérateur de la SIM des données, pour personnaliser le titre. */
    val operatorName: String? = null,
    /** Nouvelle SIM des données : le forfait saisi pour l'ancienne ne vaut plus. */
    val operatorChanged: Boolean = false,
    /** Forfaits déjà saisis pour cet opérateur, proposés en un appui. */
    val savedPlans: List<SavedPlan> = emptyList(),
    /** Erreurs affichées seulement après un premier essai de validation. */
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
) {
    val planBytes: Long? get() = QuotaViewModel.parseGigabytes(planInput)

    /** Null si le champ est vide : le décompte partira alors du début du cycle. */
    val remainingBytes: Long? get() = planBytes?.let { QuotaViewModel.parseBalance(remainingInput, it) }

    /** Prix payé pour ce forfait (ex. 28 DT pour 25 Go), en millimes ; null si vide ou invalide. */
    val planPriceMillimes: Long? get() = PricePerGb.parsePlanPrice(priceInput)

    /** [planPriceMillimes] ramené au Go, affiché sous le champ et utilisé pour chiffrer les économies. */
    val pricePerGb: PricePerGb? get() = planBytes?.let { plan -> planPriceMillimes?.let { PricePerGb.ofPlan(it, plan) } }

    val planError: Boolean get() = planBytes == null
    val remainingError: Boolean get() = remainingInput.isNotBlank() && remainingBytes == null
    // Sans forfait valide, l'erreur est déjà sur le forfait : le prix ne peut pas encore être jugé.
    val priceError: Boolean get() = priceInput.isNotBlank() && planBytes != null && pricePerGb == null

    val canSubmit: Boolean get() = !planError && !remainingError && !priceError && cycle.isValid && !isSaving
}

sealed interface PlanSetupIntent {
    data class PlanChanged(val value: String) : PlanSetupIntent
    data class PresetSelected(val gigabytes: Int) : PlanSetupIntent
    data class RemainingChanged(val value: String) : PlanSetupIntent
    data class PriceChanged(val value: String) : PlanSetupIntent
    data class CycleChanged(val cycle: PlanCycleInput) : PlanSetupIntent
    /** Remplit taille, prix et validité d'un forfait déjà utilisé ; le jour d'achat reste aujourd'hui. */
    data class SavedPlanSelected(val plan: SavedPlan) : PlanSetupIntent
    data class SavedPlanForgotten(val plan: SavedPlan) : PlanSetupIntent
    data object Submit : PlanSetupIntent
    /** « Je n'ai pas de forfait mobile ». */
    data object Skip : PlanSetupIntent
}
