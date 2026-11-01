package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.ui.theme.numeric
import kotlin.math.roundToInt

/**
 * Carte « Il te reste » en tête de Conso : le solde en grand, le nombre de jours et l'expiration,
 * la jauge avec le repère d'alerte, puis la conso du jour face au budget par jour. Cliquable vers Quota.
 * Sans forfait, une invite à le définir ; forfait dépassé, le dépassement en rouge.
 */
@Composable
fun QuotaSummaryCard(
    status: QuotaStatus?,
    period: MonthPeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    todayMobileBytes: Long? = null,
    todayBudgetBytes: Long? = null,
) {
    if (status == null) {
        DataKeeperCard(modifier = modifier, shape = RoundedCornerShape(24.dp), contentPadding = 20.dp, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title(null), style = MaterialTheme.typography.titleMedium)
                    Text(
                        body(null, period),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.quota_open))
            }
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val percent = (status.usedRatio * 100).roundToInt()
    val alertFraction = status.quota.alertThresholdPercent / 100f
    val gaugeDescription = stringResource(R.string.quota_ring_description, percent)
    val accent = if (status.isExceeded) colors.error else colors.primary
    DataKeeperCard(modifier = modifier, shape = RoundedCornerShape(24.dp), contentPadding = 20.dp, onClick = onClick) {
        Text(
            stringResource(if (status.isExceeded) R.string.usage_hero_exceeded else R.string.usage_hero_left),
            style = MaterialTheme.typography.labelLarge,
            color = if (status.isExceeded) colors.error else colors.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                (if (status.isExceeded) status.exceededBytes else status.remainingBytes).toReadableSize(),
                style = MaterialTheme.typography.displaySmall.numeric().copy(fontSize = 48.sp, lineHeight = 52.sp),
                color = if (status.isExceeded) colors.error else colors.onSurface,
            )
            if (!status.isExceeded) {
                Text(
                    stringResource(R.string.usage_hero_remaining_suffix),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                )
            }
        }
        val daysLabel = pluralStringResource(R.plurals.days, period.remainingDays, period.remainingDays)
        val ofLine = stringResource(
            R.string.usage_hero_of,
            status.quota.limitBytes.toReadableSize(),
            daysLabel,
            formatDayMonth(period.nextStartMillis),
        )
        Text(
            buildAnnotatedString {
                append(ofLine)
                val start = ofLine.indexOf(daysLabel)
                if (start >= 0) addStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold), start, start + daysLabel.length)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .semantics { contentDescription = gaugeDescription },
            contentAlignment = Alignment.CenterStart,
        ) {
            ThinBar(status.usedRatio.coerceIn(0f, 1f), accent, trackColor = colors.primaryContainer, height = 10.dp)
            Box(Modifier.fillMaxWidth(alertFraction), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.onSurface),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.usage_hero_used, status.usedBytes.toReadableSize(), percent),
                style = MaterialTheme.typography.bodySmall.numeric(),
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (status.quota.alertEnabled) {
                Text(
                    stringResource(R.string.usage_hero_alert, status.quota.alertThresholdPercent),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (todayMobileBytes != null && todayBudgetBytes != null) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroTile(
                    stringResource(R.string.usage_hero_today),
                    todayMobileBytes.toReadableSize(),
                    stringResource(R.string.usage_hero_today_of, todayBudgetBytes.toReadableSize()),
                    valueColor = if (todayMobileBytes > todayBudgetBytes) colors.error else colors.onSurface,
                )
                HeroTile(
                    stringResource(R.string.usage_hero_budget),
                    stringResource(R.string.usage_hero_budget_value, todayBudgetBytes.toReadableSize()),
                    stringResource(R.string.usage_hero_until, formatDayMonth(period.nextStartMillis)),
                    valueColor = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RowScope.HeroTile(label: String, value: String, sub: String, valueColor: Color) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.numeric(), color = valueColor)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun title(status: QuotaStatus?): String =
    if (status == null) {
        stringResource(R.string.quota_summary_none_title)
    } else {
        stringResource(R.string.quota_summary_title, formatUsedOf(status.usedBytes, status.quota.limitBytes))
    }

@Composable
private fun body(status: QuotaStatus?, period: MonthPeriod): String = when {
    status == null -> stringResource(R.string.quota_summary_none_body)
    status.isExceeded -> stringResource(
        R.string.quota_summary_exceeded,
        status.exceededBytes.toReadableSize(),
        formatDayMonth(period.nextStartMillis),
    )
    else -> buildString {
        append(
            stringResource(
                R.string.quota_summary_remaining,
                status.remainingBytes.toReadableSize(),
                pluralStringResource(R.plurals.days, period.remainingDays, period.remainingDays),
            ),
        )
        if (status.quota.alertEnabled) {
            append(' ')
            append(stringResource(R.string.quota_summary_alert, status.quota.alertThresholdPercent))
        }
    }
}

@ThemePreviews
@Composable
private fun QuotaSummaryCardPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuotaSummaryCard(PreviewData.quotaStatus, PreviewData.period, onClick = {})
            QuotaSummaryCard(PreviewData.quotaAlertStatus, PreviewData.period, onClick = {})
            QuotaSummaryCard(PreviewData.quotaExceededStatus, PreviewData.period, onClick = {})
            QuotaSummaryCard(null, PreviewData.period, onClick = {})
        }
    }
}
