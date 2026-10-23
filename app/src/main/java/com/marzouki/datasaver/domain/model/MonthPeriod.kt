package com.marzouki.datasaver.domain.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Cycle de facturation en cours, de son premier jour à minuit jusqu'à [endMillis] (maintenant).
 * Soit il commence le même jour chaque mois ([current]), soit il dure [validityDays] jours depuis
 * l'achat d'un pass ([pass]) ; [nextStartMillis] est la date de remise à zéro.
 */
data class MonthPeriod(
    val startMillis: Long,
    val endMillis: Long,
    /** Identifiant stable du cycle, ex. "2026-10" (mensuel) ou "2026-10-21+30" (pass). */
    val key: String,
    val nextStartMillis: Long = endMillis,
    /** Durée d'un pass en jours ; null pour un cycle mensuel. */
    val validityDays: Int? = null,
) {
    /** Nombre de jours du cycle (28 à 31 par mois, ou la validité du pass). */
    val totalDays: Int get() = localEpochDay(nextStartMillis) - localEpochDay(startMillis)

    /** Jour du cycle en cours, à partir de 1. */
    val currentDay: Int get() = localEpochDay(endMillis) - localEpochDay(startMillis) + 1

    /** Jours restants après aujourd'hui, au moins 1 pour pouvoir répartir le reste du quota. */
    val remainingDays: Int get() = (totalDays - currentDay).coerceAtLeast(1)

        /** Index (à partir de 0) du jour du cycle contenant [millis]. */
    fun dayIndexOf(millis: Long): Int = localEpochDay(millis) - localEpochDay(startMillis)

    /** Cycle complet qui précède celui-ci, de son premier jour jusqu'à la remise à zéro. */
    fun previous(): MonthPeriod {
        val previous = if (validityDays != null) {
            pass(startMillis - 1, purchaseMillis = startMillis, validityDays = validityDays)
        } else {
            val startDay = Calendar.getInstance().apply { timeInMillis = startMillis }.get(Calendar.DAY_OF_MONTH)
            current(startMillis - 1, startDay)
        }
        return previous.copy(endMillis = previous.nextStartMillis)
    }

    companion object {
        /** Au-delà, certains mois n'auraient pas ce jour. */
        const val MAX_CYCLE_START_DAY = 28

        /** Validité d'un pass acceptée, en jours (220 Mo pour 4 jours, 25 Go pour 30 jours…). */
        const val MAX_VALIDITY_DAYS = 365

        /**
         * Cycle d'un pass de [validityDays] jours acheté le jour de [purchaseMillis] : du 21 octobre
         * pour 30 jours, remise à zéro le 20 novembre. Au-delà, les cycles s'enchaînent avec la même
         * durée, comme si le pass était racheté à chaque fin.
         */
        fun pass(nowMillis: Long, purchaseMillis: Long, validityDays: Int): MonthPeriod {
            val days = validityDays.coerceIn(1, MAX_VALIDITY_DAYS)
            val elapsed = localEpochDay(nowMillis) - localEpochDay(purchaseMillis)
            val start = Calendar.getInstance().apply {
                timeInMillis = purchaseMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Par dates calendaires plutôt qu'en millisecondes : insensible aux changements d'heure.
                add(Calendar.DAY_OF_MONTH, Math.floorDiv(elapsed, days) * days)
            }
            val next = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, days) }
            val key = String.format(
                Locale.ROOT, "%04d-%02d-%02d+%d",
                start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1, start.get(Calendar.DAY_OF_MONTH), days,
            )
            return MonthPeriod(
                startMillis = start.timeInMillis,
                endMillis = nowMillis,
                key = key,
                nextStartMillis = next.timeInMillis,
                validityDays = days,
            )
        }

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
