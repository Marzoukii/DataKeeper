package com.marzouki.datasaver.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.presentation.AppRoutes
import com.marzouki.datasaver.ui.components.formatUsedOf
import com.marzouki.datasaver.ui.components.toReadableSize
import kotlin.math.roundToInt

/** Widget 4 × 1 : quota mobile consommé, estimation de fin de cycle et pourcentage. */
class QuotaBarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = QuotaBarWidgetModel.from(context, homeSummaryUseCase(context)())
        provideContent { DataKeeperWidgetTheme { QuotaBarWidgetContent(model) } }
    }
}

class QuotaBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuotaBarWidget()
}

private data class QuotaBarWidgetModel(
    val ring: Bitmap,
    val title: String,
    val subtitle: String,
    val percent: String,
) {
    companion object {
        fun from(context: Context, summary: HomeSummary): QuotaBarWidgetModel {
            val status = summary.quotaStatus
            val ratio = status?.usedRatio ?: 0f
            val ring = quotaRingBitmap(
                context, sizeDp = 48, strokeDp = 6f, ratio = ratio,
                markerFraction = summary.quota?.let { it.alertThresholdPercent / 100f },
            )
            return when {
                !summary.hasUsageAccess ->
                    QuotaBarWidgetModel(ring, context.getString(R.string.quota_title), context.getString(R.string.widget_no_access), "")
                status == null ->
                    QuotaBarWidgetModel(ring, context.getString(R.string.quota_title), context.getString(R.string.widget_no_quota), "")
                else -> QuotaBarWidgetModel(
                    ring = ring,
                    title = context.getString(R.string.widget_used_of_mobile, formatUsedOf(status.usedBytes, status.quota.limitBytes)),
                    subtitle = summary.projectedMobileBytes
                        ?.let { context.getString(R.string.widget_projection, it.toReadableSize()) }
                        .orEmpty(),
                    percent = context.getString(R.string.percent, (ratio * 100).roundToInt()),
                )
            }
        }
    }
}

@Composable
private fun QuotaBarWidgetContent(model: QuotaBarWidgetModel) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(36.dp)
            .background(GlanceTheme.colors.surface)
            .padding(start = 12.dp, end = 20.dp)
            .clickable(actionStartActivity(openTabIntent(context, AppRoutes.QUOTA))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(ImageProvider(model.ring), contentDescription = null, modifier = GlanceModifier.size(48.dp))
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(model.title, style = widgetText(GlanceTheme.colors.onSurface, 14.sp, FontWeight.Medium), maxLines = 1)
            if (model.subtitle.isNotEmpty()) {
                Text(model.subtitle, style = widgetText(GlanceTheme.colors.onSurfaceVariant, 12.sp), maxLines = 1)
            }
        }
        if (model.percent.isNotEmpty()) {
            Spacer(GlanceModifier.width(8.dp))
            Text(model.percent, style = widgetText(GlanceTheme.colors.tertiary, 18.sp, FontWeight.Bold))
        }
    }
}
