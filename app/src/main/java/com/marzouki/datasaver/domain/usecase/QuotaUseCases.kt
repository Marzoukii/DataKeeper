package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.CycleOutcome
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.QuotaStreak
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

class ObserveCycleStartDayUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<Int> = repository.cycleStartDay
}

/** Enregistre (ou supprime avec null) le quota et (dés)active la surveillance en arrière-plan. */
class SetQuotaUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val scheduler: QuotaMonitorScheduler,
) {
    suspend operator fun invoke(limitBytes: Long?) {
        // Changer de forfait ne doit pas réactiver une alerte que l'utilisateur a coupée.
        val alertEnabled = repository.quota.first()?.alertEnabled ?: true
        repository.setQuota(limitBytes?.let { DataQuota(it, alertEnabled = alertEnabled) })
        // Un nouveau quota doit pouvoir déclencher une nouvelle alerte ce mois-ci.
        repository.setLastAlertedMonth(null)
        if (limitBytes != null && alertEnabled) scheduler.start() else scheduler.stop()
    }
}

/** Active ou coupe la notification de seuil du quota existant. */
class SetQuotaAlertEnabledUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val scheduler: QuotaMonitorScheduler,
) {
    suspend operator fun invoke(enabled: Boolean) {
        val quota = repository.quota.first() ?: return
        repository.setQuota(quota.copy(alertEnabled = enabled))
        if (enabled) scheduler.start() else scheduler.stop()
    }
}

class ObservePricePerGbUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<PricePerGb?> = repository.pricePerGb
}

class SetPricePerGbUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(price: PricePerGb?) = repository.setPricePerGb(price)
}

/**
 * Remonte les cycles terminés tant qu'ils restent sous le quota actuel, jusqu'à
 * [QuotaStreak.MAX_CYCLES]. Null sans quota : il n'y a rien à comparer.
 */
class GetQuotaStreakUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val usageRepository: UsageRepository,
) {
    suspend operator fun invoke(current: MonthPeriod): Result<QuotaStreak?> = runCatching {
        val quota = quotaRepository.quota.first() ?: return@runCatching null
        val outcomes = mutableListOf<CycleOutcome>()
        var period = current
        while (outcomes.size < QuotaStreak.MAX_CYCLES) {
            period = period.previous()
            val usage = usageRepository.getUsage(period)
            val outcome = when {
                usage.totalBytes == 0L -> CycleOutcome.UNKNOWN
                QuotaStatus(quota, usage.mobileBytes).isExceeded -> CycleOutcome.EXCEEDED
                else -> CycleOutcome.WITHIN_QUOTA
            }
            outcomes += outcome
            // La série est rompue et les cycles récents sont connus : inutile de remonter plus loin.
            if (outcome != CycleOutcome.WITHIN_QUOTA && outcomes.size >= QuotaStreak.RECENT_CYCLES) break
        }
        QuotaStreak.from(outcomes)
    }
}

class SetCycleStartDayUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(day: Int) {
        repository.setCycleStartDay(day.coerceIn(1, MonthPeriod.MAX_CYCLE_START_DAY))
        // Le cycle courant change : l'alerte doit pouvoir se déclencher sur le nouveau.
        repository.setLastAlertedMonth(null)
    }
}

/**
 * Notifie l'utilisateur une seule fois par cycle lorsque sa consommation mobile atteint
 * le seuil d'alerte du quota. Renvoie true si une notification a été émise.
 */
class CheckQuotaAndAlertUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val usageRepository: UsageRepository,
    private val notifier: QuotaAlertNotifier,
) {
    suspend operator fun invoke(period: MonthPeriod? = null): Boolean {
        val quota = quotaRepository.quota.first() ?: return false
        if (!quota.alertEnabled) return false
        if (!usageRepository.hasUsageAccess()) return false
        val currentPeriod = period ?: MonthPeriod.current(cycleStartDay = quotaRepository.cycleStartDay.first())
        val status = QuotaStatus(quota, usageRepository.getUsage(currentPeriod).mobileBytes)
        if (!status.isAlertReached) return false
        if (quotaRepository.lastAlertedMonth.first() == currentPeriod.key) return false
        notifier.notifyThresholdReached(status)
        quotaRepository.setLastAlertedMonth(currentPeriod.key)
        return true
    }
}
