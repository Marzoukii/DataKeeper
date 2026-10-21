package com.marzouki.datasaver.ui.components

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val NBSP = ' '
private val UNITS = listOf("o", "ko", "Mo", "Go", "To")

/** Taille découpée en valeur et unité, pour styliser l'unité à part ("3,2" + "Go"). */
data class ReadableSize(val value: String, val unit: String) {
    override fun toString(): String = "$value$NBSP$unit"
}

/**
 * Taille en unités décimales (1 Go = 10⁹ octets, comme les forfaits), à la française :
 * "3,2 Go", "160 Mo", "21,6 Go", "5 Go".
 */
fun Long.toReadableSizeParts(): ReadableSize {
    var value = coerceAtLeast(0L).toDouble()
    var unit = 0
    // 999,96 Mo s'afficherait "1000 Mo" : on passe à l'unité suivante avant l'arrondi.
    while (value >= 999.5 && unit < UNITS.lastIndex) {
        value /= 1000
        unit++
    }
    val pattern = if (unit == 0 || value >= 100) "0" else "0.#"
    val format = DecimalFormat(pattern, DecimalFormatSymbols(Locale.FRANCE)).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    return ReadableSize(format.format(value), UNITS[unit])
}

fun Long.toReadableSize(): String = toReadableSizeParts().toString()

/** "3,2 / 5 Go" quand les unités sont identiques, sinon "800 Mo / 5 Go". */
fun formatUsedOf(usedBytes: Long, limitBytes: Long): String {
    val used = usedBytes.toReadableSizeParts()
    val limit = limitBytes.toReadableSizeParts()
    return if (used.unit == limit.unit) "${used.value} / $limit" else "$used / $limit"
}

/** Montant en dinars tunisiens : "36,800 DT", "2 DT" quand il n'y a pas de millimes. */
fun formatDinars(millimes: Long): String {
    val pattern = if (millimes % 1_000 == 0L) "#,##0" else "#,##0.000"
    val dinars = DecimalFormat(pattern, DecimalFormatSymbols(Locale.FRANCE)).format(millimes / 1_000.0)
    return "$dinars${NBSP}DT"
}

/** "Octobre 2026". */
fun formatMonthYear(millis: Long): String =
    SimpleDateFormat("LLLL yyyy", Locale.FRANCE).format(Date(millis))
        .replaceFirstChar { it.titlecase(Locale.FRANCE) }

/** "1er novembre", "15 novembre". */
fun formatDayMonth(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMMM", Locale.FRANCE).format(Date(millis))
    return "${formatDayOrdinal(day)}$NBSP$month"
}

/** "1er", "2", "15". */
fun formatDayOrdinal(day: Int): String = if (day == 1) "1er" else day.toString()

/** Initiale du jour de la semaine : L M M J V S D. */
fun dayInitial(millis: Long): String {
    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
    return when (dayOfWeek) {
        Calendar.MONDAY -> "L"
        Calendar.TUESDAY -> "M"
        Calendar.WEDNESDAY -> "M"
        Calendar.THURSDAY -> "J"
        Calendar.FRIDAY -> "V"
        Calendar.SATURDAY -> "S"
        else -> "D"
    }
}

/** "lundi". */
fun dayName(millis: Long): String = SimpleDateFormat("EEEE", Locale.FRANCE).format(Date(millis))
