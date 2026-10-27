package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.model.WifiScan
import com.marzouki.datasaver.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWifiAvailabilityUseCase @Inject constructor(private val repository: WifiRepository) {
    operator fun invoke(): WifiAvailability = repository.availability()
}

class ObserveOpenWifiUseCase @Inject constructor(private val repository: WifiRepository) {
    operator fun invoke(): Flow<WifiScan> = repository.observeOpenNetworks()
}

class RequestWifiScanUseCase @Inject constructor(private val repository: WifiRepository) {
    operator fun invoke(): Boolean = repository.requestScan()
}
