package com.marzouki.datasaver.domain.enums

/** Tendance de la fin de cycle par rapport au forfait. */
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
