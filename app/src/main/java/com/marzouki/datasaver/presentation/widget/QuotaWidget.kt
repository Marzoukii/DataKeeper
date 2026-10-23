package com.marzouki.datasaver.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.presentation.AppRoutes
import com.marzouki.datasaver.ui.components.toReadableSize
import kotlin.math.roundToInt

/** Widget 2 × 2 : anneau du quota mobile, data restante et jours restants. */
class QuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = QuotaWidgetModel.from(context, homeSummaryUseCase(context)())
        provideContent { DataKeeperWidgetTheme { QuotaWidgetContent(model) } }
    }
}

class QuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuotaWidget()
}

private data class QuotaWidgetModel(
    val ring: Bitmap,
    val value: String,
    val valueLabel: String,
    val footer: String,
    val description: String,
) {
    companion object {
        fun from(context: Context, summary: HomeSummary): QuotaWidgetModel {
            val status = summary.quotaStatus
            val ratio = status?.usedRatio ?: 0f
            val ring = quotaRingBitmap(
                context, sizeDp = 104, strokeDp = 10f, ratio = ratio,
                markerFraction = summary.quota?.let { it.alertThresholdPercent / 100f },
            )
            val days = summary.period.remainingDays
            val daysText = context.resources.getQuantityString(R.plurals.widget_days_left, days, days)
            return when {
                !summary.hasUsageAccess -> QuotaWidgetModel(
                    ring, context.getString(R.string.quota_no_value), "",
                    context.getString(R.string.widget_no_access), context.getString(R.string.widget_no_access),
                )
                summary.operatorChanged -> QuotaWidgetModel(
                    ring, context.getString(R.string.quota_no_value), "",
                    context.getString(R.string.widget_new_sim), context.getString(R.string.widget_new_sim),
                )
                status == null -> QuotaWidgetModel(
                    ring, context.getString(R.string.quota_no_value), "",
                    context.getString(R.string.widget_no_quota), context.getString(R.string.widget_no_quota),
                )
                status.isExceeded -> {
                    val exceeded = status.exceededBytes.toReadableSize()
                    QuotaWidgetModel(
                        ring, exceeded, context.getString(R.string.widget_exceeded), daysText,
                        context.getString(R.string.widget_quota_exceeded_description, exceeded),
                    )
                }
                else -> {
                    val remaining = status.remainingBytes.toReadableSize()
                    QuotaWidgetModel(
                        ring, remaining, context.getString(R.string.widget_remaining), daysText,
                        context.getString(
                            R.string.widget_quota_description,
                            remaining,
                            status.quota.limitBytes.toReadableSize(),
                            (ratio * 100).roundToInt(),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotaWidgetContent(model: QuotaWidgetModel) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(GlanceTheme.colors.surface)
            .padding(14.dp)
            .clickable(actionStartActivity(openTabIntent(context, AppRoutes.QUOTA))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                context.getString(R.string.app_name),
                style = widgetText(GlanceTheme.colors.onSurfaceVariant, 12.sp, FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            Image(
                ImageProvider(R.drawable.ic_cell),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(14.dp),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Box(
            GlanceModifier
                .size(104.dp)
                .semantics { contentDescription = model.description },
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(model.ring), contentDescription = null, modifier = GlanceModifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(model.value, style = widgetText(GlanceTheme.colors.onSurface, 20.sp, FontWeight.Bold))
                if (model.valueLabel.isNotEmpty()) {
                    Text(model.valueLabel, style = widgetText(GlanceTheme.colors.onSurfaceVariant, 11.sp))
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            model.footer,
            style = widgetText(GlanceTheme.colors.onSurfaceVariant, 11.sp).copy(textAlign = TextAlign.Center),
            maxLines = 2,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(GlanceModifier.height(2.dp))
    }
}
