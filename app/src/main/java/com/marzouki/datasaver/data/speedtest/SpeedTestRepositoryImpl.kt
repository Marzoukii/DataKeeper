package com.marzouki.datasaver.data.speedtest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marzouki.datasaver.di.IoDispatcher
import com.marzouki.datasaver.domain.enums.SpeedTestPhase
import com.marzouki.datasaver.domain.model.GeoPoint
import com.marzouki.datasaver.domain.model.SpeedTestCandidate
import com.marzouki.datasaver.domain.model.SpeedTestServerPicker
import com.marzouki.datasaver.domain.model.SpeedTestProgress
import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.model.SpeedTestServer
import com.marzouki.datasaver.domain.repository.SpeedTestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.random.Random

/**
 * Test de débit sur un serveur Speedtest proche : speedtest.net propose des serveurs voisins
 * d'après l'adresse IP, souvent mal située ; la position de l'appareil, quand elle est connue,
 * corrige la distance, et les serveurs de l'opérateur des données passent devant
 * (voir [SpeedTestServerPicker]). Chaque sens dure quelques secondes sur plusieurs
 * connexions parallèles ; le débit retenu ignore la première seconde, le temps que TCP
 * prenne sa vitesse de croisière.
 */
class SpeedTestRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SpeedTestRepository {

    override val history: Flow<List<SpeedTestResult>> = dataStore.data
        .map { SpeedTestHistoryCodec.decode(it[KEY_HISTORY]) }
        .distinctUntilChanged()

    override suspend fun addToHistory(result: SpeedTestResult) {
        dataStore.edit { prefs ->
            val history = listOf(result) + SpeedTestHistoryCodec.decode(prefs[KEY_HISTORY])
            prefs[KEY_HISTORY] = SpeedTestHistoryCodec.encode(history.take(HISTORY_SIZE))
        }
    }

    override fun measure(maxBytesPerPhase: Long?, operatorName: String?): Flow<SpeedTestProgress> = channelFlow {
        send(SpeedTestProgress(SpeedTestPhase.PING, fraction = 0f))
        val server = closestServer(operatorName)
        send(SpeedTestProgress(SpeedTestPhase.PING, PING_SHARE / 2, server = server.info))
        val ping = measurePing(server.baseUrl)
        val afterPing = SpeedTestProgress(SpeedTestPhase.DOWNLOAD, PING_SHARE, pingMs = ping, server = server.info)
        send(afterPing)

        val download = measureThroughput(
            DOWNLOAD_DURATION_MS,
            maxBytesPerPhase,
            transfer = { bytes, shouldStop -> download(server.baseUrl, bytes, shouldStop) },
        ) { mbps, phaseFraction ->
            send(afterPing.copy(fraction = PING_SHARE + DOWNLOAD_SHARE * phaseFraction, currentMbps = mbps))
        }
        val afterDownload = afterPing.copy(
            phase = SpeedTestPhase.UPLOAD,
            fraction = PING_SHARE + DOWNLOAD_SHARE,
            downloadMbps = download,
        )
        send(afterDownload)

        val upload = measureThroughput(
            UPLOAD_DURATION_MS,
            maxBytesPerPhase,
            transfer = { bytes, shouldStop -> upload(server.baseUrl, bytes, shouldStop) },
        ) { mbps, phaseFraction ->
            send(afterDownload.copy(fraction = PING_SHARE + DOWNLOAD_SHARE + UPLOAD_SHARE * phaseFraction, currentMbps = mbps))
        }
        send(afterDownload.copy(fraction = 1f, currentMbps = upload, uploadMbps = upload))
    }.flowOn(ioDispatcher)

    private class Candidate(val info: SpeedTestServer, val baseUrl: String)

    /** Serveur proche, de préférence celui de l'opérateur, tant qu'il répond presque aussi vite que le meilleur. */
    private fun closestServer(operatorName: String?): Candidate {
        val candidates = fetchServers()
        if (candidates.isEmpty()) throw IOException("Aucun serveur de mesure à proximité")
        val measured = SpeedTestServerPicker.order(candidates, deviceLocation(), operatorName)
            .mapNotNull { candidate ->
                val baseUrl = "https://${candidate.host}"
                // Première requête pour la connexion (DNS, TLS), la seconde pour la latence.
                runCatching { hello(baseUrl); Candidate(candidate.server, baseUrl) to hello(baseUrl) }.getOrNull()
            }
        return SpeedTestServerPicker.choose(measured) ?: throw IOException("Aucun serveur de mesure joignable")
    }

    /** Dernière position connue de l'appareil, si la localisation est autorisée et récente. */
    private fun deviceLocation(): GeoPoint? {
        val granted = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val location = try {
            manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        } catch (_: SecurityException) {
            null
        } ?: return null
        if (System.currentTimeMillis() - location.time > LOCATION_MAX_AGE_MS) return null
        return GeoPoint(location.latitude, location.longitude)
    }

    private fun fetchServers(): List<SpeedTestCandidate> {
        val connection = open(SERVERS_URL)
        val body = try {
            connection.checkSuccess()
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        return try {
            val servers = JSONArray(body)
            List(servers.length()) { servers.getJSONObject(it) }.mapNotNull { server ->
                val host = server.optString("host").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SpeedTestCandidate(
                    server = SpeedTestServer(sponsor = server.optString("sponsor").trim(), name = server.optString("name").trim()),
                    host = host,
                    latitude = server.optString("lat").toDoubleOrNull(),
                    longitude = server.optString("lon").toDoubleOrNull(),
                    apiDistanceKm = server.optString("distance").toDoubleOrNull(),
                )
            }
        } catch (e: JSONException) {
            throw IOException("Liste des serveurs illisible", e)
        }
    }

    /** Médiane de quelques allers-retours sur une connexion déjà ouverte. */
    private fun measurePing(baseUrl: String): Int {
        val samples = List(PING_SAMPLES) { hello(baseUrl) }.sorted()
        return samples[samples.size / 2].toInt()
    }

    /** Durée en millisecondes d'une requête minimale au serveur. */
    private fun hello(baseUrl: String): Long {
        val start = System.nanoTime()
        val connection = open("$baseUrl/hello?nocache=${System.nanoTime()}")
        connection.checkSuccess()
        // Flux lu jusqu'au bout et refermé sans disconnect() : la connexion reste réutilisable.
        connection.inputStream.use { it.readBytes() }
        return (System.nanoTime() - start) / 1_000_000
    }

    /**
     * Fait tourner [transfer] sur [CONNECTIONS] connexions jusqu'à [durationMs] ou [maxBytes],
     * en publiant le débit moyen et l'avancement de l'étape toutes les [SAMPLE_MS]. Renvoie le débit en Mbit/s.
     */
    private suspend fun measureThroughput(
        durationMs: Long,
        maxBytes: Long?,
        transfer: (bytes: AtomicLong, shouldStop: () -> Boolean) -> Unit,
        onSample: suspend (mbps: Double, phaseFraction: Float) -> Unit,
    ): Double = coroutineScope {
        val bytes = AtomicLong()
        val start = nowMillis()
        val deadline = start + durationMs
        val limitReached = { nowMillis() >= deadline || (maxBytes != null && bytes.get() >= maxBytes) }
        val workers = List(CONNECTIONS) {
            launch(ioDispatcher) {
                val shouldStop = { !isActive || limitReached() }
                while (!shouldStop()) transfer(bytes, shouldStop)
            }
        }
        var warmUpBytes: Long? = null
        var warmUpAt = start
        while (workers.any { it.isActive }) {
            delay(SAMPLE_MS)
            val now = nowMillis()
            val transferred = bytes.get()
            val timeFraction = (now - start).toFloat() / durationMs
            val bytesFraction = maxBytes?.let { transferred.toFloat() / it } ?: 0f
            onSample(toMbps(transferred, now - start), maxOf(timeFraction, bytesFraction).coerceAtMost(1f))
            if (warmUpBytes == null && now - start >= WARM_UP_MS) {
                warmUpBytes = transferred
                warmUpAt = now
            }
        }
        val end = nowMillis()
        val total = bytes.get()
        val warm = warmUpBytes
        // Sur un plafond vite atteint, il ne reste pas assez de mesure après l'échauffement.
        if (warm != null && end - warmUpAt >= MIN_MEASURE_MS) toMbps(total - warm, end - warmUpAt) else toMbps(total, end - start)
    }

    private fun download(baseUrl: String, bytes: AtomicLong, shouldStop: () -> Boolean) {
        val connection = open("$baseUrl/download?nocache=${System.nanoTime()}&size=$DOWNLOAD_REQUEST_BYTES")
        try {
            connection.checkSuccess()
            connection.inputStream.use { input ->
                val buffer = ByteArray(BUFFER_BYTES)
                while (!shouldStop()) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    bytes.addAndGet(read.toLong())
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Envoi d'un bloc de taille fixe : ces serveurs n'acceptent pas l'envoi par morceaux (chunked).
     * À l'échéance, la requête est coupée net et l'erreur qui en découle ignorée.
     */
    private fun upload(baseUrl: String, bytes: AtomicLong, shouldStop: () -> Boolean) {
        val connection = open("$baseUrl/upload?nocache=${System.nanoTime()}").apply {
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(UPLOAD_REQUEST_BYTES)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        try {
            val output = connection.outputStream
            var sent = 0L
            while (sent < UPLOAD_REQUEST_BYTES) {
                if (shouldStop()) return
                val size = minOf(UploadPayload.size.toLong(), UPLOAD_REQUEST_BYTES - sent).toInt()
                output.write(UploadPayload, 0, size)
                sent += size
                bytes.addAndGet(size.toLong())
            }
            output.close()
            connection.checkSuccess()
        } catch (e: IOException) {
            if (!shouldStop()) throw e
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
        }

    private fun HttpURLConnection.checkSuccess() {
        val code = responseCode
        if (code !in 200..299) throw IOException("Serveur de mesure : HTTP $code")
    }

    private fun nowMillis(): Long = System.nanoTime() / 1_000_000

    private fun toMbps(bytes: Long, millis: Long): Double =
        if (millis <= 0) 0.0 else bytes * 8.0 / (millis * 1_000.0)

    private companion object {
        /**
         * Serveurs voisins d'après l'adresse IP, joignables en HTTPS ; assez nombreux pour couvrir
         * le pays quand l'IP est située dans une autre ville.
         */
        const val SERVERS_URL = "https://www.speedtest.net/api/js/servers?engine=js&limit=30&https_functional=true"
        const val LOCATION_MAX_AGE_MS = 7 * 24 * 3_600_000L
        const val PING_SAMPLES = 5
        const val CONNECTIONS = 4
        const val DOWNLOAD_DURATION_MS = 8_000L
        const val UPLOAD_DURATION_MS = 6_000L
        const val WARM_UP_MS = 1_000L
        const val MIN_MEASURE_MS = 1_000L
        const val SAMPLE_MS = 200L
        const val TIMEOUT_MS = 10_000
        const val BUFFER_BYTES = 64 * 1024
        const val DOWNLOAD_REQUEST_BYTES = 50_000_000L
        const val UPLOAD_REQUEST_BYTES = 2_000_000L

        // Part de chaque étape dans la barre d'avancement.
        const val PING_SHARE = 0.1f
        const val DOWNLOAD_SHARE = 0.5f
        const val UPLOAD_SHARE = 0.4f

        /** Octets aléatoires : une compression en route fausserait la mesure. */
        val UploadPayload: ByteArray = Random.nextBytes(BUFFER_BYTES)

        const val HISTORY_SIZE = 10
        val KEY_HISTORY = stringPreferencesKey("speed_test_history")
    }
}
