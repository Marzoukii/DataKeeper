package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.repository.OperatorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDataNetworkGenerationUseCase @Inject constructor(private val repository: OperatorRepository) {
    operator fun invoke(): Flow<NetworkGeneration?> = repository.observeDataNetworkGeneration()
}
