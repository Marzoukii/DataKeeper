package com.marzouki.datasaver.data.quota

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.usecase.CheckQuotaAndAlertUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Vérifie périodiquement la consommation mobile et alerte à 80 % du quota. */
@HiltWorker
class QuotaCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkQuotaAndAlert: CheckQuotaAndAlertUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { checkQuotaAndAlert() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.failure() })
}

class WorkManagerQuotaMonitorScheduler @Inject constructor(
    private val workManager: WorkManager,
) : QuotaMonitorScheduler {

    override fun start() {
        val request = PeriodicWorkRequestBuilder<QuotaCheckWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun stop() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "quota-check"
    }
}
