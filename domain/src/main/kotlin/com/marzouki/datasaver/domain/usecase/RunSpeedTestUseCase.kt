package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.SpeedTestEvent
import com.marzouki.datasaver.domain.model.SpeedTestProgress
import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RunSpeedTestUseCase @Inject constructor(
    private val repository: SpeedTestRepository,
    private val clock: Clock,
) {

    /**
     * Lance le test, puis l'ajoute à l'historique s'il va au bout. En données mobiles, chaque sens
     * est plafonné à [MOBILE_MAX_BYTES_PER_PHASE] pour ménager le forfait ; [operatorName] et
     * [generation] décrivent alors le réseau mesuré.
     */
    operator fun invoke(
        onWifi: Boolean,
        operatorName: String?,
        generation: NetworkGeneration?,
    ): Flow<SpeedTestEvent> = flow {
        var last: SpeedTestProgress? = null
        repository.measure(
            maxBytesPerPhase = if (onWifi) null else MOBILE_MAX_BYTES_PER_PHASE,
            operatorName = operatorName.takeUnless { onWifi },
        ).collect {
            last = it
            emit(SpeedTestEvent.Progress(it))
        }
        val done = last ?: return@flow
        val result = SpeedTestResult(
            downloadMbps = done.downloadMbps ?: return@flow,
            uploadMbps = done.uploadMbps ?: return@flow,
            pingMs = done.pingMs ?: return@flow,
            atMillis = clock.nowMillis(),
            onWifi = onWifi,
            operatorName = operatorName.takeUnless { onWifi },
            generation = generation.takeUnless { onWifi },
            server = done.server,
        )
        repository.addToHistory(result)
        emit(SpeedTestEvent.Finished(result))
    }

    companion object {
        const val MOBILE_MAX_BYTES_PER_PHASE = 25_000_000L
        /** Consommation maximale d'un test en données mobiles, téléchargement et envoi compris. */
        const val MOBILE_MAX_BYTES = 2 * MOBILE_MAX_BYTES_PER_PHASE
    }
}
