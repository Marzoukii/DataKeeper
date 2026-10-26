package com.marzouki.datasaver

import org.junit.rules.ExternalResource
import java.util.Locale

/**
 * Fixe la langue de l'appli pendant un test, puis rétablit celle de la machine : les formats
 * (« 48,6 », « 3,2 Go ») suivent la locale par défaut, comme sur le téléphone.
 */
class LocaleRule(private val locale: Locale = Locale.FRANCE) : ExternalResource() {
    private lateinit var previous: Locale

    override fun before() {
        previous = Locale.getDefault()
        Locale.setDefault(locale)
    }

    override fun after() {
        Locale.setDefault(previous)
    }
}
