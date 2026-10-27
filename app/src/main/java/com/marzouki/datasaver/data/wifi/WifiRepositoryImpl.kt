package com.marzouki.datasaver.data.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.enums.WifiPortal
import com.marzouki.datasaver.domain.model.OpenWifiNetwork
import com.marzouki.datasaver.domain.model.WifiScan
import com.marzouki.datasaver.domain.repository.WifiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Scan des Wi-Fi ouverts par [WifiManager] et suivi de la connexion Wi-Fi par [ConnectivityManager].
 * Android ne dit s'il y a un portail qu'une fois connecté : on le retient par nom de réseau.
 */
class WifiRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : WifiRepository {

    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override fun availability(): WifiAvailability = when {
        !hasPermissions() -> WifiAvailability.PERMISSION_MISSING
        !wifiManager.isWifiEnabled -> WifiAvailability.WIFI_OFF
        !LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java)) ->
            WifiAvailability.LOCATION_OFF
        else -> WifiAvailability.READY
    }

    override fun observeOpenNetworks(): Flow<WifiScan> {
        val connection = observeWifiConnection().onEach(::rememberPortal)
        val portals = dataStore.data.map { it[KEY_PORTAL_SSIDS].orEmpty() to it[KEY_NO_PORTAL_SSIDS].orEmpty() }
        return combine(observeScanResults(), portals, connection.onStart { emit(null) }) { results, (withPortal, noPortal), current ->
            val networks = results
                .filter { it.isOpen() }
                .mapNotNull { result -> result.ssidOrNull()?.let { it to result } }
                .groupBy({ it.first }, { it.second })
                .map { (ssid, sameName) ->
                    val best = sameName.maxBy { it.level }
                    OpenWifiNetwork(
                        ssid = ssid,
                        rssi = best.level,
                        frequencyMhz = best.frequency,
                        portal = when (ssid) {
                            in withPortal -> WifiPortal.WITH_PORTAL
                            in noPortal -> WifiPortal.NO_PORTAL
                            else -> WifiPortal.UNKNOWN
                        },
                    )
                }
                .sortedWith(compareBy<OpenWifiNetwork> { it.estimatedDistanceMeters }.thenByDescending { it.rssi })
            WifiScan(networks, results.lastSeenMillis(), current?.ssid)
        }
    }

    @Suppress("DEPRECATION") // Toujours permis au premier plan, quatre fois par tranche de deux minutes.
    override fun requestScan(): Boolean = hasPermissions() && wifiManager.startScan()

    private fun hasPermissions(): Boolean {
        val required = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    /** Derniers résultats connus tout de suite, puis ceux de chaque nouveau scan. */
    @SuppressLint("MissingPermission") // Vérifiée par hasPermissions() juste avant la lecture.
    private fun observeScanResults(): Flow<List<ScanResult>> = callbackFlow {
        fun send() {
            // Permission retirée en cours de route : on garde la liste vide plutôt que de planter.
            trySend(if (hasPermissions()) runCatching { wifiManager.scanResults }.getOrNull().orEmpty() else emptyList())
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = send()
        }
        send()
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }

    /** Wi-Fi rejoint et état du portail, ou null hors Wi-Fi. */
    private fun observeWifiConnection(): Flow<WifiConnection?> = callbackFlow {
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val onChange: (NetworkCapabilities?) -> Unit = { capabilities ->
            trySend(
                capabilities?.let {
                    WifiConnection(
                        ssid = connectedSsid(it),
                        captivePortal = it.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),
                        validated = it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    )
                },
            )
        }
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WifiCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO, onChange)
        } else {
            WifiCallback(onChange)
        }
        connectivityManager.registerNetworkCallback(request, callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * Un portail vu une fois marque le réseau pour de bon : une fois la page validée, Android le dit
     * « validé » sans portail, ce qui ne doit pas le faire passer pour un réseau sans pub.
     */
    private suspend fun rememberPortal(connection: WifiConnection?) {
        val ssid = connection?.ssid ?: return
        dataStore.edit { prefs ->
            val withPortal = prefs[KEY_PORTAL_SSIDS].orEmpty()
            val noPortal = prefs[KEY_NO_PORTAL_SSIDS].orEmpty()
            when {
                connection.captivePortal -> {
                    prefs[KEY_PORTAL_SSIDS] = withPortal + ssid
                    prefs[KEY_NO_PORTAL_SSIDS] = noPortal - ssid
                }
                connection.validated && ssid !in withPortal -> prefs[KEY_NO_PORTAL_SSIDS] = noPortal + ssid
            }
        }
    }

    /** Android 12+ ne donne le nom du Wi-Fi au rappel que s'il le demande ; avant, [WifiManager] le donne. */
    @Suppress("DEPRECATION")
    private fun connectedSsid(capabilities: NetworkCapabilities): String? {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            capabilities.transportInfo as? WifiInfo
        } else {
            wifiManager.connectionInfo
        }
        return info?.ssid?.cleanSsid()
    }

    private data class WifiConnection(val ssid: String?, val captivePortal: Boolean, val validated: Boolean)

    private companion object {
        val KEY_PORTAL_SSIDS = stringSetPreferencesKey("wifi_portal_ssids")
        val KEY_NO_PORTAL_SSIDS = stringSetPreferencesKey("wifi_no_portal_ssids")

        /** Sécurités annoncées par un réseau protégé ; OWE chiffre sans mot de passe, il n'est donc pas « ouvert ». */
        val SECURED = listOf("WEP", "WPA", "PSK", "EAP", "SAE", "OWE")
    }

    private fun ScanResult.isOpen(): Boolean = SECURED.none { it in capabilities }

    @Suppress("DEPRECATION") // wifiSsid (Android 13+) donne la même chose, entre guillemets.
    private fun ScanResult.ssidOrNull(): String? = SSID?.cleanSsid()

    /** Heure du résultat le plus récent ; [ScanResult.timestamp] compte en microsecondes depuis le démarrage. */
    private fun List<ScanResult>.lastSeenMillis(): Long? {
        val latestMicros = maxOfOrNull { it.timestamp } ?: return null
        return System.currentTimeMillis() - (SystemClock.elapsedRealtime() - latestMicros / 1_000)
    }
}

/** Rappel réseau ; avec FLAG_INCLUDE_LOCATION_INFO (Android 12+), il reçoit aussi le nom du Wi-Fi. */
private class WifiCallback : ConnectivityManager.NetworkCallback {
    private val onChange: (NetworkCapabilities?) -> Unit

    constructor(onChange: (NetworkCapabilities?) -> Unit) : super() {
        this.onChange = onChange
    }

    @RequiresApi(Build.VERSION_CODES.S)
    constructor(flags: Int, onChange: (NetworkCapabilities?) -> Unit) : super(flags) {
        this.onChange = onChange
    }

    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = onChange(capabilities)

    override fun onLost(network: Network) = onChange(null)
}

/** Nom renvoyé par Android quand il masque celui du Wi-Fi. */
private const val UNKNOWN_SSID = "<unknown ssid>"

/** Nom affichable : sans guillemets, null pour un réseau caché ou un nom masqué par Android. */
private fun String.cleanSsid(): String? =
    removeSurrounding("\"").takeIf { it.isNotBlank() && it != UNKNOWN_SSID }
