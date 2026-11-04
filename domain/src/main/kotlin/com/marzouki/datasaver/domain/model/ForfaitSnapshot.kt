package com.marzouki.datasaver.domain.model

/**
 * Forfait renvoyé par une API forfait (serveur de l'opérateur, ou un serveur maison en démo) :
 * taille, data restante, validité et prix. Sert à pré-remplir l'écran de configuration au lieu
 * de tout saisir à la main. Les champs facultatifs manquent quand l'API ne les fournit pas.
 */
data class ForfaitSnapshot(
    val limitBytes: Long,
    /** Data restante annoncée ; null quand l'API ne la donne pas (forfait tout juste renouvelé). */
    val remainingBytes: Long? = null,
    /** Validité en jours ; null pour un abonnement qui se renouvelle chaque mois à date fixe. */
    val validityDays: Int? = null,
    /** Prix payé pour ce forfait, en millimes (28 500 = 28,5 DT) ; null si l'API ne le donne pas. */
    val priceMillimes: Long? = null,
) {
    init {
        require(limitBytes > 0) { "Le forfait doit être positif" }
        require(remainingBytes == null || remainingBytes in 0..limitBytes) { "Le solde dépasse le forfait" }
        require(validityDays == null || validityDays in 1..MonthPeriod.MAX_VALIDITY_DAYS) { "Validité hors bornes" }
        require(priceMillimes == null || priceMillimes > 0) { "Le prix doit être positif" }
    }
}
