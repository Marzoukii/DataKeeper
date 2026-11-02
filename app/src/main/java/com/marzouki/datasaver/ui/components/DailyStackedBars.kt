package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.ui.theme.Brand
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric

/**
 * Le carnet des derniers jours, lu comme une imsakia : une ligne par jour, du plus ancien à
 * aujourd'hui, avec le Wi-Fi et le mobile en colonnes. Aujourd'hui (la dernière ligne) est en bande
 * safran. Avec [onDayClick], chaque ligne est touchable et le jour [selectedDayMillis] reste opaque
 * quand les autres s'estompent.
 */
@Composable
fun DailyStackedBars(
    days: List<DayUsage>,
    modifier: Modifier = Modifier,
    title: String? = null,
    selectedDayMillis: Long? = null,
    onDayClick: ((DayUsage) -> Unit)? = null,
    /** Budget du jour en data mobile ; affiché sous la ligne d'aujourd'hui quand il est connu. */
    todayBudgetBytes: Long? = null,
    /**
     * Data mobile d'aujourd'hui comptée sur le forfait en cours, face au budget ; null pour toute la
     * journée. Plus petite quand un nouveau pass a été activé en cours de journée.
     */
    todayPlanBytes: Long? = null,
) {
    val maxWifi = days.maxOfOrNull { it.wifiBytes }?.takeIf { it > 0 } ?: 1L
    val maxMobile = days.maxOfOrNull { it.mobileBytes }?.takeIf { it > 0 } ?: 1L
    // Un jour sorti de la fenêtre ne doit pas estomper toutes les lignes.
    val selected = selectedDayMillis?.takeIf { millis -> days.any { it.dayStartMillis == millis } }
    val ruleColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            LegendItem(DataColors.wifi, stringResource(R.string.usage_wifi))
            Spacer(Modifier.width(12.dp))
            LegendItem(DataColors.mobile, stringResource(R.string.usage_mobile))
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = ruleColor)
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            val description = stringResource(
                R.string.usage_day_description,
                dayName(day.dayStartMillis),
                day.wifiBytes.toReadableSize(),
                day.mobileBytes.toReadableSize(),
            )
            val dimmed = selected != null && selected != day.dayStartMillis
            if (index > 0 && !isToday) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (isToday) Modifier.clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                    .then(
                        if (onDayClick != null) {
                            Modifier.clickable(role = Role.Button) { onDayClick(day) }
                        } else {
                            Modifier
                        },
                    )
                    .alpha(if (dimmed) 0.4f else 1f)
                    .clearAndSetSemantics { contentDescription = description }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(52.dp)) {
                    Text(
                        dayOfMonth(day.dayStartMillis),
                        style = MaterialTheme.typography.titleLarge.numeric().copy(fontWeight = FontWeight.Medium),
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (isToday) stringResource(R.string.usage_today_short) else dayShortName(day.dayStartMillis),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                LedgerCell(
                    value = day.wifiBytes.toReadableSize(),
                    fraction = day.wifiBytes.toFloat() / maxWifi,
                    color = DataColors.wifi,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                LedgerCell(
                    value = day.mobileBytes.toReadableSize(),
                    fraction = day.mobileBytes.toFloat() / maxMobile,
                    color = DataColors.mobile,
                    modifier = Modifier.weight(1f),
                )
            }
            if (isToday && todayBudgetBytes != null) {
                TodayBudget(todayPlanBytes ?: day.mobileBytes, todayBudgetBytes)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = ruleColor)
    }
}

/** Sous la ligne d'aujourd'hui, toujours sur la bande lavande pâle : le budget du jour et ce qu'il en reste. */
@Composable
private fun TodayBudget(usedBytes: Long, budgetBytes: Long) {
    val over = usedBytes > budgetBytes
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 56.dp, end = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.budget_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${usedBytes.toReadableSize()} / ${budgetBytes.toReadableSize()}",
                style = MaterialTheme.typography.labelMedium.numeric(),
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        ThinBar(
            fraction = if (budgetBytes == 0L) 1f else usedBytes.toFloat() / budgetBytes,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface,
            height = 6.dp,
        )
        Text(
            if (over) {
                stringResource(R.string.budget_over, (usedBytes - budgetBytes).toReadableSize())
            } else {
                stringResource(R.string.budget_left, (budgetBytes - usedBytes).toReadableSize())
            },
            style = MaterialTheme.typography.bodySmall.numeric(),
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}


@Composable
private fun LedgerCell(value: String, fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, style = MaterialTheme.typography.bodyMedium.numeric(), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun DailyStackedBarsPreview() {
    PreviewSurface {
        DataKeeperCard {
            DailyStackedBars(PreviewData.days, title = "7 derniers jours")
        }
    }
}
