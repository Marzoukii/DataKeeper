package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveQuotaUseCase @Inject constructor(private val repository: QuotaRepository)
{
    operator fun invoke(): Flow<DataQuota?> = repository.quota
}
