package com.marzouki.datasaver.domain.repository

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DownloadSchedule
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.ScheduledDownload
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun hasUsageAccess(): Boolean

    /** @throws com.marzouki.datasaver.domain.model.UsageAccessDeniedException */
    suspend fun getUsage(period: MonthPeriod): DataUsage
}

interface QuotaRepository {
    val quota: Flow<DataQuota?>
    val lastAlertedMonth: Flow<String?>
    suspend fun setQuota(quota: DataQuota?)
    suspend fun setLastAlertedMonth(monthKey: String?)
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<ScheduledDownload>>
    fun schedule(url: String, fileName: String, schedule: DownloadSchedule)
    fun cancel(id: String)
    fun clearFinished()
}

/** Planifie la vérification périodique du quota en arrière-plan. */
interface QuotaMonitorScheduler {
    fun start()
    fun stop()
}

interface QuotaAlertNotifier {
    fun notifyThresholdReached(status: QuotaStatus)
}
