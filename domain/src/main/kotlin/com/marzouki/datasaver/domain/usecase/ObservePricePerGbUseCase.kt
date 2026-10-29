package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePricePerGbUseCase @Inject constructor(private val repository: QuotaRepository)
{
    operator fun invoke(): Flow<PricePerGb?> = repository.pricePerGb
}
