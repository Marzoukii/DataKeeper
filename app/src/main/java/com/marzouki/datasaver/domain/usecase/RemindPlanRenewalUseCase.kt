package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Rappelle de recharger le forfait [NOTICE_DAYS] jours avant sa fin (remise à zéro du cycle), une
 * seule fois par cycle. Renvoie true si un rappel a été émis.
 */
class RemindPlanRenewalUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val notifier: QuotaAlertNotifier,
) {
    suspend operator fun invoke(period: MonthPeriod? = null): Boolean {
        quotaRepository.quota.first() ?: return false
        val current = period ?: getCurrentPeriod()
        if (current.nextStartMillis - current.endMillis > NOTICE_DAYS * DAY_MILLIS) return false
        if (quotaRepository.lastRenewalReminderCycle.first() == current.key) return false
        notifier.notifyPlanRenewal(current.nextStartMillis)
        quotaRepository.setLastRenewalReminderCycle(current.key)
        return true
    }

    companion object {
        const val NOTICE_DAYS = 2
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
