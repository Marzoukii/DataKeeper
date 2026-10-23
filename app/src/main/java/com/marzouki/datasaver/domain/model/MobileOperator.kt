package com.marzouki.datasaver.domain.model

import com.marzouki.datasaver.domain.enums.TunisianOperator

/** Opérateur qui fournit la data mobile ; [name] est celui des opérateurs tunisiens quand le code est reconnu. */
data class MobileOperator(val name: String, val mccMnc: String?) {
    /** Identifie l'opérateur d'une SIM à l'autre : son code MCC/MNC, à défaut son nom. */
    val key: String get() = mccMnc ?: name

    /** L'opérateur tunisien de la SIM ; null pour une SIM étrangère ou un code inconnu. */
    val tunisian: TunisianOperator? get() = TunisianOperator.fromMccMnc(mccMnc)

    companion object {
        /** Null si la SIM ne donne ni code reconnu ni nom. */
        fun from(mccMnc: String?, simName: String?): MobileOperator? {
            val code = mccMnc?.takeIf { it.isNotBlank() }
            val name = TunisianOperator.fromMccMnc(code)?.displayName
                ?: simName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            return MobileOperator(name, code)
        }
    }
}
