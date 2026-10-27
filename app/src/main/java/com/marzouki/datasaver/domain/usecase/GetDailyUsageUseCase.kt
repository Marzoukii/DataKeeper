package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.repository.UsageRepository
import javax.inject.Inject

class GetDailyUsageUseCase @Inject constructor(private val repository: UsageRepository) {
    suspend operator fun invoke(days: Int = 7, nowMillis: Long = System.currentTimeMillis()): Result<List<DayUsage>> =
        runCatching { repository.getDailyUsage(days, nowMillis) }
}
