package com.marzouki.datasaver.domain.repository

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.DownloadSchedule
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.NetworkGeneration
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.model.ScheduledDownload
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun hasUsageAccess(): Boolean

    /** @throws com.marzouki.datasaver.domain.model.UsageAccessDeniedException */
    suspend fun getUsage(period: MonthPeriod): DataUsage

    /** Trafic des [days] derniers jours calendaires, aujourd'hui compris, du plus ancien au plus récent. */
    suspend fun getDailyUsage(days: Int, nowMillis: Long): List<DayUsage>

    /** Applications ayant consommé le plus de data mobile sur la période, par ordre décroissant. */
    suspend fun getTopMobileApps(period: MonthPeriod, limit: Int): List<AppDataUsage>
}

interface QuotaRepository {
    val quota: Flow<DataQuota?>
    val lastAlertedMonth: Flow<String?>
    /** Jour du mois (1 à 28) où le forfait est remis à zéro. */
    val cycleStartDay: Flow<Int>
    /** Prix d'un Go mobile, pour chiffrer les économies du Wi-Fi ; null tant qu'il n'est pas renseigné. */
    val pricePerGb: Flow<PricePerGb?>
    suspend fun setQuota(quota: DataQuota?)
    suspend fun setLastAlertedMonth(monthKey: String?)
    suspend fun setCycleStartDay(day: Int)
    suspend fun setPricePerGb(price: PricePerGb?)
    /** Date du dernier rappel « recale ton solde » envoyé ; null si jamais. */
    val balanceReminderAt: Flow<Long?>
    suspend fun setBalanceReminderAt(millis: Long?)
    /** Cycle ([MonthPeriod.key]) dont la fin du forfait a déjà été rappelée ; null si jamais. */
    val lastRenewalReminderCycle: Flow<String?>
    suspend fun setLastRenewalReminderCycle(cycleKey: String?)
    /** Vrai une fois l'écran de configuration du forfait validé ou passé (« pas de forfait »). */
    val planSetupDone: Flow<Boolean>
    suspend fun setPlanSetupDone(done: Boolean)
    /** [com.marzouki.datasaver.domain.model.MobileOperator.key] de la SIM pour laquelle le forfait a été saisi. */
    val planOperatorKey: Flow<String?>
    suspend fun setPlanOperatorKey(key: String?)
    /** Pass en cours (date d'achat et validité) ; null pour un cycle mensuel selon [cycleStartDay]. */
    val planValidity: Flow<PlanValidity?>
    suspend fun setPlanValidity(validity: PlanValidity?)
    /** Forfaits déjà saisis, tous opérateurs confondus, du plus récent au plus ancien. */
    val savedPlans: Flow<List<SavedPlan>>
    suspend fun setSavedPlans(plans: List<SavedPlan>)
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<ScheduledDownload>>
    fun schedule(url: String, fileName: String, schedule: DownloadSchedule)
    fun cancel(id: String)
    fun clearFinished()
}

interface OperatorRepository {
    /** Opérateur de la SIM des données mobiles ; null sans SIM prête ou illisible. */
    fun dataOperator(): MobileOperator?

    /** [dataOperator], relu à chaque changement de SIM (carte changée, SIM des données basculée). */
    fun observeDataOperator(): Flow<MobileOperator?>

    /** Permission « Téléphone » (READ_PHONE_STATE), nécessaire pour connaître la 4G ou la 5G. */
    fun hasPhoneStatePermission(): Boolean

    /** 2G à 5G pour la SIM des données, mise à jour en continu ; null sans permission ou hors réseau. */
    fun observeDataNetworkGeneration(): Flow<NetworkGeneration?>
}

interface ConnectivityRepository {
    /** Vrai tant que l'appareil est sur un réseau non facturé (Wi-Fi), condition des téléchargements. */
    fun observeUnmeteredConnection(): Flow<Boolean>
}

/** Planifie la vérification périodique du quota en arrière-plan. */
interface QuotaMonitorScheduler {
    fun start()
    fun stop()
}

interface QuotaAlertNotifier {
    fun notifyThresholdReached(status: QuotaStatus)

    /** Invite à re-saisir le solde réel chez l'opérateur, relevé pour la dernière fois à [sinceMillis]. */
    fun notifyBalanceRecalibration(remainingBytes: Long, sinceMillis: Long)

    /** Prévient que le forfait arrive à son terme le [renewalMillis] et qu'il faudra le recharger. */
    fun notifyPlanRenewal(renewalMillis: Long)
}
