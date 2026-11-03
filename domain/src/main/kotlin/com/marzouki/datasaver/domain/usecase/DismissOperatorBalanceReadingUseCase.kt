package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaRepository
import javax.inject.Inject

/** Écarte le solde détecté sans recaler le forfait. */
class DismissOperatorBalanceReadingUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke() = repository.setOperatorBalanceReading(null)
}
