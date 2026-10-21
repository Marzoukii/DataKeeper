package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric
import kotlin.math.roundToInt

/** Répartition Wi-Fi / mobile du cycle : deux colonnes chiffrées et une barre partagée. */
@Composable
fun UsageSplitCard(
    wifiBytes: Long,
    mobileBytes: Long,
    modifier: Modifier = Modifier,
    /** Opérateur de la SIM des données, accolé au libellé Mobile. */
    operatorName: String? = null,
    /** "4G", "5G"… ; null si inconnu. */
    networkLabel: String? = null,
    /** Non null tant que la permission manque : propose d'afficher la 4G ou la 5G. */
    onShowNetwork: (() -> Unit)? = null,
) {
    val total = wifiBytes + mobileBytes
    val wifiPercent = if (total == 0L) 0 else (wifiBytes * 100f / total).roundToInt()
    val mobilePercent = if (total == 0L) 0 else 100 - wifiPercent

    DataKeeperCard(modifier, shape = MaterialTheme.shapes.extraLarge) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.usage_split_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.usage_total, total.toReadableSize()),
                style = MaterialTheme.typography.bodySmall.numeric(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row {
            UsageColumn(
                icon = Icons.Rounded.Wifi,
                label = stringResource(R.string.usage_wifi),
                color = DataColors.wifi,
                bytes = wifiBytes,
                percent = wifiPercent,
                modifier = Modifier.weight(1f),
            )
            UsageColumn(
                icon = Icons.Rounded.SignalCellularAlt,
                label = listOfNotNull(operatorName, networkLabel).takeIf { it.isNotEmpty() }
                    ?.let { stringResource(R.string.usage_mobile_operator, it.joinToString(" · ")) }
                    ?: stringResource(R.string.usage_mobile),
                color = DataColors.mobile,
                bytes = mobileBytes,
                percent = mobilePercent,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
        SplitBar(wifiBytes, mobileBytes)
        if (onShowNetwork != null) {
            TextButton(
                onClick = onShowNetwork,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(stringResource(R.string.usage_show_network), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun UsageColumn(
    icon: ImageVector,
    label: String,
    color: Color,
    bytes: Long,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val size = bytes.toReadableSizeParts()
    val unitStyle = MaterialTheme.typography.titleMedium.numeric()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            buildAnnotatedString {
                append(size.value)
                withStyle(SpanStyle(fontSize = unitStyle.fontSize, fontWeight = unitStyle.fontWeight)) {
                    append(" ${size.unit}")
                }
            },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.usage_share_of_total, percent),
            style = MaterialTheme.typography.bodySmall.numeric(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Barre de 12 dp en deux segments arrondis séparés de 3 dp, proportionnels aux volumes. */
@Composable
private fun SplitBar(wifiBytes: Long, mobileBytes: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (wifiBytes == 0L && mobileBytes == 0L) {
            Segment(MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
        }
        if (wifiBytes > 0) Segment(DataColors.wifi, Modifier.weight(wifiBytes.toFloat()))
        if (mobileBytes > 0) Segment(DataColors.mobile, Modifier.weight(mobileBytes.toFloat()))
    }
}

@Composable
private fun Segment(color: Color, modifier: Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(color),
    )
}

@ThemePreviews
@Composable
private fun UsageSplitCardPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageSplitCard(
                PreviewData.usage.wifiBytes,
                PreviewData.usage.mobileBytes,
                operatorName = "Tunisie Telecom",
                networkLabel = "5G",
            )
            UsageSplitCard(PreviewData.usage.wifiBytes, PreviewData.usage.mobileBytes, operatorName = "Orange", onShowNetwork = {})
        }
    }
}
