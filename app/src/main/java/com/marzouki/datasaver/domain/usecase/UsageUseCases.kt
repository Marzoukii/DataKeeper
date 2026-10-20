package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.UsageRepository
import javax.inject.Inject

class HasUsageAccessUseCase @Inject constructor(private val repository: UsageRepository) {
    operator fun invoke(): Boolean = repository.hasUsageAccess()
}

class GetMonthlyUsageUseCase @Inject constructor(private val repository: UsageRepository) {
    suspend operator fun invoke(period: MonthPeriod = MonthPeriod.current()): Result<DataUsage> =
        runCatching { repository.getUsage(period) }
}
