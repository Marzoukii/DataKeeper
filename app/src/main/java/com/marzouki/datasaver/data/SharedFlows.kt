package com.marzouki.datasaver.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Un seul abonnement système (récepteur, rappel, sondage) pour tous les écrans qui l'observent ensemble.
 * Coupé [STOP_TIMEOUT_MILLIS] après le dernier, sans rejouer ensuite une valeur périmée au suivant.
 */
internal fun <T> Flow<T>.shareWhileObserved(scope: CoroutineScope): Flow<T> =
    shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS, replayExpirationMillis = 0), replay = 1)
