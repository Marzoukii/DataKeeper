package com.marzouki.datasaver.domain.enums

/** Résultat d'un cycle terminé, comparé au quota actuel. */
enum class CycleOutcome {
    WITHIN_QUOTA,
    EXCEEDED,
    /** Aucun trafic mesuré : Android n'a plus l'historique, ou l'appli n'avait pas l'accès. */
    UNKNOWN,
}
