package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Ajoute [plan] en tête des forfaits retenus pour son opérateur. La même offre (taille et durée)
 * n'apparaît qu'une fois, avec son dernier prix : une promo remplace l'ancien.
 */
class RememberPlanUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(plan: SavedPlan) {
        val others = repository.savedPlans.first().filterNot { it.isSameOfferAs(plan) }
        val sameOperator = others.filter { it.operatorKey == plan.operatorKey }.take(SavedPlan.MAX_PER_OPERATOR - 1)
        repository.setSavedPlans(listOf(plan) + sameOperator + others.filter { it.operatorKey != plan.operatorKey })
    }
}
