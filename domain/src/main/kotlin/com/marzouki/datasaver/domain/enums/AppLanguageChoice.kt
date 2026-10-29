package com.marzouki.datasaver.domain.enums

/** Langue choisie dans l'appli ; [SYSTEM] suit celle du téléphone. [tag] est l'étiquette BCP 47. */
enum class AppLanguageChoice(val tag: String?) {
    SYSTEM(null),
    FRENCH("fr"),
    ENGLISH("en"),
    ARABIC("ar"),
    ;

    companion object {
        /** Langue de l'appli d'après son étiquette (« fr-TN » → français) ; une langue non proposée suit le téléphone. */
        fun fromTag(tag: String?): AppLanguageChoice {
            val language = tag?.substringBefore('-')?.lowercase() ?: return SYSTEM
            return entries.firstOrNull { it.tag == language } ?: SYSTEM
        }
    }
}
