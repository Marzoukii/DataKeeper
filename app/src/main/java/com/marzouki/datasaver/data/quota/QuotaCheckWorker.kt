package com.marzouki.datasaver.data.quota

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marzouki.datasaver.domain.model.PlanStatus
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.usecase.CheckQuotaAndAlertUseCase
import com.marzouki.datasaver.domain.usecase.ClearStaleBalanceUseCase
import com.marzouki.datasaver.domain.usecase.ObservePlanStatusUseCase
import com.marzouki.datasaver.domain.usecase.RemindBalanceRecalibrationUseCase
import com.marzouki.datasaver.domain.usecase.RemindPlanRenewalUseCase
import com.marzouki.datasaver.presentation.widget.refreshHomeSurfaces
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Vérifie périodiquement la consommation mobile et alerte à 80 % du quota. */
@HiltWorker
class QuotaCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkQuotaAndAlert: CheckQuotaAndAlertUseCase,
    private val clearStaleBalance: ClearStaleBalanceUseCase,
    private val remindBalanceRecalibration: RemindBalanceRecalibrationUseCase,
    private val remindPlanRenewal: RemindPlanRenewalUseCase,
    private val observePlanStatus: ObservePlanStatusUseCase,
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching {
            // Nouvelle SIM des données : le forfait enregistré est celui de l'ancienne, ni alerte ni
            // rappel tant que le nouveau n'est pas saisi. Le travail reste planifié et reprend seul
            // une fois le forfait saisi, ou si l'ancienne SIM revient.
            if (observePlanStatus().first() != PlanStatus.OPERATOR_CHANGED) {
                // Au nouveau cycle, le solde relevé le mois dernier est oublié (décompte depuis le forfait plein).
                clearStaleBalance()
                checkQuotaAndAlert()
                // Rappelle de recaler le solde s'il commence à dater.
                remindBalanceRecalibration()
                // Deux jours avant la fin du forfait, invite à le recharger.
                remindPlanRenewal()
            }
            // Profite du passage horaire pour garder widgets et tuiles à jour.
            refreshHomeSurfaces(applicationContext)
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.failure() })
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
