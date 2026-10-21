package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.ui.theme.DataColors

private val ChartHeight = 132.dp
private val LabelGap = 4.dp

/**
 * Courbe de la data mobile cumulée depuis le début du cycle (trait plein, aire orange), prolongée
 * en pointillés jusqu'à la [projectedBytes] de fin de cycle. Le forfait est la ligne rouge en tirets,
 * le seuil d'alerte la ligne pointillée grise.
 */
@Composable
fun ForecastChart(
    cumulativeBytes: List<Long>,
    totalDays: Int,
    projectedBytes: Long,
    limitBytes: Long,
    alertPercent: Int,
    startLabel: String,
    endLabel: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val mobile = DataColors.mobile
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = colors.onSurfaceVariant)
    val todayStyle = labelStyle.copy(color = colors.onSurface)
    val limitText = stringResource(R.string.forecast_chart_limit, limitBytes.toReadableSize())
    val alertText = stringResource(R.string.percent, alertPercent)
    val todayText = stringResource(R.string.forecast_chart_today)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val gap = LabelGap.toPx()
        val limitLabel = measurer.measure(limitText, labelStyle)
        val alertLabel = measurer.measure(alertText, labelStyle)
        val startText = measurer.measure(startLabel, labelStyle)
        val endText = measurer.measure(endLabel, labelStyle)
        val today = measurer.measure(todayText, todayStyle)

        val top = limitLabel.size.height + gap
        val bottom = size.height - today.size.height - gap
        val plotHeight = bottom - top
        val used = cumulativeBytes.lastOrNull() ?: 0L
        val maxBytes = maxOf(limitBytes, projectedBytes, used, 1L).toFloat()
        fun y(bytes: Long) = bottom - plotHeight * (bytes / maxBytes)
        // Chaque point est la fin d'une journée du cycle.
        fun x(daysElapsed: Int) = size.width * daysElapsed / totalDays.coerceAtLeast(1)

        // Forfait et seuil d'alerte.
        val limitY = y(limitBytes)
        drawLine(
            colors.error, Offset(0f, limitY), Offset(size.width, limitY),
            strokeWidth = 1.5.dp.toPx(), pathEffect = dashes(4f, 4f),
        )
        drawText(limitLabel, topLeft = Offset(0f, limitY - limitLabel.size.height - gap / 2))
        val alertY = y(limitBytes / 100 * alertPercent)
        drawLine(
            colors.outlineVariant, Offset(0f, alertY), Offset(size.width, alertY),
            strokeWidth = 1.dp.toPx(), pathEffect = dashes(2f, 4f),
        )
        drawText(alertLabel, topLeft = Offset(size.width - alertLabel.size.width, alertY + gap / 2))

        // Consommation réelle : aire puis trait.
        val points = listOf(Offset(0f, bottom)) +
            cumulativeBytes.mapIndexed { index, bytes -> Offset(x(index + 1), y(bytes)) }
        val todayPoint = points.last()
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(todayPoint.x, bottom)
            close()
        }
        drawPath(area, colors.tertiaryContainer)
        drawPath(
            line, mobile,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Projection jusqu'à la fin du cycle.
        drawLine(
            mobile, todayPoint, Offset(size.width, y(projectedBytes)),
            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashes(2f, 6f),
        )
        drawCircle(colors.surface, radius = 7.dp.toPx(), center = todayPoint)
        drawCircle(mobile, radius = 5.dp.toPx(), center = todayPoint)

        drawLine(colors.outlineVariant, Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = 1.dp.toPx())

        // Dates : « Aujourd'hui » sous le point, début et fin seulement s'ils ne le chevauchent pas.
        val labelY = bottom + gap
        val todayLeft = (todayPoint.x - today.size.width / 2f).coerceIn(0f, size.width - today.size.width)
        drawText(today, topLeft = Offset(todayLeft, labelY))
        if (startText.size.width + gap < todayLeft) drawText(startText, topLeft = Offset(0f, labelY))
        val endLeft = size.width - endText.size.width
        if (todayLeft + today.size.width + gap < endLeft) drawText(endText, topLeft = Offset(endLeft, labelY))
    }
}

private fun DrawScope.dashes(onDp: Float, offDp: Float): PathEffect =
    PathEffect.dashPathEffect(floatArrayOf(onDp.dp.toPx(), offDp.dp.toPx()))
