package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.ui.theme.numeric
import kotlin.math.roundToInt

/**
 * Résumé du quota mobile sur le tableau de bord, cliquable vers l'écran Quota.
 * Le fond se durcit à l'approche du seuil (tertiary) puis passe en erreur une fois dépassé.
 */
@Composable
fun QuotaSummaryCard(
    status: QuotaStatus?,
    period: MonthPeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when {
        status == null -> colors.tertiaryContainer to colors.onTertiaryContainer
        status.isExceeded -> colors.errorContainer to colors.onErrorContainer
        status.isAlertReached -> colors.tertiary to colors.onTertiary
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    val onStrongBackground = status != null && status.isAlertReached && !status.isExceeded
    val percent = status?.let { (it.usedRatio * 100).roundToInt() } ?: 0
    val ringDescription = stringResource(R.string.quota_ring_description, percent)

    DataKeeperCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = container,
        contentColor = content,
        contentPadding = 16.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuotaRing(
                used = status?.usedBytes ?: 0L,
                quota = status?.quota?.limitBytes ?: 0L,
                color = if (onStrongBackground) content else MaterialTheme.colorScheme.tertiary,
                trackColor = if (onStrongBackground) content.copy(alpha = 0.3f) else colors.surface,
                markerColor = if (onStrongBackground) content else colors.onSurface,
                markerFraction = (status?.quota?.alertThresholdPercent ?: 80) / 100f,
                modifier = Modifier.semantics { contentDescription = ringDescription },
            ) {
                if (status != null) {
                    Text(
                        stringResource(R.string.percent, percent),
                        style = MaterialTheme.typography.labelMedium.numeric(),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title(status), style = MaterialTheme.typography.titleMedium)
                Text(
                    body(status, period),
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.85f),
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.quota_open),
                tint = content,
            )
        }
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
