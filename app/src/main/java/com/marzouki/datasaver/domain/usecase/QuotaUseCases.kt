package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.CycleOutcome
import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.QuotaStreak
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class ObserveQuotaUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<DataQuota?> = repository.quota
}

class ObserveCycleStartDayUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<Int> = repository.cycleStartDay
}

/**
 * Configuré quand le forfait est connu (ou que l'utilisateur a dit ne pas en avoir) : le tableau
 * de bord peut alors s'afficher. Avant, ses chiffres partiraient d'un cycle au hasard (le 1er du
 * mois). Un quota déjà enregistré, d'avant cet écran, vaut configuration.
 *
 * Suivi en direct de la SIM des données : passer d'une SIM Ooredoo à une SIM Tunisie Telecom
 * redemande le forfait, car quota, solde et renouvellement ne sont plus les mêmes. Sans SIM prête
 * (mode avion, démarrage), on garde le forfait en place plutôt que de le redemander à tort.
 */
class ObservePlanStatusUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
) {
    operator fun invoke(): Flow<PlanStatus> =
        combine(
            repository.quota,
            repository.planSetupDone,
            repository.planOperatorKey,
            operatorRepository.observeDataOperator(),
        ) { quota, done, planOperatorKey, operator ->
            when {
                quota == null && !done -> PlanStatus.MISSING
                operator == null || operator.key == planOperatorKey -> PlanStatus.CONFIGURED
                planOperatorKey == null -> {
                    // Forfait saisi avant ce suivi : il est rattaché à la SIM du moment.
                    repository.setPlanOperatorKey(operator.key)
                    PlanStatus.CONFIGURED
                }
                else -> PlanStatus.OPERATOR_CHANGED
            }
        }.distinctUntilChanged()
}

/**
 * Enregistre en une fois ce que demande l'écran de configuration : forfait, jour de
 * renouvellement, solde restant chez l'opérateur et prix du forfait (ces deux derniers facultatifs).
 */
class CompletePlanSetupUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
    private val setQuota: SetQuotaUseCase,
    private val setPlanCycle: SetPlanCycleUseCase,
    private val setOperatorBalance: SetOperatorBalanceUseCase,
    private val setPlanPrice: SetPlanPriceUseCase,
    private val rememberPlan: RememberPlanUseCase,
) {
    /**
     * @param purchaseMillis jour d'achat (ou de renouvellement) du forfait.
     * @param validityDays durée du pass en jours ; null s'il se renouvelle chaque mois à cette date.
     */
    suspend operator fun invoke(
        limitBytes: Long,
        purchaseMillis: Long,
        validityDays: Int?,
        remainingBytes: Long?,
        planPriceMillimes: Long?,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        setPlanCycle(purchaseMillis, validityDays)
        setQuota(limitBytes)
        // Sans solde saisi, le décompte part du début du cycle.
        setOperatorBalance(remainingBytes, nowMillis)
        // Prix laissé vide : celui d'un forfait précédent (autre SIM) ne vaut pas pour celui-ci.
        setPlanPrice(planPriceMillimes)
        val operatorKey = operatorRepository.dataOperator()?.key
        if (operatorKey != null) rememberPlan(SavedPlan(operatorKey, limitBytes, planPriceMillimes, validityDays))
        repository.setPlanOperatorKey(operatorKey)
        repository.setPlanSetupDone(true)
    }
}

/**
 * Cycle du forfait : pass de [validityDays] jours depuis [purchaseMillis] (30 jours pour un
 * Flexi 25 Go, 4 pour un 220 Mo), ou, avec null, chaque mois le jour de [purchaseMillis].
 */
class SetPlanCycleUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val setCycleStartDay: SetCycleStartDayUseCase,
) {
    suspend operator fun invoke(purchaseMillis: Long, validityDays: Int?) {
        if (validityDays == null) {
            val day = Calendar.getInstance().apply { timeInMillis = purchaseMillis }.get(Calendar.DAY_OF_MONTH)
            repository.setPlanValidity(null)
            setCycleStartDay(day)
        } else {
            repository.setPlanValidity(PlanValidity(purchaseMillis, validityDays.coerceIn(1, MonthPeriod.MAX_VALIDITY_DAYS)))
            // Le cycle courant change : l'alerte doit pouvoir se déclencher sur le nouveau.
            repository.setLastAlertedMonth(null)
        }
    }
}

class ObservePlanValidityUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<PlanValidity?> = repository.planValidity
}

/**
 * Ajoute [plan] en tête des forfaits retenus pour son opérateur. La même offre (taille et durée)
 * n'apparaît qu'une fois, avec son dernier prix : une promo remplace l'ancien.
 */
class RememberPlanUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(plan: SavedPlan) {
        val others = repository.savedPlans.first().filterNot { it.isSameOfferAs(plan) }
        val sameOperator = others.filter { it.operatorKey == plan.operatorKey }.take(SavedPlan.MAX_PER_OPERATOR - 1)
        repository.setSavedPlans(listOf(plan) + sameOperator + others.filter { it.operatorKey != plan.operatorKey })
    }
}

class ForgetSavedPlanUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(plan: SavedPlan) =
        repository.setSavedPlans(repository.savedPlans.first().filterNot { it.isSameOfferAs(plan) })
}

/** Forfaits déjà saisis pour l'opérateur de la SIM des données, suivi en direct ; vide sans SIM. */
class ObserveSavedPlansUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
) {
    operator fun invoke(): Flow<List<SavedPlan>> =
        combine(repository.savedPlans, operatorRepository.observeDataOperator()) { plans, operator ->
            if (operator == null) emptyList() else plans.filter { it.operatorKey == operator.key }
        }.distinctUntilChanged()
}

/**
 * « Je n'ai pas de forfait mobile » : le tableau de bord s'affiche sans quota. Après un changement
 * de SIM, le forfait de l'ancienne est retiré.
 */
class SkipPlanSetupUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
    private val setQuota: SetQuotaUseCase,
) {
    suspend operator fun invoke() {
        setQuota(null)
        repository.setPlanOperatorKey(operatorRepository.dataOperator()?.key)
        repository.setPlanSetupDone(true)
    }
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
        // Chaque forfait a son prix (28 DT les 25 Go, 4,8 DT les 1,4 Go) : il ne suit pas un changement de taille.
        val price = current?.priceMillimes?.takeIf { current.limitBytes == limitBytes }
        repository.setQuota(
            limitBytes?.let { DataQuota(it, alertEnabled = alertEnabled, balance = balance, priceMillimes = price) },
        )
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

/** Prix du Go saisi directement, quand il n'y a pas de forfait pour le déduire. */
class SetPricePerGbUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(price: PricePerGb?) = repository.setPricePerGb(price)
}

/** Prix payé pour le forfait en cours (null l'efface) ; le prix du Go des économies en découle. */
class SetPlanPriceUseCase @Inject constructor(private val repository: QuotaRepository) {
    suspend operator fun invoke(priceMillimes: Long?) {
        val quota = repository.quota.first() ?: return
        repository.setQuota(quota.copy(priceMillimes = priceMillimes?.takeIf { it in 1..PricePerGb.MAX_PLAN_MILLIMES }))
    }
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
        val currentPeriod = period ?: quotaRepository.currentPeriod(System.currentTimeMillis())
        val status = QuotaStatus(quota, usageRepository.quotaUsedBytes(quota, currentPeriod))
        if (!status.isAlertReached) return false
        if (quotaRepository.lastAlertedMonth.first() == currentPeriod.key) return false
        notifier.notifyThresholdReached(status)
        quotaRepository.setLastAlertedMonth(currentPeriod.key)
        return true
    }
}
