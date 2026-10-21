package com.marzouki.datasaver.domain.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Cycle de facturation en cours, de son premier jour à minuit jusqu'à [endMillis] (maintenant).
 * Le cycle commence le même jour chaque mois ; [nextStartMillis] est la date de remise à zéro.
 */
data class MonthPeriod(
    val startMillis: Long,
    val endMillis: Long,
    /** Identifiant stable du cycle (mois de son premier jour), ex. "2026-10". */
    val key: String,
    val nextStartMillis: Long = endMillis,
) {
    /** Nombre de jours du cycle (28 à 31). */
    val totalDays: Int get() = localEpochDay(nextStartMillis) - localEpochDay(startMillis)

    /** Jour du cycle en cours, à partir de 1. */
    val currentDay: Int get() = localEpochDay(endMillis) - localEpochDay(startMillis) + 1

    /** Jours restants après aujourd'hui, au moins 1 pour pouvoir répartir le reste du quota. */
    val remainingDays: Int get() = (totalDays - currentDay).coerceAtLeast(1)

    /** Cycle complet qui précède celui-ci, de son premier jour jusqu'à la remise à zéro. */
    fun previous(): MonthPeriod {
        val startDay = Calendar.getInstance().apply { timeInMillis = startMillis }.get(Calendar.DAY_OF_MONTH)
        val previous = current(startMillis - 1, startDay)
        return previous.copy(endMillis = previous.nextStartMillis)
    }

    companion object {
        /** Au-delà, certains mois n'auraient pas ce jour. */
        const val MAX_CYCLE_START_DAY = 28

        fun current(nowMillis: Long = System.currentTimeMillis(), cycleStartDay: Int = 1): MonthPeriod {
            val startDay = cycleStartDay.coerceIn(1, MAX_CYCLE_START_DAY)
            val start = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                val today = get(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, 1)
                if (today < startDay) add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, startDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val next = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val key = String.format(
                Locale.ROOT, "%04d-%02d",
                start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1,
            )
            return MonthPeriod(
                startMillis = start.timeInMillis,
                endMillis = nowMillis,
                key = key,
                nextStartMillis = next.timeInMillis,
            )
        }

        // Jour calendaire local : insensible aux changements d'heure.
        private fun localEpochDay(millis: Long): Int =
            TimeUnit.MILLISECONDS.toDays(millis + TimeZone.getDefault().getOffset(millis)).toInt()
    }
}
