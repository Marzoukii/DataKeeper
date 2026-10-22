package com.marzouki.datasaver.domain.model

import kotlin.math.roundToLong

/**
 * Prévision de fin de cycle à partir de la data mobile consommée chaque jour depuis le début du
 * cycle, au rythme moyen des jours écoulés.
 */
data class QuotaForecast(
    val quota: DataQuota,
    val period: MonthPeriod,
    /** Data mobile de chaque jour écoulé du cycle, du premier jour à aujourd'hui compris. */
    val dailyMobileBytes: List<Long>,
) {
    /** Data mobile cumulée à la fin de chaque jour écoulé. */
    val cumulativeBytes: List<Long> = dailyMobileBytes.runningReduce { total, day -> total + day }

    val usedBytes: Long get() = cumulativeBytes.lastOrNull() ?: 0L

    /** Data mobile estimée le dernier jour du cycle. */
    val projectedBytes: Long
        get() = usedBytes * period.totalDays / dailyMobileBytes.size.coerceAtLeast(1)

    val outlook: ForecastOutlook
        get() = when {
            usedBytes >= quota.limitBytes -> ForecastOutlook.EXCEEDED
            projectedBytes > quota.limitBytes -> ForecastOutlook.OVER_QUOTA
            projectedBytes * 100 > quota.limitBytes * quota.alertThresholdPercent -> ForecastOutlook.TIGHT
            else -> ForecastOutlook.COMFORTABLE
        }

    /**
     * Data mobile à ne pas dépasser par jour d'ici la fin du cycle : de quoi finir à
     * [MARGIN_PERCENT] % du forfait quand il tient, juste dans le forfait quand il est menacé
     * (ou que la marge est déjà mangée).
     */
    val safeDailyBytes: Long
        get() {
            val margin = quota.limitBytes / 100 * MARGIN_PERCENT
            val target = if (outlook == ForecastOutlook.OVER_QUOTA || usedBytes >= margin) quota.limitBytes else margin
            return (target - usedBytes).coerceAtLeast(0) / period.remainingDays
        }

    companion object {
        const val MARGIN_PERCENT = 95
    }
}

/**
 * Recale la data mobile de chaque jour du cycle pour que son cumul retombe sur les [usedBytes]
 * du forfait. Les jours d'avant le relevé opérateur ([fromDayIndex]) absorbent l'écart au prorata
 * de ce qu'ils ont mesuré, ou le premier jour seul s'ils n'ont rien mesuré.
 */
fun alignDailyToUsed(dailyBytes: List<Long>, usedBytes: Long, fromDayIndex: Int): List<Long> {
    if (dailyBytes.isEmpty()) return dailyBytes
    val split = fromDayIndex.coerceIn(0, dailyBytes.size)
    val after = dailyBytes.subList(split, dailyBytes.size)
    val target = usedBytes - after.sum()
    if (split == 0) {
        // Relevé le premier jour : seul ce jour peut absorber l'écart.
        return listOf((dailyBytes[0] + target).coerceAtLeast(0)) + dailyBytes.drop(1)
    }
    val before = dailyBytes.subList(0, split)
    val beforeTarget = target.coerceAtLeast(0)
    val measured = before.sum()
    val scaled = if (measured == 0L) {
        List(split) { if (it == 0) beforeTarget else 0L }
    } else {
        val ratio = beforeTarget.toDouble() / measured
        val rounded = before.map { (it * ratio).roundToLong() }
        // Le dernier jour reprend l'arrondi pour tomber pile sur la cible.
        rounded.dropLast(1) + (beforeTarget - rounded.dropLast(1).sum()).coerceAtLeast(0)
    }
    return scaled + after
}

enum class ForecastOutlook {
    /** Fin de cycle sous le seuil d'alerte. */
    COMFORTABLE,

    /** Fin de cycle entre le seuil d'alerte et le forfait. */
    TIGHT,

    /** Le forfait sera dépassé avant la fin du cycle. */
    OVER_QUOTA,

    /** Le forfait est déjà dépassé. */
    EXCEEDED,
}
