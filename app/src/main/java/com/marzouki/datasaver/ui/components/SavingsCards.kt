package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric

private val SmallCardShape = RoundedCornerShape(24.dp)

/** Rangée du tableau de bord : économies du Wi-Fi et forfait souscrit chez l'opérateur, de même hauteur. */
@Composable
fun SavingsAndPlanRow(
    wifiBytes: Long,
    pricePerGb: PricePerGb?,
    status: QuotaStatus?,
    quota: DataQuota?,
    balance: OperatorBalance?,
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
    operatorName: String? = null,
    resetMillis: Long? = null,
) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SavingsCard(wifiBytes, pricePerGb, onOpenQuota, Modifier.weight(1f).fillMaxHeight(), operatorName)
        PlanCard(
            quota = quota,
            status = status,
            balance = balance,
            operatorName = operatorName,
            onClick = onOpenQuota,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            resetMillis = resetMillis,
        )
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

/**
 * Forfait souscrit chez l'opérateur (ex. pass Ooredoo de 25 Go), ce qu'il en reste et sa date
 * d'expiration ([resetMillis]). Toucher la carte ouvre le réglage du forfait.
 */
@Composable
fun PlanCard(
    quota: DataQuota?,
    status: QuotaStatus?,
    balance: OperatorBalance?,
    operatorName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    resetMillis: Long? = null,
) {
    DataKeeperCard(
        modifier = modifier,
        shape = SmallCardShape,
        contentPadding = 16.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.SimCard,
                    contentDescription = null,
                    tint = DataColors.mobile,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            val size = quota?.limitBytes?.toReadableSizeParts()
            Text(
                buildAnnotatedString {
                    append(size?.value ?: stringResource(R.string.quota_no_value))
                    if (size != null) withStyle(SpanStyle(fontSize = 16.sp)) { append(" ${size.unit}") }
                },
                style = MaterialTheme.typography.headlineMedium.numeric().copy(fontSize = 28.sp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (quota == null) {
            Text(
                stringResource(R.string.usage_plan_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            return@DataKeeperCard
        }
        Text(
            if (operatorName != null) {
                stringResource(R.string.usage_plan_label_operator, operatorName)
            } else {
                stringResource(R.string.usage_plan_label)
            },
            style = MaterialTheme.typography.titleSmall,
        )
        if (status != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (status.isExceeded) {
                    stringResource(R.string.quota_exceeded_chip, status.exceededBytes.toReadableSize())
                } else {
                    stringResource(R.string.quota_remaining_chip, status.remainingBytes.toReadableSize())
                },
                style = MaterialTheme.typography.bodySmall.numeric(),
                color = if (status.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            RemainingBar(remainingRatio = 1f - status.usedRatio.coerceIn(0f, 1f), exceeded = status.isExceeded)
        }
        // Le solde est saisi à une date ; le forfait expire à la remise à zéro du cycle.
        if (balance != null && resetMillis != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.usage_plan_expires, formatDayMonth(resetMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Part du forfait qui reste : pleine au début, vide une fois consommé. */
@Composable
private fun RemainingBar(remainingRatio: Float, exceeded: Boolean) {
    val description = stringResource(R.string.usage_plan_bar_description, (remainingRatio * 100).toInt())
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(if (exceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        if (remainingRatio > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(remainingRatio)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(DataColors.mobile),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SavingsAndPlanRowPreview() {
    val status = PreviewData.quotaStatus
    val quota = status.quota.copy(balance = OperatorBalance(status.remainingBytes, PreviewData.period.endMillis))
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SavingsAndPlanRow(
                wifiBytes = PreviewData.usage.wifiBytes,
                pricePerGb = PreviewData.pricePerGb,
                status = status.copy(quota = quota),
                quota = quota,
                balance = quota.balance,
                onOpenQuota = {},
                operatorName = "Ooredoo",
            )
            SavingsAndPlanRow(
                wifiBytes = PreviewData.usage.wifiBytes,
                pricePerGb = null,
                status = null,
                quota = null,
                balance = null,
                onOpenQuota = {},
            )
        }
    }
}
