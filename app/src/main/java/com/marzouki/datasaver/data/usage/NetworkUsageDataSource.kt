package com.marzouki.datasaver.data.usage

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import com.marzouki.datasaver.domain.model.UsageAccessDeniedException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Accès bas niveau aux statistiques réseau de l'appareil via [NetworkStatsManager]. */
@Singleton
class NetworkUsageDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)

    /** Vrai si l'utilisateur a accordé "Accès aux données d'utilisation" à l'application. */
    @Suppress("DEPRECATION")
    fun hasUsageAccess(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun wifiBytes(startMillis: Long, endMillis: Long): Long =
        @Suppress("DEPRECATION")
        totalBytes(ConnectivityManager.TYPE_WIFI, startMillis, endMillis)

    /**
     * Depuis Android 10, un subscriberId null agrège toutes les SIM. Avant, la lecture de
     * l'IMSI exige READ_PHONE_STATE et le résultat peut être incomplet.
     */
    fun mobileBytes(startMillis: Long, endMillis: Long): Long =
        @Suppress("DEPRECATION")
        totalBytes(ConnectivityManager.TYPE_MOBILE, startMillis, endMillis)

    /** Data mobile (réception + émission) par UID sur la période. */
    fun mobileBytesByUid(startMillis: Long, endMillis: Long): Map<Int, Long> =
        try {
            @Suppress("DEPRECATION")
            val stats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, null, startMillis, endMillis)
            val totals = HashMap<Int, Long>()
            val bucket = NetworkStats.Bucket()
            try {
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    totals[bucket.uid] = (totals[bucket.uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
                }
            } finally {
                stats.close()
            }
            totals
        } catch (_: SecurityException) {
            throw UsageAccessDeniedException()
        }

    private fun totalBytes(networkType: Int, startMillis: Long, endMillis: Long): Long =
        try {
            val bucket = networkStatsManager.querySummaryForDevice(networkType, null, startMillis, endMillis)
            bucket.rxBytes + bucket.txBytes
        } catch (_: SecurityException) {
            throw UsageAccessDeniedException()
        }
}
