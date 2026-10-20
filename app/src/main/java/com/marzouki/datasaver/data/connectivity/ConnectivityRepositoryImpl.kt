package com.marzouki.datasaver.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.marzouki.datasaver.domain.repository.ConnectivityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Suit le réseau par défaut et indique s'il est non facturé, comme la contrainte WorkManager UNMETERED. */
class ConnectivityRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ConnectivityRepository {

    override fun observeUnmeteredConnection(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.isUnmetered())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val current = connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        trySend(current?.isUnmetered() == true)
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun NetworkCapabilities.isUnmetered(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
