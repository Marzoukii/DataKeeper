package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Enregistre (ou supprime avec null) le quota et (dés)active la surveillance en arrière-plan. */
class SetQuotaUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val scheduler: QuotaMonitorScheduler,
) {
    suspend operator fun invoke(limitBytes: Long?) {
        val current = repository.quota.first()
        // Changer de forfait ne doit pas réactiver une alerte que l'utilisateur a coupée.
        val alertEnabled = current?.alertEnabled ?: true
        // Le solde relevé reste valable tant qu'il tient dans le nouveau forfait.
        val balance = current?.balance?.takeIf { limitBytes != null && it.remainingBytes <= limitBytes }
        // Chaque forfait a son prix (28 DT les 25 Go, 4,8 DT les 1,4 Go) : il ne suit pas un changement de taille.
        val price = current?.priceMillimes?.takeIf { current.limitBytes == limitBytes }
        repository.setQuota(
            limitBytes?.let { DataQuota(it, alertEnabled = alertEnabled, balance = balance, priceMillimes = price) },
        )
        // Un nouveau quota doit pouvoir déclencher une nouvelle alerte ce mois-ci.
        repository.setLastAlertedMonth(null)
        if (limitBytes != null && alertEnabled) scheduler.start() else scheduler.stop()
    }
}
