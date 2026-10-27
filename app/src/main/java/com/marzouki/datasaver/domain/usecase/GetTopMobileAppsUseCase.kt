package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.UsageRepository
import javax.inject.Inject

class GetTopMobileAppsUseCase @Inject constructor(private val repository: UsageRepository) {
    suspend operator fun invoke(period: MonthPeriod, limit: Int = 3): Result<List<AppDataUsage>> =
        runCatching { repository.getTopMobileApps(period, limit) }
}
