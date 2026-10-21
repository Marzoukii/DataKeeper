package com.marzouki.datasaver.domain.model

import java.math.RoundingMode

/** Prix d'un Go de data mobile en millimes (1 DT = 1 000 millimes), tel que saisi par l'utilisateur. */
@JvmInline
value class PricePerGb(val millimes: Int) {
    init {
        require(millimes in 1..MAX_MILLIMES) { "Prix du Go hors bornes" }
    }

    /** Ce qu'aurait coûté [bytes] de data mobile, en millimes arrondis. */
    fun costMillimes(bytes: Long): Long = (bytes.coerceAtLeast(0L) * millimes + BYTES_PER_GB / 2) / BYTES_PER_GB

    companion object {
        const val MILLIMES_PER_DINAR = 1_000
        const val MAX_MILLIMES = 100 * MILLIMES_PER_DINAR
        private const val BYTES_PER_GB = 1_000_000_000L

        /** "2", "2,5" ou "1.250" DT → prix ; null si invalide. */
        fun parse(input: String): PricePerGb? {
            val dinars = input.trim().removeSuffix("DT").trim().replace(',', '.').toBigDecimalOrNull() ?: return null
            val millimes = runCatching { dinars.movePointRight(3).setScale(0, RoundingMode.HALF_UP).intValueExact() }
                .getOrNull() ?: return null
            return if (millimes in 1..MAX_MILLIMES) PricePerGb(millimes) else null
        }
    }
}

/** Résultat d'un cycle terminé, comparé au quota actuel. */
enum class CycleOutcome {
    WITHIN_QUOTA,
    EXCEEDED,
    /** Aucun trafic mesuré : Android n'a plus l'historique, ou l'appli n'avait pas l'accès. */
    UNKNOWN,
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
