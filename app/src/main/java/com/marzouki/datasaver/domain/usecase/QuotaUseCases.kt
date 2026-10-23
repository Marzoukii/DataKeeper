package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.CycleOutcome
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.OperatorBalance
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
        val current = repository.quota.first()
        // Changer de forfait ne doit pas réactiver une alerte que l'utilisateur a coupée.
        val alertEnabled = current?.alertEnabled ?: true
        // Le solde relevé reste valable tant qu'il tient dans le nouveau forfait.
        val balance = current?.balance?.takeIf { limitBytes != null && it.remainingBytes <= limitBytes }
        repository.setQuota(limitBytes?.let { DataQuota(it, alertEnabled = alertEnabled, balance = balance) })
        // Un nouveau quota doit pouvoir déclencher une nouvelle alerte ce mois-ci.
        repository.setLastAlertedMonth(null)
        if (limitBytes != null && alertEnabled) scheduler.start() else scheduler.stop()
    }
}

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

/** Data mobile décomptée du forfait sur [period] ; null sans quota. */
class GetQuotaUsedBytesUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val usageRepository: UsageRepository,
) {
    suspend operator fun invoke(period: MonthPeriod): Result<Long?> = runCatching {
        quotaRepository.quota.first()?.let { usageRepository.quotaUsedBytes(it, period) }
    }
}

/**
 * Depuis le solde relevé ce cycle : ce qui était déjà parti du forfait plus la data mobile
 * mesurée depuis. Sinon, la data mobile mesurée depuis le début du cycle.
 */
internal suspend fun UsageRepository.quotaUsedBytes(quota: DataQuota, period: MonthPeriod): Long {
    val balance = quota.balanceIn(period) ?: return getUsage(period).mobileBytes
    val sinceBalance = getUsage(period.copy(startMillis = balance.atMillis)).mobileBytes
    return quota.limitBytes - balance.remainingBytes + sinceBalance
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
        val status = QuotaStatus(quota, usageRepository.quotaUsedBytes(quota, currentPeriod))
        if (!status.isAlertReached) return false
        if (quotaRepository.lastAlertedMonth.first() == currentPeriod.key) return false
        notifier.notifyThresholdReached(status)
        quotaRepository.setLastAlertedMonth(currentPeriod.key)
        return true
    }
}
