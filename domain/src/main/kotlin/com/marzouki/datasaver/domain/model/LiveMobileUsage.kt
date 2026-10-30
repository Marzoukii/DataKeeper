package com.marzouki.datasaver.domain.model

/**
 * Data mobile du cycle tenue à jour entre deux lectures de l'historique d'Android, qui n'est mis à
 * jour que toutes les quelques dizaines de minutes : pendant un partage de connexion, le total et le
 * quota resteraient figés. Les compteurs du téléphone, eux, bougent en direct : ce qu'ils ont avancé
 * depuis l'ancre (un total lu dans l'historique et le compteur au même instant) s'y ajoute.
 *
 * L'ancre ne suit l'historique que lorsqu'il a rattrapé l'estimation : relu encore en retard, il ne
 * fait pas reculer le chiffre affiché.
 */
data class LiveMobileUsage(
    /** Début du cycle mesuré : un autre cycle repart d'une nouvelle ancre. */
    val periodStartMillis: Long,
    /** Data mobile du cycle à la dernière lecture de l'historique. */
    val historyBytes: Long,
    val anchorHistoryBytes: Long,
    val anchorCounterBytes: Long,
    /** Dernière valeur lue du compteur du téléphone. */
    val counterBytes: Long,
) {
    /** Ce que l'historique n'a pas encore compté, à ajouter au total et au quota du cycle. */
    val extraBytes: Long
        get() = (anchorHistoryBytes + (counterBytes - anchorCounterBytes) - historyBytes).coerceAtLeast(0)

    /** Nouveau relevé du compteur ; reparti en arrière (redémarrage), il devient la nouvelle ancre. */
    fun withCounter(counterBytes: Long): LiveMobileUsage =
        if (counterBytes < anchorCounterBytes) anchoredAt(periodStartMillis, historyBytes, counterBytes)
        else copy(counterBytes = counterBytes)

    companion object {
        fun anchoredAt(periodStartMillis: Long, historyBytes: Long, counterBytes: Long) =
            LiveMobileUsage(periodStartMillis, historyBytes, historyBytes, counterBytes, counterBytes)
    }
}

/** Historique relu ([historyBytes] pour le cycle débutant à [periodStartMillis]) quand le compteur vaut [counterBytes]. */
fun LiveMobileUsage?.withHistory(periodStartMillis: Long, historyBytes: Long, counterBytes: Long): LiveMobileUsage {
    val fresh = LiveMobileUsage.anchoredAt(periodStartMillis, historyBytes, counterBytes)
    if (this == null || this.periodStartMillis != periodStartMillis || counterBytes < anchorCounterBytes) return fresh
    val estimate = anchorHistoryBytes + (counterBytes - anchorCounterBytes)
    return if (historyBytes >= estimate) fresh else copy(historyBytes = historyBytes, counterBytes = counterBytes)
}
