package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.ConnectivityRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HasUsageAccessUseCase @Inject constructor(private val repository: UsageRepository) {
    operator fun invoke(): Boolean = repository.hasUsageAccess()
}

/** Cycle de facturation en cours, selon le jour de remise à zéro choisi par l'utilisateur. */
class GetCurrentPeriodUseCase @Inject constructor(private val quotaRepository: QuotaRepository) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): MonthPeriod =
        MonthPeriod.current(nowMillis, quotaRepository.cycleStartDay.first())
}

class GetMonthlyUsageUseCase @Inject constructor(private val repository: UsageRepository) {
    suspend operator fun invoke(period: MonthPeriod = MonthPeriod.current()): Result<DataUsage> =
        runCatching { repository.getUsage(period) }
}

class GetDailyUsageUseCase @Inject constructor(private val repository: UsageRepository) {
    suspend operator fun invoke(days: Int = 7, nowMillis: Long = System.currentTimeMillis()): Result<List<DayUsage>> =
        runCatching { repository.getDailyUsage(days, nowMillis) }
}

class GetTopMobileAppsUseCase @Inject constructor(private val repository: UsageRepository) {
    suspend operator fun invoke(period: MonthPeriod, limit: Int = 3): Result<List<AppDataUsage>> =
        runCatching { repository.getTopMobileApps(period, limit) }
}

class ObserveWifiConnectionUseCase @Inject constructor(private val repository: ConnectivityRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeUnmeteredConnection()
}
