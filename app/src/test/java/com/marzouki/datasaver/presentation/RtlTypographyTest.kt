package com.marzouki.datasaver.presentation

import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.ui.theme.RtlTypography
import com.marzouki.datasaver.ui.theme.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RtlTypographyTest {

    private fun styles(typography: androidx.compose.material3.Typography) = with(typography) {
        listOf(
            displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
            titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall,
        )
    }

    @Test
    fun `en arabe, aucun style n'espace ses lettres`() {
        styles(RtlTypography).forEach { assertEquals(0.sp, it.letterSpacing) }
    }

    @Test
    fun `seul l'espacement change, polices et tailles restent`() {
        styles(Typography).zip(styles(RtlTypography)).forEach { (ltr, rtl) ->
            assertEquals(ltr.copy(letterSpacing = 0.sp), rtl)
        }
        // Les titres latins gardent leur espacement resserré.
        assertTrue(Typography.headlineSmall.letterSpacing.value < 0f)
    }
}
