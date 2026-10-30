package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.TetheringRepository
import javax.inject.Inject

/** Compteur de data mobile du téléphone, lu en temps réel ; null s'il est illisible. */
class GetMobileCounterUseCase @Inject constructor(private val repository: TetheringRepository) {
    operator fun invoke(): Long? = repository.mobileCounterBytes()
}
