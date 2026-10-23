package com.marzouki.datasaver.domain.model

import kotlin.math.roundToLong

/**
 * Prévision de fin de cycle : la data déjà décomptée du forfait, plus le rythme moyen réellement
 * mesuré depuis le début du cycle prolongé sur les jours restants.
 */
data class QuotaForecast(
    val quota: DataQuota,
    val period: MonthPeriod,
    /**
     * Data mobile décomptée chaque jour écoulé du cycle, du premier jour à aujourd'hui compris.
     * Recalée sur le solde opérateur, elle peut porter sur ses premiers jours ce qui était parti
     * du forfait avant le relevé.
     */
    val dailyMobileBytes: List<Long>,
    /**
     * Data mobile réellement mesurée par Android sur les mêmes jours, sans recalage : elle seule
     * donne le rythme de consommation.
     */
    val measuredDailyBytes: List<Long> = dailyMobileBytes,
) {
    /** Data mobile cumulée à la fin de chaque jour écoulé. */
    val cumulativeBytes: List<Long> = dailyMobileBytes.runningReduce { total, day -> total + day }

    val usedBytes: Long get() = cumulativeBytes.lastOrNull() ?: 0L

    private val elapsedDays: Int get() = dailyMobileBytes.size.coerceAtLeast(1)

    /** Jours restants du cycle, aujourd'hui compris (au moins 1). */
    private val daysLeftIncludingToday: Int get() = (period.totalDays - elapsedDays + 1).coerceAtLeast(1)

    /**
     * Data mobile estimée le dernier jour du cycle. Le solde déjà parti avant un relevé opérateur
     * n'est compté qu'une fois : l'extrapoler comme un rythme donnerait des fins de cycle absurdes
     * (9 Go recalés au 3ᵉ jour ne font pas 93 Go en fin de mois).
     */
    val projectedBytes: Long
        get() {
            val daysAfterToday = (period.totalDays - elapsedDays).coerceAtLeast(0)
            return usedBytes + measuredDailyBytes.sum() * daysAfterToday / measuredDailyBytes.size.coerceAtLeast(1)
        }

    /** Data mobile mesurée aujourd'hui. */
    val todayBytes: Long get() = measuredDailyBytes.lastOrNull() ?: 0L

    /** Forfait décompté avant aujourd'hui. */
    private val usedBeforeTodayBytes: Long get() = (usedBytes - todayBytes).coerceAtLeast(0)

    /**
     * Budget du jour : ce qui restait du forfait ce matin, réparti également sur les jours restants
     * aujourd'hui compris. Le tenir chaque jour fait finir le cycle pile dans le forfait.
     */
    val dailyBudgetBytes: Long
        get() = (quota.limitBytes - usedBeforeTodayBytes).coerceAtLeast(0) / daysLeftIncludingToday

    val outlook: ForecastOutlook
        get() = when {
            usedBytes >= quota.limitBytes -> ForecastOutlook.EXCEEDED
            projectedBytes > quota.limitBytes -> ForecastOutlook.OVER_QUOTA
            projectedBytes * 100 > quota.limitBytes * quota.alertThresholdPercent -> ForecastOutlook.TIGHT
            else -> ForecastOutlook.COMFORTABLE
        }

    /**
     * Data mobile à ne pas dépasser par jour, aujourd'hui compris, d'ici la fin du cycle : de quoi
     * finir à [MARGIN_PERCENT] % du forfait quand il tient, juste dans le forfait quand il est menacé
     * (ou que la marge est déjà mangée). Dans ce dernier cas, c'est le [dailyBudgetBytes].
     */
    val safeDailyBytes: Long
        get() {
            if (outlook == ForecastOutlook.EXCEEDED) return 0L
            val margin = quota.limitBytes / 100 * MARGIN_PERCENT
            val target = if (outlook == ForecastOutlook.OVER_QUOTA || usedBytes >= margin) quota.limitBytes else margin
            return (target - usedBeforeTodayBytes).coerceAtLeast(0) / daysLeftIncludingToday
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
