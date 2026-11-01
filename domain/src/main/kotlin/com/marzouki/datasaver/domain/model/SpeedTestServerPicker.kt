package com.marzouki.datasaver.domain.model

import java.text.Normalizer
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Serveur proposé par speedtest.net : [host] pour le joindre, sa position et la distance estimée par speedtest.net. */
data class SpeedTestCandidate(
    val server: SpeedTestServer,
    val host: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val apiDistanceKm: Double? = null,
)

/** Position de l'appareil, en degrés. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Choix du serveur de mesure. speedtest.net situe l'appareil d'après son adresse IP, souvent loin
 * de la vraie ville (Sousse au lieu de Tunis) : quand la position de l'appareil est connue, la
 * distance est recalculée depuis elle. Parmi les serveurs proches, ceux de l'opérateur de la SIM
 * passent devant, puis le plus proche ; un serveur n'est écarté que s'il répond nettement moins vite.
 */
object SpeedTestServerPicker {

    /** Serveurs à tester, du préféré au moins préféré. */
    fun order(
        candidates: List<SpeedTestCandidate>,
        device: GeoPoint?,
        operatorName: String?,
        limit: Int = LATENCY_CANDIDATES,
    ): List<SpeedTestCandidate> {
        val byDistance = candidates.map { it to distanceKm(it, device) }.sortedBy { it.second }
        val nearest = byDistance.firstOrNull()?.second ?: return emptyList()
        val operator = operatorKey(operatorName)
        val (nearby, far) = byDistance.partition { it.second <= nearest + NEARBY_MARGIN_KM }
        val (ownOperator, others) = nearby.partition { operator != null && operatorKey(it.first.server.sponsor) == operator }
        return (ownOperator + others + far).map { it.first }.take(limit)
    }

    /**
     * Premier serveur, dans l'ordre de préférence, dont la latence reste proche de la meilleure.
     * [measured] suit l'ordre de [order] ; null si aucun n'a répondu.
     */
    fun <T> choose(measured: List<Pair<T, Long>>): T? {
        val best = measured.minOfOrNull { it.second } ?: return null
        val tolerance = maxOf(MIN_TOLERANCE_MS, best / 2)
        return measured.first { it.second <= best + tolerance }.first
    }

    /** Distance à l'appareil quand sa position et celle du serveur sont connues, sinon celle de speedtest.net. */
    fun distanceKm(candidate: SpeedTestCandidate, device: GeoPoint?): Double {
        val lat = candidate.latitude
        val lon = candidate.longitude
        if (device != null && lat != null && lon != null) return haversineKm(device, GeoPoint(lat, lon))
        return candidate.apiDistanceKm ?: Double.MAX_VALUE
    }

    /** « Tunisie Telecom », « TT » → tt ; « OOREDOO 5G », « Ooredoo TN » → ooredoo. Null sans nom exploitable. */
    fun operatorKey(name: String?): String? {
        val normalized = Normalizer.normalize(name.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
        if (normalized.isEmpty()) return null
        val compact = normalized.replace(" ", "")
        return when {
            "ooredoo" in compact || "tunisiana" in compact -> "ooredoo"
            "orange" in compact -> "orange"
            "tunisietelecom" in compact || normalized == "tt" || normalized.startsWith("tt ") -> "tt"
            else -> normalized.split(" ").firstOrNull { it.length >= 3 } ?: normalized
        }
    }

    private fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
    }

    const val LATENCY_CANDIDATES = 5
    /** Serveurs à moins de cette marge du plus proche : même agglomération. */
    private const val NEARBY_MARGIN_KM = 30.0
    private const val MIN_TOLERANCE_MS = 15L
    private const val EARTH_RADIUS_KM = 6_371.0
}
