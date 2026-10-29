package com.marzouki.datasaver.domain.enums

/** Opérateurs tunisiens reconnus par leur code MCC/MNC (MCC 605). */
enum class TunisianOperator(val mccMnc: String, val displayName: String) {
    ORANGE("60501", "Orange"),
    TUNISIE_TELECOM("60502", "Tunisie Telecom"),
    OOREDOO("60503", "Ooredoo"),
    ;

    companion object {
        /** Null si le code n'est pas celui d'un opérateur tunisien. */
        fun fromMccMnc(mccMnc: String?): TunisianOperator? = entries.firstOrNull { it.mccMnc == mccMnc }
    }
}
