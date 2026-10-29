package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.repository.WifiRepository
import javax.inject.Inject

class GetWifiAvailabilityUseCase @Inject constructor(private val repository: WifiRepository) {
    operator fun invoke(): WifiAvailability = repository.availability()
}
