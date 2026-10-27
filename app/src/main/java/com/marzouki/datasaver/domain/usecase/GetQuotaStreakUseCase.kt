package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.CycleOutcome
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.QuotaStreak
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

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
