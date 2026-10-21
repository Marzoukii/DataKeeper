package com.marzouki.datasaver.domain.model

/** Génération du réseau mobile de la SIM des données, telle qu'affichée dans la barre d'état. */
enum class NetworkGeneration(val label: String) {
    G2("2G"),
    G3("3G"),
    G4("4G"),
    G5("5G"),
}

/** Opérateur qui fournit la data mobile ; [name] est celui des opérateurs tunisiens quand le code est reconnu. */
data class MobileOperator(val name: String, val mccMnc: String?) {
    companion object {
        /** Codes MCC/MNC des opérateurs tunisiens (MCC 605). */
        private val TUNISIAN_OPERATORS = mapOf(
            "60501" to "Orange",
            "60502" to "Tunisie Telecom",
            "60503" to "Ooredoo",
        )

        /** Null si la SIM ne donne ni code reconnu ni nom. */
        fun from(mccMnc: String?, simName: String?): MobileOperator? {
            val code = mccMnc?.takeIf { it.isNotBlank() }
            val name = code?.let(TUNISIAN_OPERATORS::get) ?: simName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return MobileOperator(name, code)
        }
    }
}
