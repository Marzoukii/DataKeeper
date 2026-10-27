package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Au renouvellement du forfait, oublie le solde relevé lors d'un cycle précédent : le décompte
 * repart alors du forfait plein (data mobile mesurée depuis le début du nouveau cycle). Évite que
 * l'écart entre la mesure de l'appli et le compteur de l'opérateur s'accumule d'un mois sur l'autre.
 */
class ClearStaleBalanceUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
) {
    suspend operator fun invoke(): Boolean {
        val quota = quotaRepository.quota.first() ?: return false
        val balance = quota.balance ?: return false
        if (balance.atMillis >= getCurrentPeriod().startMillis) return false
        quotaRepository.setQuota(quota.copy(balance = null))
        quotaRepository.setBalanceReminderAt(null)
        return true
    }
}
