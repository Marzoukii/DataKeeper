package com.marzouki.datasaver.domain.model

import com.marzouki.datasaver.domain.enums.CycleOutcome
import java.math.RoundingMode

/** Prix d'un Go de data mobile en millimes (1 DT = 1 000 millimes), tel que saisi par l'utilisateur. */
@JvmInline
value class PricePerGb(val millimes: Int) {
    init {
        require(millimes in 1..MAX_MILLIMES) { "Prix du Go hors bornes" }
    }

    /** Ce qu'aurait coûté [bytes] de data mobile, en millimes arrondis. */
    fun costMillimes(bytes: Long): Long = (bytes.coerceAtLeast(0L) * millimes + BYTES_PER_GB / 2) / BYTES_PER_GB

    /** Prix d'un forfait de [planBytes] à ce prix du Go : 1,120 DT × 25 Go → 28 DT. */
    fun planMillimes(planBytes: Long): Long = costMillimes(planBytes)

    companion object {
        const val MILLIMES_PER_DINAR = 1_000
        const val MAX_MILLIMES = 100 * MILLIMES_PER_DINAR
        private const val BYTES_PER_GB = 1_000_000_000L

        /** Un forfait au-delà ne peut pas être une saisie sérieuse. */
        const val MAX_PLAN_MILLIMES = 10_000L * MILLIMES_PER_DINAR

        /** "2", "2,5" ou "1.250" DT → prix ; null si invalide. */
        fun parse(input: String): PricePerGb? {
            val millimes = parseMillimes(input) ?: return null
            return if (millimes in 1..MAX_MILLIMES.toLong()) PricePerGb(millimes.toInt()) else null
        }

        /** "28" ou "4,8" DT payés pour un forfait → millimes ; null si invalide. */
        fun parsePlanPrice(input: String): Long? = parseMillimes(input)?.takeIf { it in 1..MAX_PLAN_MILLIMES }

        /**
         * Prix payé pour un forfait de [planBytes], ramené au Go : 28 DT pour 25 Go → 1,120 DT le Go,
         * 4,8 DT pour 1,4 Go → 3,429 DT. Null si le Go qui en résulte sort des bornes.
         */
        fun ofPlan(planMillimes: Long, planBytes: Long): PricePerGb? {
            if (planBytes <= 0 || planMillimes !in 1..MAX_PLAN_MILLIMES) return null
            val millimes = (planMillimes * BYTES_PER_GB + planBytes / 2) / planBytes
            return if (millimes in 1..MAX_MILLIMES.toLong()) PricePerGb(millimes.toInt()) else null
        }

        fun fromPlan(planPriceInput: String, planBytes: Long): PricePerGb? =
            parsePlanPrice(planPriceInput)?.let { ofPlan(it, planBytes) }

        private fun parseMillimes(input: String): Long? {
            val dinars = input.trim().removeSuffix("DT").trim().replace(',', '.').toBigDecimalOrNull() ?: return null
            return runCatching { dinars.movePointRight(3).setScale(0, RoundingMode.HALF_UP).longValueExact() }.getOrNull()
        }
    }
}

/**
 * Série de cycles terminés sans dépasser le quota, du plus récent au plus ancien.
 * L'historique des forfaits n'est pas conservé : chaque cycle passé est comparé au quota actuel.
 */
data class QuotaStreak(
    /** Cycles consécutifs sous le quota, en partant du dernier terminé. */
    val months: Int,
    /** Derniers cycles terminés, du plus ancien au plus récent. */
    val recentOutcomes: List<CycleOutcome>,
) {
    companion object {
        /** Cycles affichés sur la carte, en plus du cycle en cours. */
        const val RECENT_CYCLES = 3

        /** Au-delà, Android n'a généralement plus les statistiques réseau. */
        const val MAX_CYCLES = 12

        /** [outcomes] du dernier cycle terminé au plus ancien. */
        fun from(outcomes: List<CycleOutcome>): QuotaStreak = QuotaStreak(
            months = outcomes.takeWhile { it == CycleOutcome.WITHIN_QUOTA }.size,
            recentOutcomes = outcomes.take(RECENT_CYCLES).reversed(),
        )
    }
}
