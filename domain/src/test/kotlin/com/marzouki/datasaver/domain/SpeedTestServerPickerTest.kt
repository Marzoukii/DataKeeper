package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.GeoPoint
import com.marzouki.datasaver.domain.model.SpeedTestCandidate
import com.marzouki.datasaver.domain.model.SpeedTestServer
import com.marzouki.datasaver.domain.model.SpeedTestServerPicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedTestServerPickerTest {

    private val tunis = GeoPoint(36.8065, 10.1815)
    private val ooredooSousse = candidate("OOREDOO 5G", "Sousse", 35.8256, 10.6084, api = 5.0)
    private val ttTunis = candidate("Tunisie Telecom", "Tunis", 36.8008, 10.1800, api = 120.0)
    private val ationTunis = candidate("Agence Tunisienne d'Internet", "Kasbah", 36.7990, 10.1700, api = 118.0)
    private val ooredooTunis = candidate("Ooredoo Tunisie", "Tunis", 36.8400, 10.2300, api = 125.0)

    @Test
    fun `la position reelle passe avant celle devinee par l'adresse IP`() {
        val order = SpeedTestServerPicker.order(listOf(ooredooSousse, ationTunis), tunis, operatorName = null)
        assertEquals(listOf(ationTunis, ooredooSousse), order)
    }

    @Test
    fun `parmi les serveurs proches ceux de l'operateur passent devant`() {
        val order = SpeedTestServerPicker.order(
            listOf(ooredooSousse, ationTunis, ooredooTunis, ttTunis),
            tunis,
            operatorName = "Tunisie Telecom",
        )
        assertEquals(ttTunis, order.first())
        assertEquals(ooredooSousse, order.last())
    }

    @Test
    fun `sans position la distance de speedtest net sert`() {
        val order = SpeedTestServerPicker.order(listOf(ttTunis, ooredooSousse), device = null, operatorName = "TT")
        assertEquals(ooredooSousse, order.first())
    }

    @Test
    fun `un serveur prefere est garde s'il repond presque aussi vite`() {
        assertEquals("tt", SpeedTestServerPicker.choose(listOf("tt" to 24L, "sousse" to 18L)))
        assertEquals("sousse", SpeedTestServerPicker.choose(listOf("tt" to 90L, "sousse" to 18L)))
        assertNull(SpeedTestServerPicker.choose(emptyList<Pair<String, Long>>()))
    }

    @Test
    fun `noms d'operateurs reconnus`() {
        assertEquals("tt", SpeedTestServerPicker.operatorKey("TT"))
        assertEquals("tt", SpeedTestServerPicker.operatorKey("Tunisie Télécom"))
        assertEquals("ooredoo", SpeedTestServerPicker.operatorKey("OOREDOO 5G"))
        assertEquals("orange", SpeedTestServerPicker.operatorKey("Orange Tunisie"))
        assertNull(SpeedTestServerPicker.operatorKey(" "))
    }

    private fun candidate(sponsor: String, name: String, lat: Double, lon: Double, api: Double) =
        SpeedTestCandidate(SpeedTestServer(sponsor, name), host = "$name.$sponsor", latitude = lat, longitude = lon, apiDistanceKm = api)
}
