package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCycleStartDayUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<Int> = repository.cycleStartDay
}
