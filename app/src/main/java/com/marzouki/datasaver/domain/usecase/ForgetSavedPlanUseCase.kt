package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ForgetSavedPlanUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(plan: SavedPlan) =
        repository.setSavedPlans(repository.savedPlans.first().filterNot {
            it.isSameOfferAs(plan)
        })
}
