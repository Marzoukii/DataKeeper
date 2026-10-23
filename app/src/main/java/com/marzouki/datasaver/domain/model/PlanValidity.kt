package com.marzouki.datasaver.domain.model

/**
 * Pass de [days] jours acheté le jour de [purchaseMillis] : Flexi 25 Go valable 30 jours, Flexi
 * 220 Mo valable 4 jours. Remplace le cycle mensuel « le 21 de chaque mois » quand il est connu.
 */
data class PlanValidity(val purchaseMillis: Long, val days: Int) {
    init {
        require(days in 1..MonthPeriod.MAX_VALIDITY_DAYS) { "Validité hors bornes" }
    }

    fun periodAt(nowMillis: Long): MonthPeriod = MonthPeriod.pass(nowMillis, purchaseMillis, days)
}

/**
 * Forfait déjà saisi chez un opérateur, reproposé en un appui au prochain achat. Chaque opérateur
 * a ses offres et ses prix : la liste est celle de l'utilisateur, pas un catalogue figé dans l'appli.
 */
data class SavedPlan(
    /** [MobileOperator.key] de la SIM pour laquelle il a été saisi. */
    val operatorKey: String,
    val limitBytes: Long,
    /** Prix payé, en millimes ; null s'il n'a pas été renseigné. */
    val priceMillimes: Long?,
    /** Validité en jours ; null pour un forfait qui se renouvelle chaque mois à date fixe. */
    val validityDays: Int?,
) {
    /** Même offre : même opérateur, même taille, même durée. Le prix, lui, peut changer (promo). */
    fun isSameOfferAs(other: SavedPlan): Boolean =
        operatorKey == other.operatorKey && limitBytes == other.limitBytes && validityDays == other.validityDays

    companion object {
        /** Forfaits gardés par opérateur, du plus récent au plus ancien. */
        const val MAX_PER_OPERATOR = 6
    }
}
