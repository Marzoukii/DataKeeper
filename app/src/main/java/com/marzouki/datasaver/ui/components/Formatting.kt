package com.marzouki.datasaver.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Espace insécable : « 3,2 Go » ne se coupe jamais entre le nombre et l'unité. */
private const val NBSP = ' '

/**
 * Langue d'affichage, celle des textes : français, anglais ou arabe ; toute autre langue du
 * téléphone retombe sur le français, comme `values/strings.xml`. Lue à chaque appel : Android
 * met à jour la locale par défaut quand on change la langue de l'appli.
 */
internal enum class AppLanguage(
    /** Nombres et dates. En arabe : chiffres latins et mois tunisiens (جانفي، فيفري), comme dans les SMS des opérateurs. */
    val locale: Locale,
    val sizeUnits: List<String>,
    val currency: String,
    val today: String,
    val yesterday: String,
    val justNow: String,
    /** « il y a 5 min » ; %d est le nombre de minutes. */
    val minutesAgo: String,
    /** Initiales des jours, du lundi au dimanche ; noms complets en arabe, où une lettre ne se lit pas. */
    val dayInitials: List<String>,
) {
    FR(
        Locale.FRANCE, listOf("o", "ko", "Mo", "Go", "To"), "DT",
        "Aujourd’hui", "Hier", "à l’instant", "il y a %d${NBSP}min",
        listOf("L", "M", "M", "J", "V", "S", "D"),
    ),
    EN(
        Locale.UK, listOf("B", "kB", "MB", "GB", "TB"), "DT",
        "Today", "Yesterday", "just now", "%d${NBSP}min ago",
        listOf("M", "T", "W", "T", "F", "S", "S"),
    ),
    AR(
        Locale.forLanguageTag("ar-TN-u-nu-latn"), listOf("بايت", "كيلو", "ميغا", "جيغا", "تيرا"), "د.ت",
        "اليوم", "أمس", "الآن", "منذ %d${NBSP}د",
        listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"),
    ),
    ;

    companion object {
        fun current(): AppLanguage = when (Locale.getDefault().language) {
            "en" -> EN
            "ar" -> AR
            else -> FR
        }
    }
}

private fun symbols() = DecimalFormatSymbols(AppLanguage.current().locale)

private fun dateFormat(pattern: String) = SimpleDateFormat(pattern, AppLanguage.current().locale)

/** Taille découpée en valeur et unité, pour styliser l'unité à part ("3,2" + "Go"). */
data class ReadableSize(val value: String, val unit: String) {
    override fun toString(): String = "$value$NBSP$unit"
}

/**
 * Taille en unités décimales (1 Go = 10⁹ octets, comme les forfaits), dans la langue de l'appli :
 * "3,2 Go", "160 Mo", "3.2 GB", "3,2 جيغا".
 */
fun Long.toReadableSizeParts(): ReadableSize {
    val units = AppLanguage.current().sizeUnits
    var value = coerceAtLeast(0L).toDouble()
    var unit = 0
    // 999,96 Mo s'afficherait "1000 Mo" : on passe à l'unité suivante avant l'arrondi.
    while (value >= 999.5 && unit < units.lastIndex) {
        value /= 1000
        unit++
    }
    val pattern = if (unit == 0 || value >= 100) "0" else "0.#"
    val format = DecimalFormat(pattern, symbols()).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    return ReadableSize(format.format(value), units[unit])
}

fun Long.toReadableSize(): String = toReadableSizeParts().toString()

/** "3,2 / 5 Go" quand les unités sont identiques, sinon "800 Mo / 5 Go". */
fun formatUsedOf(usedBytes: Long, limitBytes: Long): String {
    val used = usedBytes.toReadableSizeParts()
    val limit = limitBytes.toReadableSizeParts()
    return if (used.unit == limit.unit) "${used.value} / $limit" else "$used / $limit"
}

/** Montant en dinars tunisiens, sans zéros inutiles : "27,6 DT", "1,25 DT", "2 DT". */
fun formatDinars(millimes: Long): String {
    val dinars = DecimalFormat("#,##0.###", symbols()).format(millimes / 1_000.0)
    return "$dinars$NBSP${AppLanguage.current().currency}"
}

/**
 * Montant des économies arrondi aux 100 millimes et toujours écrit avec trois décimales :
 * "87,800 DT" pour 87,790 ou 87,798 DT, "2,000 DT". Les prix gardent [formatDinars] (1,250 DT).
 */
fun formatDinarsRounded(millimes: Long): String {
    val rounded = BigDecimal.valueOf(millimes, 3).setScale(1, RoundingMode.HALF_UP)
    val dinars = DecimalFormat("#,##0.000", symbols()).format(rounded)
    return "$dinars$NBSP${AppLanguage.current().currency}"
}

/** "Octobre 2026", "October 2026", "أكتوبر 2026". */
fun formatMonthYear(millis: Long): String {
    val locale = AppLanguage.current().locale
    return SimpleDateFormat("LLLL yyyy", locale).format(Date(millis)).replaceFirstChar { it.titlecase(locale) }
}

/** "1er novembre", "15 novembre" ; "1 November" ; "1 نوفمبر". */
fun formatDayMonth(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = dateFormat("MMMM").format(Date(millis))
    // « 1er » ne s'écrit qu'en français : ailleurs, le jour reste un simple nombre devant le mois.
    val dayLabel = if (AppLanguage.current() == AppLanguage.FR) formatDayOrdinal(day) else day.toString()
    return "$dayLabel$NBSP$month"
}

/** Jour du mois en ordinal, pour « le 1er de chaque mois » : "1er", "15" ; "1st", "22nd" ; "1". */
fun formatDayOrdinal(day: Int): String = when (AppLanguage.current()) {
    AppLanguage.FR -> if (day == 1) "1er" else day.toString()
    AppLanguage.EN -> day.toString() + when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    AppLanguage.AR -> day.toString()
}

/** Jour de la semaine sous l'axe : L M M J V S D, M T W T F S S, الإثنين … الأحد. */
fun dayInitial(millis: Long): String {
    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
    // Calendar numérote du dimanche (1) au samedi (7) ; les listes vont du lundi au dimanche.
    return AppLanguage.current().dayInitials[(dayOfWeek + 5) % 7]
}

/** "lundi", "Monday", "الإثنين". */
fun dayName(millis: Long): String = dateFormat("EEEE").format(Date(millis))

/** "31 oct.", "1 nov." : date courte des axes de graphique. */
fun formatShortDate(millis: Long): String = dateFormat("d MMM").format(Date(millis))

/** Débit dans la langue de l'appli : "48,6" ou "48.6" sous 100 Mbit/s, "312" au-delà. */
fun formatMbps(mbps: Double): String {
    val pattern = if (mbps >= 99.95) "0" else "0.0"
    return DecimalFormat(pattern, symbols())
        .apply { roundingMode = RoundingMode.HALF_UP }
        .format(mbps.coerceAtLeast(0.0))
}

/** "Aujourd'hui 09:12", "Hier 18:40", "12 oct. 18:40". */
fun formatDayTime(millis: Long, nowMillis: Long): String {
    val language = AppLanguage.current()
    val time = dateFormat("HH:mm").format(Date(millis))
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        day.isSameDay(today) -> "${language.today} $time"
        day.isSameDay(yesterday) -> "${language.yesterday} $time"
        else -> "${formatShortDate(millis)} $time"
    }
}

/** Heure seule pour aujourd'hui ("14:20"), sinon le jour et l'heure comme [formatDayTime] ("Hier 18:40"). */
fun formatTimeOrDay(millis: Long, nowMillis: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return if (day.isSameDay(today)) dateFormat("HH:mm").format(Date(millis)) else formatDayTime(millis, nowMillis)
}

/** Durée arrondie à la minute : "1 h 10", "50 min", "2 h" ; "1 h 10 min" ; "1 س 10 د". */
fun formatDuration(millis: Long): String {
    val totalMinutes = ((millis + 30_000) / 60_000).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val (h, min) = when (AppLanguage.current()) {
        AppLanguage.AR -> "س" to "د"
        else -> "h" to "min"
    }
    return when {
        hours == 0L -> "$minutes$NBSP$min"
        minutes == 0L -> "$hours$NBSP$h"
        // En français, « 1 h 10 » se lit sans unité de minutes.
        AppLanguage.current() == AppLanguage.FR -> "$hours$NBSP$h$NBSP${"%02d".format(minutes)}"
        else -> "$hours$NBSP$h$NBSP$minutes$NBSP$min"
    }
}

/** "à l'instant", "il y a 5 min", puis l'heure du jour comme [formatDayTime]. */
fun formatElapsed(millis: Long, nowMillis: Long): String {
    val language = AppLanguage.current()
    val minutes = (nowMillis - millis).coerceAtLeast(0L) / 60_000
    return when {
        minutes < 1 -> language.justNow
        minutes < 60 -> language.minutesAgo.format(language.locale, minutes)
        else -> formatDayTime(millis, nowMillis).replaceFirstChar { it.lowercase(language.locale) }
    }
}

private fun Calendar.isSameDay(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
