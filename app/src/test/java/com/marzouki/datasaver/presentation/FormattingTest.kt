package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.LocaleRule
import com.marzouki.datasaver.ui.components.dayInitial
import com.marzouki.datasaver.ui.components.formatDayMonth
import com.marzouki.datasaver.ui.components.formatDayOrdinal
import com.marzouki.datasaver.ui.components.formatDayTime
import com.marzouki.datasaver.ui.components.formatDinars
import com.marzouki.datasaver.ui.components.formatElapsed
import com.marzouki.datasaver.ui.components.formatMbps
import com.marzouki.datasaver.ui.components.toReadableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Calendar
import java.util.Locale

/** Mêmes valeurs dans les trois langues de l'appli ; toute autre langue retombe sur le français. */
@RunWith(Parameterized::class)
class FormattingTest(private val case: Case) {

    data class Case(
        val locale: Locale,
        val size: String,
        val price: String,
        val mbps: String,
        val dayMonth: String,
        val ordinal: String,
        val today: String,
        val minutesAgo: String,
        val monday: String,
    ) {
        override fun toString() = locale.toLanguageTag()
    }

    @get:Rule
    val rule: RuleChain = RuleChain.outerRule(LocaleRule(case.locale))

    private val nov1 = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 1, 9, 12) }.timeInMillis
    private val monday = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 26) }.timeInMillis

    @Test
    fun `tailles, prix et débit`() {
        assertEquals(case.size, 3_200_000_000L.toReadableSize())
        assertEquals(case.price, formatDinars(27_600))
        assertEquals(case.mbps, formatMbps(48.62))
    }

    @Test
    fun `dates et jours`() {
        assertEquals(case.dayMonth, formatDayMonth(nov1))
        assertEquals(case.ordinal, formatDayOrdinal(1))
        assertEquals(case.today, formatDayTime(nov1, nowMillis = nov1))
        assertEquals(case.minutesAgo, formatElapsed(nov1, nowMillis = nov1 + 5 * 60_000))
        assertEquals(case.monday, dayInitial(monday))
    }

    @Test
    fun `chiffres latins, même en arabe`() {
        // En arabe, pas de chiffres indo-arabes (٠١٢…) : les opérateurs tunisiens écrivent 48,6.
        assertTrue(formatMbps(48.62).none { it in '٠'..'٩' })
        assertTrue(formatDayMonth(nov1).none { it in '٠'..'٩' })
    }

    companion object {
        private const val NBSP = ' '

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf(
            Case(Locale.FRANCE, "3,2${NBSP}Go", "27,6${NBSP}DT", "48,6", "1er${NBSP}novembre", "1er", "Aujourd’hui 09:12", "il y a 5${NBSP}min", "L"),
            Case(Locale.UK, "3.2${NBSP}GB", "27.6${NBSP}DT", "48.6", "1${NBSP}November", "1st", "Today 09:12", "5${NBSP}min ago", "M"),
            Case(Locale.forLanguageTag("ar-TN"), "3,2${NBSP}جيغا", "27,6${NBSP}د.ت", "48,6", "1${NBSP}نوفمبر", "1", "اليوم 09:12", "منذ 5${NBSP}د", "ن"),
            // Langue non traduite : formats français, comme les textes.
            Case(Locale.GERMANY, "3,2${NBSP}Go", "27,6${NBSP}DT", "48,6", "1er${NBSP}novembre", "1er", "Aujourd’hui 09:12", "il y a 5${NBSP}min", "L"),
        )
    }
}
