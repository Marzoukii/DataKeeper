package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DownloadState
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.domain.model.PlanStatus
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
    private val observePlanStatus: ObservePlanStatusUseCase,
) {
    suspend operator fun invoke(): HomeSummary {
        val operatorChanged = runCatching { observePlanStatus().first() == PlanStatus.OPERATOR_CHANGED }.getOrDefault(false)
        val period = getCurrentPeriod()
        val hasAccess = usageRepository.hasUsageAccess()
        val usage = if (hasAccess) runCatching { usageRepository.getUsage(period) }.getOrNull() else null
        val pending = runCatching {
            downloadRepository.observeDownloads().first()
                .count { it.state == DownloadState.WAITING_FOR_WIFI || it.state == DownloadState.RUNNING }
        }.getOrDefault(0)
        // Le forfait de l'ancienne SIM ne vaut plus : pas de quota à montrer d'ici la nouvelle saisie.
        val quota = if (operatorChanged) null else quotaRepository.quota.first()
        val quotaUsed = if (usage != null && quota != null) {
            runCatching { usageRepository.quotaUsedBytes(quota, period) }.getOrNull()
        } else {
            null
        }
        return HomeSummary(
            period = period,
            hasUsageAccess = hasAccess,
            usage = usage,
            quota = quota,
            pendingDownloads = pending,
            pricePerGb = quotaRepository.pricePerGb.first(),
            quotaUsedBytes = quotaUsed,
            operatorChanged = operatorChanged,
        )
    }
}
