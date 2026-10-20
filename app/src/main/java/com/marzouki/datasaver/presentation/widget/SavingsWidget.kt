package com.marzouki.datasaver.presentation.widget

import android.content.Context
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.HomeSummary
import com.marzouki.datasaver.presentation.AppRoutes
import com.marzouki.datasaver.ui.components.toReadableSize
import kotlin.math.roundToInt

/**
 * Widget 2 × 2 bleu : data passée par le Wi-Fi ce cycle, donc économisée sur le forfait mobile.
 * La maquette affiche un montant en euros ; sans prix du Go connu, le volume est affiché.
 */
class SavingsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = SavingsWidgetModel.from(context, homeSummaryUseCase(context)())
        provideContent { DataKeeperWidgetTheme { SavingsWidgetContent(model) } }
    }
}

class SavingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SavingsWidget()
}

private data class SavingsWidgetModel(val value: String, val share: String) {
    companion object {
        fun from(context: Context, summary: HomeSummary): SavingsWidgetModel {
            val usage = summary.usage
                ?: return SavingsWidgetModel(context.getString(R.string.quota_no_value), context.getString(R.string.widget_no_access))
            return SavingsWidgetModel(
                value = usage.wifiBytes.toReadableSize(),
                share = context.getString(R.string.widget_wifi_share, (usage.wifiShare * 100).roundToInt()),
            )
        }
    }
}

@Composable
private fun SavingsWidgetContent(model: SavingsWidgetModel) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(GlanceTheme.colors.primary)
            .padding(16.dp)
            .clickable(actionStartActivity(openTabIntent(context, AppRoutes.USAGE))),
    ) {
        Image(
            ImageProvider(R.drawable.ic_wifi),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(26.dp),
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(context.getString(R.string.widget_saved_this_month), style = widgetText(GlanceTheme.colors.onPrimary, 12.sp))
        Text(model.value, style = widgetText(GlanceTheme.colors.onPrimary, 28.sp, FontWeight.Bold), maxLines = 1)
        Text(model.share, style = widgetText(GlanceTheme.colors.onPrimary, 12.sp), maxLines = 2)
    }
}
