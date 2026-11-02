package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Prévient une seule fois qu'une nouvelle SIM fournit la data : « Nouvelle SIM : Tunisie Telecom ».
 * Renvoie true si la notification vient d'être émise.
 */
class NotifyOperatorChangeUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val notifier: QuotaAlertNotifier,
) {
    suspend operator fun invoke(): Boolean {
        val change = repository.operatorChange.first() ?: return false
        if (change.notified) return false
        notifier.notifyOperatorChanged(change.operatorName, change.atMillis)
        repository.setOperatorChange(change.copy(notified = true))
        return true
    }
}
