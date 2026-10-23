package com.marzouki.datasaver.domain.enums

/** Où en est la configuration du forfait, qui décide de l'écran affiché à la racine. */
enum class PlanStatus {
    /** Jamais configuré : l'écran du forfait s'affiche avant le tableau de bord. */
    MISSING,

    /** La SIM des données n'est plus celle du forfait saisi : il faut saisir celui de la nouvelle. */
    OPERATOR_CHANGED,

    /** Forfait saisi (ou « pas de forfait ») pour la SIM actuelle. */
    CONFIGURED,
}
