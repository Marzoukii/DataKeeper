package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ObserveQuotaUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<DataQuota?> = repository.quota
}

/** Enregistre (ou supprime avec null) le quota et (dés)active la surveillance en arrière-plan. */
class SetQuotaUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val scheduler: QuotaMonitorScheduler,
) {
    suspend operator fun invoke(limitBytes: Long?) {
        repository.setQuota(limitBytes?.let { DataQuota(it) })
        // Un nouveau quota doit pouvoir déclencher une nouvelle alerte ce mois-ci.
        repository.setLastAlertedMonth(null)
        if (limitBytes != null) scheduler.start() else scheduler.stop()
    }
}

/**
 * Notifie l'utilisateur une seule fois par mois lorsque sa consommation mobile atteint
 * le seuil d'alerte du quota. Renvoie true si une notification a été émise.
 */
class CheckQuotaAndAlertUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val usageRepository: UsageRepository,
    private val notifier: QuotaAlertNotifier,
) {
    suspend operator fun invoke(period: MonthPeriod = MonthPeriod.current()): Boolean {
        val quota = quotaRepository.quota.first() ?: return false
        if (!usageRepository.hasUsageAccess()) return false
        val status = QuotaStatus(quota, usageRepository.getUsage(period).mobileBytes)
        if (!status.isAlertReached) return false
        if (quotaRepository.lastAlertedMonth.first() == period.key) return false
        notifier.notifyThresholdReached(status)
        quotaRepository.setLastAlertedMonth(period.key)
        return true
    }
}
