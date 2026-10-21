package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.ui.theme.DataColors

private val ChartHeight = 120.dp
/** Part de la colonne du jour occupée par la barre. */
private const val BarWidthFraction = 0.78f
private val SegmentGap = 3.dp
private val MinSegment = 3.dp

/**
 * Histogramme empilé des derniers jours : Wi-Fi en bas, mobile au-dessus.
 * Le dernier jour (aujourd'hui) a son initiale mise en avant.
 */
@Composable
fun DailyStackedBars(days: List<DayUsage>, modifier: Modifier = Modifier, title: String? = null) {
    val max = days.maxOfOrNull { it.totalBytes }?.takeIf { it > 0 } ?: 1L
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            LegendItem(DataColors.wifi, stringResource(R.string.usage_wifi))
            Spacer(Modifier.width(12.dp))
            LegendItem(DataColors.mobile, stringResource(R.string.usage_mobile))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val description = stringResource(
                    R.string.usage_day_description,
                    dayName(day.dayStartMillis),
                    day.wifiBytes.toReadableSize(),
                    day.mobileBytes.toReadableSize(),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier
                            .height(ChartHeight)
                            .fillMaxWidth(BarWidthFraction),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (day.mobileBytes > 0) {
                            Bar(DataColors.mobile, segmentHeight(day.mobileBytes, max, day))
                            if (day.wifiBytes > 0) Spacer(Modifier.height(SegmentGap))
                        }
                        if (day.wifiBytes > 0) {
                            Bar(DataColors.wifi, segmentHeight(day.wifiBytes, max, day))
                        }
                        if (day.totalBytes == 0L) {
                            Bar(MaterialTheme.colorScheme.surfaceVariant, MinSegment)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val isToday = index == days.lastIndex
                    Text(
                        dayInitial(day.dayStartMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Hauteur proportionnelle, en réservant l'espace entre les deux segments. */
private fun segmentHeight(bytes: Long, max: Long, day: DayUsage): Dp {
    val gap = if (day.wifiBytes > 0 && day.mobileBytes > 0) SegmentGap else 0.dp
    val available = ChartHeight - gap
    return (available * (bytes.toFloat() / max)).coerceAtLeast(MinSegment)
}

@Composable
private fun Bar(color: Color, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
