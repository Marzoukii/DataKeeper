package com.marzouki.datasaver.domain.repository

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.DownloadSchedule
import com.marzouki.datasaver.domain.model.MonthPeriod
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
    suspend fun setQuota(quota: DataQuota?)
    suspend fun setLastAlertedMonth(monthKey: String?)
    suspend fun setCycleStartDay(day: Int)
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<ScheduledDownload>>
    fun schedule(url: String, fileName: String, schedule: DownloadSchedule)
    fun cancel(id: String)
    fun clearFinished()
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
