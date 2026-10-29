package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.repository.TetheringMonitorScheduler
import com.marzouki.datasaver.domain.repository.TetheringRepository
import javax.inject.Inject

/** Enregistre l'alerte et ne surveille le partage en arrière-plan que lorsqu'elle est active. */
class SetTetheringAlertUseCase @Inject constructor(
    private val repository: TetheringRepository,
    private val scheduler: TetheringMonitorScheduler,
) {
    suspend operator fun invoke(alert: TetheringAlert) {
        repository.setAlert(alert)
        if (alert.enabled) scheduler.start() else scheduler.stop()
    }
}
