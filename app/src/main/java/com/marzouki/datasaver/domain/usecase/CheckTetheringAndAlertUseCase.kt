package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.TetheringRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Une notification par session de partage, dès que le seuil est atteint. Vrai si elle a été envoyée. */
class CheckTetheringAndAlertUseCase @Inject constructor(
    private val repository: TetheringRepository,
    private val getTetheringUsage: GetTetheringUsageUseCase,
    private val notifier: QuotaAlertNotifier,
) {
    suspend operator fun invoke(): Boolean {
        val alert = repository.alert.first()
        if (!alert.enabled) return false
        // Lu même sans alerte à envoyer : la session suit ainsi l'allumage et l'extinction du partage.
        val usage = getTetheringUsage() ?: return false
        if (usage.usedBytes < alert.thresholdBytes) return false
        if (repository.alertedSessionStart.first() == usage.session.startMillis) return false
        notifier.notifyTetheringThreshold(usage.usedBytes, alert.thresholdBytes)
        repository.setAlertedSessionStart(usage.session.startMillis)
        return true
    }
}
