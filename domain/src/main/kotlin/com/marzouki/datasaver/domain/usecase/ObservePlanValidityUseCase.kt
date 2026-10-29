package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlanValidityUseCase @Inject constructor(private val repository: QuotaRepository)
{
    operator fun invoke(): Flow<PlanValidity?> = repository.planValidity
}
