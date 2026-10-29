package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Data mobile décomptée du forfait sur [period] ; null sans quota. */
class GetQuotaUsedBytesUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val usageRepository: UsageRepository,
) {
    suspend operator fun invoke(period: MonthPeriod): Result<Long?> = runCatching {
        quotaRepository.quota.first()?.let { usageRepository.quotaUsedBytes(it, period) }
    }
}
