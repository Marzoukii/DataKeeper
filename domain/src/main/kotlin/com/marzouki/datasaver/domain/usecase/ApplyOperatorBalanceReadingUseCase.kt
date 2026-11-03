package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Confirme le solde détecté dans une notification d'opérateur : il recale le forfait, puis la
 * détection en attente est effacée. Sans forfait enregistré ou sans détection, ne fait rien.
 */
class ApplyOperatorBalanceReadingUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val setOperatorBalance: SetOperatorBalanceUseCase,
) {
    suspend operator fun invoke() {
        val reading = repository.operatorBalanceReading.first() ?: return
        setOperatorBalance(reading.remainingBytes, reading.atMillis)
        repository.setOperatorBalanceReading(null)
    }
}
