package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.OperatorBalanceReading
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Solde détecté dans une notification d'opérateur, en attente de confirmation ; null si aucun. */
class ObserveOperatorBalanceReadingUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<OperatorBalanceReading?> = repository.operatorBalanceReading.distinctUntilChanged()
}
