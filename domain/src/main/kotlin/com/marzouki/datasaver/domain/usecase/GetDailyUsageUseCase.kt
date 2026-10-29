package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.UsageRepository
import javax.inject.Inject

class GetDailyUsageUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(days: Int = 7, nowMillis: Long = clock.nowMillis()): Result<List<DayUsage>> =
        runCatching { repository.getDailyUsage(days, nowMillis) }
}
