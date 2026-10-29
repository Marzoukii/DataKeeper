package com.marzouki.datasaver.data.tethering

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.marzouki.datasaver.data.shareWhileObserved
import com.marzouki.datasaver.di.ApplicationScope
import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringSession
import com.marzouki.datasaver.domain.repository.TetheringRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import kotlin.math.abs

/**
 * État du partage d'après deux diffusions système persistantes (sticky), non documentées mais sans
 * permission : WIFI_AP_STATE_CHANGED (point d'accès Wi-Fi) et TETHER_STATE_CHANGED (interfaces
 * partagées, y compris USB et Bluetooth), complétées par la présence d'une interface réseau de
 * partage. Selon les constructeurs, l'une ou l'autre peut manquer ou retarder : le partage est
 * allumé dès que l'une le dit.
 *
 * La conso vient des compteurs de data mobile du téléphone ([TrafficStats]), lus en temps réel :
 * l'historique d'Android ne compte que par tranches de deux heures. Elle inclut donc aussi le trafic
 * des applis du téléphone pendant le partage.
 */
class TetheringRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : TetheringRepository {

    private val apFilter = IntentFilter(ACTION_WIFI_AP_STATE_CHANGED)
    private val tetherFilter = IntentFilter(ACTION_TETHER_STATE_CHANGED)

    override fun observeHotspotActive(): Flow<Boolean> = hotspotActive

    // Ma consommation et Partage le suivent : un seul récepteur et un seul sondage pour les deux.
    private val hotspotActive: Flow<Boolean> = callbackFlow {
        var apOn = false
        var tetherOn = false
        var interfaceOn = false
        fun send() = trySend(apOn || tetherOn || interfaceOn)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_WIFI_AP_STATE_CHANGED -> apOn = intent.isApEnabled()
                    ACTION_TETHER_STATE_CHANGED -> tetherOn = intent.hasActiveTether()
                }
                send()
            }
        }
        // Les diffusions persistantes donnent l'état courant dès l'abonnement.
        apOn = register(receiver, apFilter)?.isApEnabled() == true
        tetherOn = register(receiver, tetherFilter)?.hasActiveTether() == true
        // Filet de sécurité si un constructeur tait les diffusions : l'interface du partage est
        // revérifiée toutes les quelques secondes, à l'ouverture comme ensuite.
        val poll = launch {
            while (true) {
                interfaceOn = withContext(Dispatchers.IO) { hasTetherInterface() }
                send()
                delay(INTERFACE_POLL_MILLIS)
            }
        }
        awaitClose {
            poll.cancel()
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged().shareWhileObserved(appScope)

    override fun isHotspotActive(): Boolean =
        register(null, apFilter)?.isApEnabled() == true ||
            register(null, tetherFilter)?.hasActiveTether() == true ||
            hasTetherInterface()

    /**
     * Interface de partage allumée et adressée : point d'accès (swlan0 chez Samsung, ap0, wlan1…),
     * USB (rndis0, ncm0) ou Bluetooth (bt-pan). wlan0 est la connexion Wi-Fi du téléphone, pas un partage.
     */
    private fun hasTetherInterface(): Boolean = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { iface ->
            iface.isUp && TETHER_INTERFACE.matches(iface.name) &&
                iface.inetAddresses.toList().any { it is Inet4Address && !it.isLoopbackAddress }
        }
    }.getOrDefault(false)

    override fun mobileCounterBytes(): Long? {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return null
        return rx + tx
    }

    override suspend fun syncSession(active: Boolean, nowMillis: Long, counterBytes: Long): TetheringSession? {
        val bootMillis = nowMillis - SystemClock.elapsedRealtime()
        val prefs = dataStore.edit { prefs ->
            val lastSeenActive = prefs[KEY_LAST_SEEN_ACTIVE]
            prefs[KEY_LAST_SEEN_ACTIVE] = active
            if (!active) {
                prefs.remove(KEY_SESSION_START)
                prefs.remove(KEY_SESSION_START_KNOWN)
                prefs.remove(KEY_SESSION_BASELINE)
                prefs.remove(KEY_SESSION_BOOT)
                return@edit
            }
            val baseline = prefs[KEY_SESSION_BASELINE]
            // Les compteurs repartent de zéro au démarrage du téléphone : la session aussi.
            val sameBoot = prefs[KEY_SESSION_BOOT]?.let { abs(it - bootMillis) < BOOT_TOLERANCE_MILLIS } == true
            when {
                prefs[KEY_SESSION_START] == null || !sameBoot -> {
                    prefs[KEY_SESSION_START] = nowMillis
                    prefs[KEY_SESSION_START_KNOWN] = lastSeenActive == false
                    prefs[KEY_SESSION_BASELINE] = counterBytes
                    prefs[KEY_SESSION_BOOT] = bootMillis
                }
                // Compteur reparti en arrière (interface mobile recréée) : on repart de là.
                baseline != null && counterBytes < baseline -> prefs[KEY_SESSION_BASELINE] = counterBytes
            }
        }
        val start = prefs[KEY_SESSION_START] ?: return null
        return TetheringSession(
            startMillis = start,
            startKnown = prefs[KEY_SESSION_START_KNOWN] == true,
            baselineBytes = prefs[KEY_SESSION_BASELINE] ?: counterBytes,
        )
    }

    override val alert: Flow<TetheringAlert> = dataStore.data
        .map { prefs ->
            TetheringAlert(
                enabled = prefs[KEY_ALERT_ENABLED] ?: TetheringAlert.DEFAULT.enabled,
                thresholdBytes = prefs[KEY_ALERT_THRESHOLD] ?: TetheringAlert.DEFAULT.thresholdBytes,
            )
        }
        .distinctUntilChanged()

    override suspend fun setAlert(alert: TetheringAlert) {
        dataStore.edit { prefs ->
            prefs[KEY_ALERT_ENABLED] = alert.enabled
            prefs[KEY_ALERT_THRESHOLD] = alert.thresholdBytes
        }
    }

    override val alertedSessionStart: Flow<Long?> = dataStore.data.map { it[KEY_ALERTED_SESSION_START] }

    override suspend fun setAlertedSessionStart(startMillis: Long?) {
        dataStore.edit { prefs ->
            if (startMillis == null) prefs.remove(KEY_ALERTED_SESSION_START) else prefs[KEY_ALERTED_SESSION_START] = startMillis
        }
    }

    /** Abonne [receiver] (ou lit seulement l'état si null) et renvoie la dernière diffusion persistante. */
    private fun register(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? =
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

    private fun Intent.isApEnabled(): Boolean = getIntExtra(EXTRA_WIFI_AP_STATE, -1) == WIFI_AP_STATE_ENABLED

    private fun Intent.hasActiveTether(): Boolean = !getStringArrayListExtra(EXTRA_ACTIVE_TETHER).isNullOrEmpty()

    private companion object {
        // Constantes masquées de WifiManager et ConnectivityManager, stables depuis Android 4.
        const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val EXTRA_WIFI_AP_STATE = "wifi_state"
        const val WIFI_AP_STATE_ENABLED = 13
        const val ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED"
        const val EXTRA_ACTIVE_TETHER = "tetherArray"

        val TETHER_INTERFACE = Regex("swlan\\d+|ap\\d+|softap\\d+|wlan[1-9]\\d*|wigig\\d+|rndis\\d+|ncm\\d+|bt-pan")
        const val INTERFACE_POLL_MILLIS = 3_000L

        /** L'heure de démarrage recalculée varie de quelques millisecondes d'une lecture à l'autre. */
        const val BOOT_TOLERANCE_MILLIS = 10_000L

        val KEY_LAST_SEEN_ACTIVE = booleanPreferencesKey("tethering_last_seen_active")
        val KEY_SESSION_START = longPreferencesKey("tethering_session_start")
        val KEY_SESSION_START_KNOWN = booleanPreferencesKey("tethering_session_start_known")
        val KEY_SESSION_BASELINE = longPreferencesKey("tethering_session_baseline")
        val KEY_SESSION_BOOT = longPreferencesKey("tethering_session_boot")
        val KEY_ALERT_ENABLED = booleanPreferencesKey("tethering_alert_enabled")
        val KEY_ALERT_THRESHOLD = longPreferencesKey("tethering_alert_threshold")
        val KEY_ALERTED_SESSION_START = longPreferencesKey("tethering_alerted_session_start")
    }
}
