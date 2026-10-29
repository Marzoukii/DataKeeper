package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Prix payé pour le forfait en cours (null l'efface) ; le prix du Go des économies en découle. */
class SetPlanPriceUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(priceMillimes: Long?) {
        val quota = repository.quota.first() ?: return
        repository.setQuota(quota.copy(priceMillimes = priceMillimes?.takeIf { it in 1..PricePerGb.MAX_PLAN_MILLIMES }))
    }
}
