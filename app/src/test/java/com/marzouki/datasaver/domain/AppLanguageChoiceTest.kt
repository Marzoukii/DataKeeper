package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.enums.AppLanguageChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageChoiceTest {

    @Test
    fun `étiquette de langue, avec ou sans région`() {
        assertEquals(AppLanguageChoice.FRENCH, AppLanguageChoice.fromTag("fr"))
        assertEquals(AppLanguageChoice.ARABIC, AppLanguageChoice.fromTag("ar-TN"))
        assertEquals(AppLanguageChoice.ENGLISH, AppLanguageChoice.fromTag("EN-gb"))
    }

    @Test
    fun `aucune langue ou langue non proposée, celle du téléphone`() {
        assertEquals(AppLanguageChoice.SYSTEM, AppLanguageChoice.fromTag(null))
        assertEquals(AppLanguageChoice.SYSTEM, AppLanguageChoice.fromTag("de-DE"))
    }
}
