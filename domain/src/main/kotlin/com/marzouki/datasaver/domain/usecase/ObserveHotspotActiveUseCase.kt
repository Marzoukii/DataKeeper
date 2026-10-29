package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.TetheringRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHotspotActiveUseCase @Inject constructor(private val repository: TetheringRepository)
{
    operator fun invoke(): Flow<Boolean> = repository.observeHotspotActive()
}
