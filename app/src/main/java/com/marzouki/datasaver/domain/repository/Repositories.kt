package com.marzouki.datasaver.domain.repository

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.DownloadSchedule
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.NetworkGeneration
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
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
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<ScheduledDownload>>
    fun schedule(url: String, fileName: String, schedule: DownloadSchedule)
    fun cancel(id: String)
    fun clearFinished()
}

interface OperatorRepository {
    /** Opérateur de la SIM des données mobiles ; null sans SIM prête. */
    fun dataOperator(): MobileOperator?

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
}
