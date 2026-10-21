package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.CycleOutcome
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStreak
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric

private val SmallCardShape = RoundedCornerShape(24.dp)

/** Rangée du tableau de bord : économies du Wi-Fi et série de mois sans dépassement, de même hauteur. */
@Composable
fun SavingsAndStreakRow(
    wifiBytes: Long,
    pricePerGb: PricePerGb?,
    streak: QuotaStreak?,
    currentCycleExceeded: Boolean,
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
    operatorName: String? = null,
) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SavingsCard(wifiBytes, pricePerGb, onOpenQuota, Modifier.weight(1f).fillMaxHeight(), operatorName)
        StreakCard(streak, currentCycleExceeded, onOpenQuota, Modifier.weight(1f).fillMaxHeight())
    }
}

/**
 * Ce qu'aurait coûté en data mobile le trafic passé par le Wi-Fi ; le volume seul tant que le prix manque.
 * [operatorName] rappelle à quel forfait le prix du Go se rapporte.
 */
@Composable
fun SavingsCard(
    wifiBytes: Long,
    pricePerGb: PricePerGb?,
    onSetPrice: () -> Unit,
    modifier: Modifier = Modifier,
    operatorName: String? = null,
) {
    DataKeeperCard(
        modifier = modifier,
        shape = SmallCardShape,
        contentPadding = 16.dp,
        // Sans prix, la carte mène au réglage.
        onClick = if (pricePerGb == null) onSetPrice else null,
    ) {
        Text(
            stringResource(R.string.usage_savings_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (pricePerGb != null) formatDinars(pricePerGb.costMillimes(wifiBytes)) else wifiBytes.toReadableSize(),
            style = MaterialTheme.typography.headlineMedium.numeric().copy(fontSize = 28.sp),
            color = DataColors.success,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (pricePerGb != null) {
                val volume = wifiBytes.toReadableSize()
                val price = formatDinars(pricePerGb.millimes.toLong())
                if (operatorName != null) {
                    stringResource(R.string.usage_savings_detail_operator, volume, price, operatorName)
                } else {
                    stringResource(R.string.usage_savings_detail, volume, price)
                }
            } else {
                stringResource(R.string.usage_savings_no_price)
            },
            style = MaterialTheme.typography.bodySmall.numeric(),
            color = if (pricePerGb != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        )
    }
}

/** Nombre de cycles terminés sous le quota, et une barre par cycle récent plus le cycle en cours. */
@Composable
fun StreakCard(
    streak: QuotaStreak?,
    currentCycleExceeded: Boolean,
    onSetQuota: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DataKeeperCard(
        modifier = modifier,
        shape = SmallCardShape,
        contentPadding = 16.dp,
        onClick = if (streak == null) onSetQuota else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DataColors.successContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = DataColors.success,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                streak?.months?.toString() ?: stringResource(R.string.quota_no_value),
                style = MaterialTheme.typography.headlineMedium.numeric().copy(fontSize = 28.sp),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (streak == null) {
            Text(
                stringResource(R.string.usage_streak_no_quota),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(stringResource(R.string.usage_streak_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            StreakBars(streak.recentOutcomes, currentCycleExceeded)
        }
    }
}

@Composable
private fun StreakBars(outcomes: List<CycleOutcome>, currentCycleExceeded: Boolean) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(
        R.string.usage_streak_bars_description,
        outcomes.map { stringResource(it.labelRes) }.joinToString(", "),
        stringResource(if (currentCycleExceeded) R.string.usage_streak_outcome_exceeded else R.string.usage_streak_current_running),
    )
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Toujours la même largeur : les cycles sans historique restent des barres neutres.
        val padding = List(QuotaStreak.RECENT_CYCLES - outcomes.size) { CycleOutcome.UNKNOWN }
        (padding + outcomes).forEach { outcome ->
            StreakBar(
                when (outcome) {
                    CycleOutcome.WITHIN_QUOTA -> DataColors.success
                    CycleOutcome.EXCEEDED -> colors.error
                    CycleOutcome.UNKNOWN -> colors.surfaceVariant
                },
            )
        }
        StreakBar(if (currentCycleExceeded) colors.error else colors.surfaceVariant)
    }
}

@Composable
private fun RowScope.StreakBar(color: Color) {
    Box(
        Modifier
            .weight(1f)
            .height(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}

private val CycleOutcome.labelRes: Int
    get() = when (this) {
        CycleOutcome.WITHIN_QUOTA -> R.string.usage_streak_outcome_within
        CycleOutcome.EXCEEDED -> R.string.usage_streak_outcome_exceeded
        CycleOutcome.UNKNOWN -> R.string.usage_streak_outcome_unknown
    }

@ThemePreviews
@Composable
private fun SavingsAndStreakRowPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SavingsAndStreakRow(
                wifiBytes = PreviewData.usage.wifiBytes,
                pricePerGb = PreviewData.pricePerGb,
                streak = PreviewData.streak,
                currentCycleExceeded = false,
                onOpenQuota = {},
                operatorName = "Ooredoo",
            )
            SavingsAndStreakRow(
                wifiBytes = PreviewData.usage.wifiBytes,
                pricePerGb = null,
                streak = null,
                currentCycleExceeded = false,
                onOpenQuota = {},
            )
        }
    }
}
