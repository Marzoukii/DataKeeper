package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.OperatorBalanceReading
import com.marzouki.datasaver.domain.model.OperatorBalanceSmsParser
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.QuotaRepository
import javax.inject.Inject

/**
 * Lit passivement une notification (souvent un SMS) de l'opérateur : si [title] et [body] annoncent
 * un solde data, il est mémorisé pour que l'utilisateur confirme le recalage. Renvoie true quand un
 * solde a été reconnu. Ne contacte aucun serveur : c'est le message que l'opérateur a déjà envoyé.
 */
class RecordOperatorBalanceFromTextUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(title: String?, body: String, nowMillis: Long = clock.nowMillis()): Boolean {
        val reading = OperatorBalanceSmsParser.parse(title, body) ?: return false
        repository.setOperatorBalanceReading(
            OperatorBalanceReading(
                remainingBytes = reading.remainingBytes,
                operatorName = reading.operator?.displayName,
                atMillis = nowMillis,
            ),
        )
        return true
    }
}
