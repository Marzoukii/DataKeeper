package com.marzouki.datasaver.presentation.usage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.HeroCard
import com.marzouki.datasaver.ui.components.IconChip
import com.marzouki.datasaver.ui.components.formatDayMonth
import com.marzouki.datasaver.ui.components.formatDinars
import com.marzouki.datasaver.ui.components.formatDinarsRounded
import com.marzouki.datasaver.ui.components.toPlanSize
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Phrase d'état en tête de Conso, pour comprendre la situation avant les chiffres : forfait
 * dépassé, seuil d'alerte atteint, nouveau forfait du jour, rythme trop rapide ou tout va bien.
 */
@Composable
internal fun StatusLine(status: QuotaStatus?, period: MonthPeriod) {
    if (status == null) return
    val colors = MaterialTheme.colorScheme
    val newToday = period.currentDay == 1
    val perDay = status.usedBytes / period.currentDay.coerceAtLeast(1)
    val emptyAt = if (perDay > 0) period.endMillis + status.remainingBytes / perDay * DAY_MILLIS else Long.MAX_VALUE
    val (dot, text) = when {
        status.isExceeded -> colors.error to stringResource(R.string.usage_status_exceeded, status.exceededBytes.toPlanSize())
        status.isAlertReached -> colors.tertiary to stringResource(R.string.usage_status_alert, (status.usedRatio * 100).roundToInt())
        newToday -> colors.primary to stringResource(R.string.usage_status_new_plan)
        period.currentDay >= 3 && emptyAt < period.nextStartMillis ->
            colors.tertiary to stringResource(R.string.usage_status_pace, formatDayMonth(emptyAt))
        else -> DataColors.wifi to stringResource(R.string.usage_status_ok, formatDayMonth(period.nextStartMillis))
    }
    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
    }
}

/** Grande carte « Il te reste », seule place du solde dans l'appli ; la toucher ouvre Quota. */
@Composable
internal fun RemainingHero(status: QuotaStatus?, period: MonthPeriod, onOpenQuota: () -> Unit) {
    HeroCard(onClick = onOpenQuota) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    when {
                        status == null -> R.string.usage_hero_none
                        status.isExceeded -> R.string.usage_hero_exceeded
                        else -> R.string.usage_hero_left
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onOpenQuota)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.usage_hero_edit), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (status == null) {
            Text(stringResource(R.string.quota_summary_none_body), style = MaterialTheme.typography.bodyMedium)
            return@HeroCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                (if (status.isExceeded) status.exceededBytes else status.remainingBytes).toPlanSize(),
                style = MaterialTheme.typography.displaySmall.numeric().copy(fontSize = 54.sp, lineHeight = 58.sp),
            )
            Text(
                stringResource(R.string.usage_hero_of_short, status.quota.limitBytes.toPlanSize()),
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 8.dp, bottom = 9.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(status.usedRatio.coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.usage_hero_used, status.usedBytes.toPlanSize(), (status.usedRatio * 100).roundToInt()),
                style = MaterialTheme.typography.bodySmall.numeric(),
                color = LocalContentColor.current.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(
                    R.string.usage_hero_days_end,
                    pluralStringResource(R.plurals.days, period.remainingDays, period.remainingDays),
                    formatDayMonth(period.nextStartMillis),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.9f),
            )
        }
    }
}

/**
 * Wi-Fi et mobile en deux tuiles côte à côte (sarcelle et lavande), puis ce que le Wi-Fi a fait
 * économiser. Remplace la carte « Wi-Fi vs données mobiles » et la carte d'économies.
 */
@Composable
internal fun SplitTiles(
    wifiBytes: Long,
    mobileBytes: Long,
    mobileNote: String?,
    generation: NetworkGeneration?,
    pricePerGb: PricePerGb?,
    onShowNetwork: (() -> Unit)?,
    onSetPrice: () -> Unit,
) {
    val total = (wifiBytes + mobileBytes).coerceAtLeast(1L)
    val wifiPercent = (wifiBytes * 100f / total).roundToInt()
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageTile(
                icon = Icons.Rounded.Wifi,
                label = stringResource(R.string.usage_tile_wifi, wifiPercent),
                value = if (wifiBytes > 0) wifiBytes.toPlanSize() else stringResource(R.string.usage_tile_no_wifi),
                sub = stringResource(R.string.usage_tile_wifi_sub),
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
            )
            UsageTile(
                icon = Icons.Rounded.SignalCellularAlt,
                label = stringResource(R.string.usage_tile_mobile, 100 - wifiPercent) + (generation?.let { " · ${it.label}" } ?: ""),
                value = mobileBytes.toPlanSize(),
                sub = mobileNote ?: stringResource(R.string.usage_tile_mobile_sub),
                container = colors.tertiaryContainer,
                content = colors.onTertiaryContainer,
            )
        }
        DataKeeperCard(contentPadding = 16.dp, onClick = if (pricePerGb == null) onSetPrice else null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Rounded.Savings, size = 40.dp, containerColor = colors.secondaryContainer, contentColor = DataColors.wifi)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.usage_savings_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (pricePerGb != null) {
                            stringResource(R.string.usage_savings_sub, wifiBytes.toPlanSize(), formatDinars(pricePerGb.millimes.toLong()))
                        } else {
                            stringResource(R.string.usage_savings_none)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    pricePerGb?.let { formatDinarsRounded(it.costMillimes(wifiBytes)) } ?: "—",
                    style = MaterialTheme.typography.titleLarge.numeric(),
                    color = DataColors.wifi,
                )
            }
        }
        if (onShowNetwork != null) {
            Text(
                stringResource(R.string.usage_show_network),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onShowNetwork)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun RowScope.UsageTile(icon: ImageVector, label: String, value: String, sub: String, container: Color, content: Color) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
        Text(value, style = MaterialTheme.typography.headlineSmall.numeric(), color = content, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 2)
    }
}

/** Premier jour d'un forfait : une invite plutôt que des chiffres vides (« 0 o », courbe plate). */
@Composable
internal fun FirstDayCard() {
    val colors = MaterialTheme.colorScheme
    val wifi = DataColors.wifi
    val mobile = DataColors.mobile
    DataKeeperCard(contentPadding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.size(width = 160.dp, height = 90.dp)) {
                val w = size.width / 160f
                val r = CornerRadius(7.dp.toPx())
                drawRoundRect(mobile.copy(alpha = 0.35f), Offset(14 * w, 52 * w), Size(22 * w, 34 * w), r)
                drawRoundRect(wifi.copy(alpha = 0.35f), Offset(46 * w, 36 * w), Size(22 * w, 50 * w), r)
                drawRoundRect(mobile.copy(alpha = 0.35f), Offset(78 * w, 58 * w), Size(22 * w, 28 * w), r)
                drawRoundRect(colors.primary, Offset(110 * w, 26 * w), Size(22 * w, 60 * w), r)
                drawCircle(colors.primary, radius = 6.dp.toPx(), center = Offset(121 * w, 12 * w))
            }
            Text(stringResource(R.string.usage_first_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.usage_first_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
