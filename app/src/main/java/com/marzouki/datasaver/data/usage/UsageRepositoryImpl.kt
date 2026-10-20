package com.marzouki.datasaver.data.usage

import com.marzouki.datasaver.di.IoDispatcher
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.UsageAccessDeniedException
import com.marzouki.datasaver.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dataSource: NetworkUsageDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UsageRepository {

    override fun hasUsageAccess(): Boolean = dataSource.hasUsageAccess()

    override suspend fun getUsage(period: MonthPeriod): DataUsage = withContext(ioDispatcher) {
        if (!dataSource.hasUsageAccess()) throw UsageAccessDeniedException()
        DataUsage(
            wifiBytes = dataSource.wifiBytes(period.startMillis, period.endMillis),
            mobileBytes = dataSource.mobileBytes(period.startMillis, period.endMillis),
            period = period,
        )
    }
}
