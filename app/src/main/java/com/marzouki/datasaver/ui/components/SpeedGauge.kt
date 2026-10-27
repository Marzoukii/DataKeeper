package com.marzouki.datasaver.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.ui.theme.DataColors
import kotlin.math.cos
import kotlin.math.sin

/** Débit en haut de l'échelle du compteur, affiché « 100+ ». */
const val SPEED_GAUGE_MAX_MBPS = 100.0

private const val START_ANGLE = 165f
private const val SWEEP_ANGLE = 210f

/**
 * Compteur de vitesse en demi-cercle, gradué de 0 à [SPEED_GAUGE_MAX_MBPS] Mbit/s.
 * Sans mesure ([mbps] nul), l'aiguille repose sur 0 et l'arc reste vide.
 */
@Composable
fun SpeedGauge(
    mbps: Double?,
    color: Color,
    modifier: Modifier = Modifier,
    showScale: Boolean = false,
) {
    val target = ((mbps ?: 0.0) / SPEED_GAUGE_MAX_MBPS).coerceIn(0.0, 1.0).toFloat()
    val fraction by animateFloatAsState(target, label = "gaugeFraction")
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val needleColor = if (mbps == null) MaterialTheme.colorScheme.onSurface else color

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(width = 200.dp, height = 130.dp)) {
            val stroke = 14.dp.toPx()
            val radius = 90.dp.toPx()
            val center = Offset(size.width / 2, 100.dp.toPx())
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(trackColor, START_ANGLE, SWEEP_ANGLE, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (fraction > 0f) {
                drawArc(color, START_ANGLE, SWEEP_ANGLE * fraction, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * fraction).toDouble())
            val needleLength = radius * 0.72f
            val tip = Offset(center.x + needleLength * cos(angle).toFloat(), center.y + needleLength * sin(angle).toFloat())
            drawLine(needleColor, center, tip, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(needleColor, radius = 7.dp.toPx(), center = center)
        }
        if (showScale) {
            Row(Modifier.width(200.dp)) {
                Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f))
                Text("100+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SpeedGaugePreview() {
    PreviewSurface {
        Row(Modifier.fillMaxWidth()) {
            SpeedGauge(mbps = null, color = DataColors.wifi, showScale = true, modifier = Modifier.weight(1f))
            SpeedGauge(mbps = 48.6, color = DataColors.wifi, modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
    }
}
