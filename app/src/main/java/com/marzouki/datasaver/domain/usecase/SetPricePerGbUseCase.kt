package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.repository.QuotaRepository
import javax.inject.Inject

/** Prix du Go saisi directement, quand il n'y a pas de forfait pour le déduire. */
class SetPricePerGbUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(price: PricePerGb?) = repository.setPricePerGb(price)
}
