package com.marzouki.datasaver.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.ui.theme.DataColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anneau de consommation : piste [trackColor], arc [color] partant du haut, repère à
 * [markerFraction] (seuil d'alerte). Au-delà de 100 %, l'arc passe en couleur d'erreur.
 */
@Composable
fun QuotaRing(
    used: Long,
    quota: Long,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 8.dp,
    color: Color = DataColors.mobile,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    markerColor: Color = MaterialTheme.colorScheme.onSurface,
    markerFraction: Float = DataQuota.DEFAULT_ALERT_THRESHOLD_PERCENT / 100f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val ratio = if (quota > 0) used.toFloat() / quota else 0f
    val arcColor = if (quota > 0 && used >= quota) MaterialTheme.colorScheme.error else color
    val sweep by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(durationMillis = 900), label = "quotaRing")

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, -90f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx))
            if (sweep > 0f) {
                drawArc(
                    arcColor, -90f, 360f * sweep, useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Round),
                )
            }
            if (quota > 0) {
                val angle = Math.toRadians(-90.0 + 360.0 * markerFraction)
                val radius = diameter / 2
                val inner = radius - strokePx / 2
                val outer = radius + strokePx / 2
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                drawLine(
                    markerColor,
                    start = center + Offset(dx * inner, dy * inner),
                    end = center + Offset(dx * outer, dy * outer),
                    strokeWidth = (strokePx / 4).coerceAtLeast(2.dp.toPx()),
                    cap = StrokeCap.Round,
                )
            }
        }
        content()
    }
}

@ThemePreviews
@Composable
private fun QuotaRingPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val gb = 1_000_000_000L
            QuotaRing(used = 3_200_000_000L, quota = 5 * gb) {
                Text("64 %", style = MaterialTheme.typography.labelMedium)
            }
            QuotaRing(used = 4_300_000_000L, quota = 5 * gb)
            QuotaRing(used = 5_400_000_000L, quota = 5 * gb)
            QuotaRing(used = 3_200_000_000L, quota = 5 * gb, size = 120.dp, stroke = 12.dp)
        }
    }
}
