package com.marzouki.datasaver.data.quota

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marzouki.datasaver.MainActivity
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class NotificationQuotaAlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : QuotaAlertNotifier {

    override fun notifyThresholdReached(status: QuotaStatus) {
        val percent = (status.usedRatio * 100).roundToInt()
        val text = context.getString(
            R.string.notification_quota_text,
            Formatter.formatShortFileSize(context, status.usedBytes),
            Formatter.formatShortFileSize(context, status.quota.limitBytes),
        )
        post(NOTIFICATION_ID, context.getString(R.string.notification_quota_title, percent), text, NotificationCompat.PRIORITY_HIGH)
    }

    override fun notifyBalanceRecalibration(remainingBytes: Long, sinceMillis: Long) {
        val days = ((System.currentTimeMillis() - sinceMillis) / DAY_MILLIS).coerceAtLeast(1).toInt()
        val text = context.resources.getQuantityString(
            R.plurals.notification_recalibrate_text, days, days, Formatter.formatShortFileSize(context, remainingBytes),
        )
        post(RECALIBRATE_NOTIFICATION_ID, context.getString(R.string.notification_recalibrate_title), text, NotificationCompat.PRIORITY_DEFAULT)
    }

    override fun notifyPlanRenewal(renewalMillis: Long) {
        // Arrondi au jour supérieur : à 36 h de l'échéance, « dans 2 jours ».
        val days = ((renewalMillis - System.currentTimeMillis() + DAY_MILLIS - 1) / DAY_MILLIS).coerceAtLeast(1).toInt()
        val date = SimpleDateFormat("d MMMM", Locale.FRANCE).format(Date(renewalMillis))
        post(
            RENEWAL_NOTIFICATION_ID,
            context.resources.getQuantityString(R.plurals.notification_renewal_title, days, days),
            context.getString(R.string.notification_renewal_text, date),
            NotificationCompat.PRIORITY_DEFAULT,
        )
    }

    /** Publie sur le canal des alertes de quota ; rien sans la permission de notifier (Android 13+). */
    private fun post(id: Int, title: String, text: String, priority: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.notification_channel_quota))
                .build()
        )

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_data)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(priority)
            .build()
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL_ID = "quota_alerts"
        const val NOTIFICATION_ID = 80
        const val RECALIBRATE_NOTIFICATION_ID = 81
        const val RENEWAL_NOTIFICATION_ID = 82
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
