package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.TetheringUsage
import com.marzouki.datasaver.domain.repository.TetheringRepository
import javax.inject.Inject

/** Partage en cours et data mobile consommée depuis son début ; null quand il est éteint. */
class GetTetheringUsageUseCase @Inject constructor(private val repository: TetheringRepository) {
    suspend operator fun invoke(
        active: Boolean = repository.isHotspotActive(),
        nowMillis: Long = System.currentTimeMillis(),
    ): TetheringUsage? {
        // Compteurs illisibles (rare) : la session reste suivie, sans conso.
        val counter = repository.mobileCounterBytes() ?: 0L
        val session = repository.syncSession(active, nowMillis, counter) ?: return null
        return TetheringUsage(session, (counter - session.baselineBytes).coerceAtLeast(0), nowMillis)
    }
}
