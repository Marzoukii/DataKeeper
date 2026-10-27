package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Rappelle une fois tous les [intervalMillis] de re-saisir le solde réel chez l'opérateur, tant
 * qu'un solde a été relevé mais commence à dater, pour corriger la dérive de mesure. Renvoie true
 * si un rappel a été émis.
 */
class RemindBalanceRecalibrationUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val notifier: QuotaAlertNotifier,
) {
    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    ): Boolean {
        val balance = quotaRepository.quota.first()?.balance ?: return false
        if (nowMillis - balance.atMillis < intervalMillis) return false
        val lastReminder = quotaRepository.balanceReminderAt.first()
        // Pas plus d'un rappel par intervalle, et aucun avant que le solde ait vieilli.
        if (lastReminder != null && nowMillis - lastReminder < intervalMillis) return false
        if (lastReminder == null && nowMillis - balance.atMillis < intervalMillis) return false
        notifier.notifyBalanceRecalibration(balance.remainingBytes, balance.atMillis)
        quotaRepository.setBalanceReminderAt(nowMillis)
        return true
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
