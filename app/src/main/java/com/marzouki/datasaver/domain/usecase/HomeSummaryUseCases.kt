package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DownloadState
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.domain.repository.DownloadRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Lit en une fois tout ce qu'affichent les widgets et les tuiles ; ne lève jamais d'exception. */
class GetHomeSummaryUseCase @Inject constructor(
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val usageRepository: UsageRepository,
    private val quotaRepository: QuotaRepository,
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(): HomeSummary {
        val period = getCurrentPeriod()
        val hasAccess = usageRepository.hasUsageAccess()
        val usage = if (hasAccess) runCatching { usageRepository.getUsage(period) }.getOrNull() else null
        val pending = runCatching {
            downloadRepository.observeDownloads().first()
                .count { it.state == DownloadState.WAITING_FOR_WIFI || it.state == DownloadState.RUNNING }
        }.getOrDefault(0)
        return HomeSummary(
            period = period,
            hasUsageAccess = hasAccess,
            usage = usage,
            quota = quotaRepository.quota.first(),
            pendingDownloads = pending,
        )
    }
}
