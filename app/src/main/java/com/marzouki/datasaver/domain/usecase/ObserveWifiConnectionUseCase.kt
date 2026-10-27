package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWifiConnectionUseCase @Inject constructor(private val repository: ConnectivityRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeUnmeteredConnection()
}
