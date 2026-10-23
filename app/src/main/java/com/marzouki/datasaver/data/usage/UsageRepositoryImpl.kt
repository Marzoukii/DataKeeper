package com.marzouki.datasaver.data.usage

import android.app.usage.NetworkStats
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.marzouki.datasaver.di.IoDispatcher
import com.marzouki.datasaver.domain.enums.AppKind
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.UsageAccessDeniedException
import com.marzouki.datasaver.domain.repository.UsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dataSource: NetworkUsageDataSource,
    @param:ApplicationContext private val context: Context,
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

    override suspend fun getDailyUsage(days: Int, nowMillis: Long): List<DayUsage> = withContext(ioDispatcher) {
        if (!dataSource.hasUsageAccess()) throw UsageAccessDeniedException()
        val day = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        List(days) {
            val start = day.timeInMillis
            day.add(Calendar.DAY_OF_YEAR, 1)
            val end = minOf(day.timeInMillis, nowMillis)
            DayUsage(
                dayStartMillis = start,
                wifiBytes = dataSource.wifiBytes(start, end),
                mobileBytes = dataSource.mobileBytes(start, end),
            )
        }
    }

    override suspend fun getTopMobileApps(period: MonthPeriod, limit: Int): List<AppDataUsage> =
        withContext(ioDispatcher) {
            if (!dataSource.hasUsageAccess()) throw UsageAccessDeniedException()
            val packageManager = context.packageManager
            dataSource.mobileBytesByUid(period.startMillis, period.endMillis)
                .filterValues { it > 0 }
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { (uid, bytes) -> toAppUsage(packageManager, uid, bytes) }
        }

    private fun toAppUsage(packageManager: PackageManager, uid: Int, bytes: Long): AppDataUsage {
        val kind = when {
            uid == NetworkStats.Bucket.UID_TETHERING -> AppKind.TETHERING
            uid == NetworkStats.Bucket.UID_REMOVED -> AppKind.REMOVED
            uid < Process.FIRST_APPLICATION_UID -> AppKind.SYSTEM
            else -> AppKind.APP
        }
        if (kind != AppKind.APP) return AppDataUsage(kind, packageName = null, label = null, mobileBytes = bytes)
        // Plusieurs paquets peuvent partager un UID : le premier sert de représentant.
        val packageName = packageManager.getPackagesForUid(uid)?.firstOrNull()
        val label = packageName?.let {
            runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString() }
                .getOrNull()
        }
        return AppDataUsage(kind, packageName, label, bytes)
    }
}
