package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSpeedTestHistoryUseCase @Inject constructor(private val repository: SpeedTestRepository) {
    operator fun invoke(): Flow<List<SpeedTestResult>> = repository.history
}
