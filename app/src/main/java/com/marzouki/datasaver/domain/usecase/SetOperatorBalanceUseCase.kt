package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Recale le forfait sur le solde annoncé par l'opérateur (ex. « il reste 17 Go ») ; null revient
 * au décompte depuis le début du cycle. Le solde est ramené au forfait s'il le dépasse.
 */
class SetOperatorBalanceUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(remainingBytes: Long?, nowMillis: Long = System.currentTimeMillis()) {
        val quota = repository.quota.first() ?: return
        val balance = remainingBytes?.let { OperatorBalance(it.coerceIn(0, quota.limitBytes), nowMillis) }
        repository.setQuota(quota.copy(balance = balance))
        // La consommation du forfait change : l'alerte doit pouvoir se déclencher à nouveau.
        repository.setLastAlertedMonth(null)
        // Le solde vient d'être recalé : le compteur du prochain rappel repart de zéro.
        repository.setBalanceReminderAt(null)
    }
}
