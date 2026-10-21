package com.marzouki.datasaver.data.operator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.NetworkGeneration
import com.marzouki.datasaver.domain.repository.OperatorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Opérateur et génération de réseau de la SIM choisie pour les données mobiles. Le code MCC/MNC
 * de la SIM se lit sans permission ; la 4G ou la 5G demande READ_PHONE_STATE.
 */
class OperatorRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : OperatorRepository {

    override fun dataOperator(): MobileOperator? {
        val telephony = dataTelephony() ?: return null
        if (telephony.simState != TelephonyManager.SIM_STATE_READY) return null
        return MobileOperator.from(mccMnc = telephony.simOperator, simName = telephony.simOperatorName)
    }

    override fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    override fun observeDataNetworkGeneration(): Flow<NetworkGeneration?> {
        val telephony = dataTelephony()
        if (telephony == null || !hasPhoneStatePermission()) return flowOf(null)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> displayInfoFlow(telephony)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> legacyDisplayInfoFlow(telephony)
            // Avant Android 11, pas d'info d'affichage : la 5G NSA apparaît comme de la 4G.
            else -> flowOf(generationOf(telephony.safeDataNetworkType()))
        }.distinctUntilChanged()
    }

    /** L'info d'affichage distingue la 5G NSA, que le type de réseau seul annonce en LTE. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun displayInfoFlow(telephony: TelephonyManager): Flow<NetworkGeneration?> = callbackFlow {
        val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
            override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                trySend(generationOf(info))
            }
        }
        trySend(generationOf(telephony.safeDataNetworkType()))
        try {
            telephony.registerTelephonyCallback(context.mainExecutor, callback)
        } catch (_: SecurityException) {
            // Permission retirée entre-temps : on garde la dernière valeur connue.
        }
        awaitClose { telephony.unregisterTelephonyCallback(callback) }
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun legacyDisplayInfoFlow(telephony: TelephonyManager): Flow<NetworkGeneration?> = callbackFlow {
        val listener = object : PhoneStateListener(context.mainExecutor) {
            override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                trySend(generationOf(info))
            }
        }
        trySend(generationOf(telephony.safeDataNetworkType()))
        try {
            telephony.listen(listener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
        } catch (_: SecurityException) {
        }
        awaitClose { telephony.listen(listener, PhoneStateListener.LISTEN_NONE) }
    }

    private fun dataTelephony(): TelephonyManager? {
        val base = context.getSystemService(TelephonyManager::class.java) ?: return null
        val subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()
        return if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            base.createForSubscriptionId(subscriptionId)
        } else {
            base
        }
    }

    private fun TelephonyManager.safeDataNetworkType(): Int =
        try {
            dataNetworkType
        } catch (_: SecurityException) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }

    // NR_NSA_MMWAVE est l'ancien nom de NR_ADVANCED (Android 12) ; les constantes sont copiées à la compilation.
    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun generationOf(info: TelephonyDisplayInfo): NetworkGeneration? =
        when (info.overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED,
            -> NetworkGeneration.G5
            else -> generationOf(info.networkType)
        }
}

/** Type de réseau de [TelephonyManager] → génération ; null si inconnu ou en appel Wi-Fi (IWLAN). */
@Suppress("DEPRECATION")
internal fun generationOf(networkType: Int): NetworkGeneration? = when (networkType) {
    TelephonyManager.NETWORK_TYPE_GPRS,
    TelephonyManager.NETWORK_TYPE_EDGE,
    TelephonyManager.NETWORK_TYPE_CDMA,
    TelephonyManager.NETWORK_TYPE_1xRTT,
    TelephonyManager.NETWORK_TYPE_IDEN,
    TelephonyManager.NETWORK_TYPE_GSM,
    -> NetworkGeneration.G2
    TelephonyManager.NETWORK_TYPE_UMTS,
    TelephonyManager.NETWORK_TYPE_EVDO_0,
    TelephonyManager.NETWORK_TYPE_EVDO_A,
    TelephonyManager.NETWORK_TYPE_EVDO_B,
    TelephonyManager.NETWORK_TYPE_HSDPA,
    TelephonyManager.NETWORK_TYPE_HSUPA,
    TelephonyManager.NETWORK_TYPE_HSPA,
    TelephonyManager.NETWORK_TYPE_EHRPD,
    TelephonyManager.NETWORK_TYPE_HSPAP,
    TelephonyManager.NETWORK_TYPE_TD_SCDMA,
    -> NetworkGeneration.G3
    TelephonyManager.NETWORK_TYPE_LTE -> NetworkGeneration.G4
    TelephonyManager.NETWORK_TYPE_NR -> NetworkGeneration.G5
    else -> null
}
