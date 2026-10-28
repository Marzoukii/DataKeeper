package com.marzouki.datasaver.data.tethering

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marzouki.datasaver.domain.repository.CrashReporter
import com.marzouki.datasaver.domain.repository.TetheringMonitorScheduler
import com.marzouki.datasaver.domain.usecase.CheckTetheringAndAlertUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Suit le partage de connexion et prévient au seuil choisi ; tourne au rythme le plus court permis (15 min). */
@HiltWorker
class TetheringCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkTetheringAndAlert: CheckTetheringAndAlertUseCase,
    private val crashReporter: CrashReporter,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { checkTetheringAndAlert() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = {
                    // Un travail arrêté par le système n'est pas une erreur à corriger.
                    if (it !is CancellationException) crashReporter.recordException(it)
                    Result.failure()
                },
            )
}

class WorkManagerTetheringMonitorScheduler @Inject constructor(
    private val workManager: WorkManager,
) : TetheringMonitorScheduler {

    override fun start() {
        val request = PeriodicWorkRequestBuilder<TetheringCheckWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun stop() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "tethering-check"
    }
}
