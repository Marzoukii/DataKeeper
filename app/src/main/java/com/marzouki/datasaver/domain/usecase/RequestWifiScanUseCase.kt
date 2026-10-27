package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.WifiRepository
import javax.inject.Inject

class RequestWifiScanUseCase @Inject constructor(private val repository: WifiRepository) {
    operator fun invoke(): Boolean = repository.requestScan()
}
