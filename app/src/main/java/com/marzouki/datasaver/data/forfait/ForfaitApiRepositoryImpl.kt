package com.marzouki.datasaver.data.forfait

import com.marzouki.datasaver.BuildConfig
import com.marzouki.datasaver.di.IoDispatcher
import com.marzouki.datasaver.domain.enums.TunisianOperator
import com.marzouki.datasaver.domain.model.ForfaitSnapshot
import com.marzouki.datasaver.domain.repository.ForfaitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Récupère le forfait via une API forfait **maison**, dont l'URL se règle à la compilation par
 * `FORFAIT_API_URL` (voir app/build.gradle.kts).
 *
 * - URL vide (par défaut) : renvoie un forfait de **démonstration**, pour que l'écran marche sans
 *   serveur — pratique pour une soutenance hors-ligne.
 * - URL renseignée : GET `"$url?operator=<MCC/MNC>"`, réponse JSON
 *   `{ "limitGb": 25, "remainingGb": 19, "validityDays": 30, "priceDt": 28.5 }` (champs facultatifs
 *   à null). HTTP 404 ⇒ opérateur non couvert (null) ; toute autre erreur ⇒ [IOException].
 *
 * Ce dépôt ne vise jamais le backend privé d'un opérateur : la saisie manuelle reste la référence.
 */
class ForfaitApiRepositoryImpl @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ForfaitRepository {

    override suspend fun fetchForfait(operatorKey: String?): ForfaitSnapshot? = withContext(dispatcher) {
        val baseUrl = BuildConfig.FORFAIT_API_URL.trim()
        if (baseUrl.isEmpty()) demoSnapshot(operatorKey) else httpSnapshot(baseUrl, operatorKey)
    }

    /** Forfait de démonstration, calqué sur un pass Ooredoo (25 Go, 19 restants, 30 j, 28,5 DT). */
    private suspend fun demoSnapshot(operatorKey: String?): ForfaitSnapshot? {
        // Petit délai : la démo montre l'état « récupération… » comme un vrai appel réseau.
        delay(DEMO_LATENCY_MILLIS)
        // Seuls les opérateurs tunisiens connus sont « couverts » ; sinon on retombe sur la saisie.
        TunisianOperator.fromMccMnc(operatorKey) ?: return null
        return ForfaitSnapshot(
            limitBytes = gbToBytes(25.0),
            remainingBytes = gbToBytes(19.0),
            validityDays = 30,
            priceMillimes = dtToMillimes(28.5),
        )
    }

    private fun httpSnapshot(baseUrl: String, operatorKey: String?): ForfaitSnapshot? {
        val query = operatorKey?.let { "?operator=" + URLEncoder.encode(it, "UTF-8") }.orEmpty()
        val connection = (URL(baseUrl + query).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
        }
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    return parse(body)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> return null
                else -> throw IOException("Réponse inattendue de l'API forfait : $code")
            }
        } catch (e: org.json.JSONException) {
            // Un corps illisible est une panne côté serveur, pas un forfait absent.
            throw IOException("Réponse illisible de l'API forfait", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): ForfaitSnapshot {
        val json = JSONObject(body)
        val limitBytes = gbToBytes(json.getDouble("limitGb"))
        val remaining = json.optDoubleOrNull("remainingGb")?.let { gbToBytes(it).coerceIn(0, limitBytes) }
        val validity = json.optIntOrNull("validityDays")?.coerceIn(1, MAX_VALIDITY_DAYS)
        val price = json.optDoubleOrNull("priceDt")?.let { dtToMillimes(it) }?.takeIf { it > 0 }
        return ForfaitSnapshot(limitBytes, remaining, validity, price)
    }

    private companion object {
        const val BYTES_PER_GB = 1_000_000_000L
        const val MILLIMES_PER_DINAR = 1_000L

        /** Même borne que le domaine (MonthPeriod.MAX_VALIDITY_DAYS), pour clamper une validité folle. */
        const val MAX_VALIDITY_DAYS = 365
        const val TIMEOUT_MILLIS = 10_000
        const val DEMO_LATENCY_MILLIS = 700L

        fun gbToBytes(gb: Double): Long = (gb * BYTES_PER_GB).roundToLong()
        fun dtToMillimes(dt: Double): Long = (dt * MILLIMES_PER_DINAR).roundToLong()

        fun JSONObject.optDoubleOrNull(key: String): Double? =
            if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

        fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null
    }
}
