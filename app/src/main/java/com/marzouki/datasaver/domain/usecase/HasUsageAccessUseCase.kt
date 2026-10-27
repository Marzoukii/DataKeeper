package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.UsageRepository
import javax.inject.Inject

class HasUsageAccessUseCase @Inject constructor(private val repository: UsageRepository) {
    operator fun invoke(): Boolean = repository.hasUsageAccess()
}
