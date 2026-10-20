package com.marzouki.datasaver.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.marzouki.datasaver.ui.theme.DarkError
import com.marzouki.datasaver.ui.theme.DarkOnSurface
import com.marzouki.datasaver.ui.theme.DarkSurfaceVariant
import com.marzouki.datasaver.ui.theme.DarkTertiary
import com.marzouki.datasaver.ui.theme.LightError
import com.marzouki.datasaver.ui.theme.LightOnSurface
import com.marzouki.datasaver.ui.theme.LightSurfaceVariant
import com.marzouki.datasaver.ui.theme.LightTertiary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Version bitmap de [com.marzouki.datasaver.ui.components.QuotaRing] : Glance ne sait pas dessiner d'arc.
 * Les couleurs suivent le thème clair/sombre au moment du rendu du widget.
 */
internal fun quotaRingBitmap(
    context: Context,
    sizeDp: Int,
    strokeDp: Float,
    ratio: Float,
    markerFraction: Float?,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).roundToInt()
    val strokePx = strokeDp * density
    val dark = context.isNightMode

    val track = if (dark) DarkSurfaceVariant else LightSurfaceVariant
    val arc = when {
        ratio >= 1f -> if (dark) DarkError else LightError
        else -> if (dark) DarkTertiary else LightTertiary
    }
    val markerColor = if (dark) DarkOnSurface else LightOnSurface

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
    }
    val inset = strokePx / 2
    val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)

    paint.color = track.toArgb()
    canvas.drawArc(bounds, 0f, 360f, false, paint)

    val sweep = ratio.coerceIn(0f, 1f) * 360f
    if (sweep > 0f) {
        paint.color = arc.toArgb()
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(bounds, -90f, sweep, false, paint)
    }

    if (markerFraction != null) {
        val angle = Math.toRadians(-90.0 + 360.0 * markerFraction)
        val radius = bounds.width() / 2
        val center = sizePx / 2f
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        paint.color = markerColor.toArgb()
        paint.strokeWidth = (strokePx / 4).coerceAtLeast(2 * density)
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(
            center + dx * (radius - inset), center + dy * (radius - inset),
            center + dx * (radius + inset), center + dy * (radius + inset),
            paint,
        )
    }
    return bitmap
}
