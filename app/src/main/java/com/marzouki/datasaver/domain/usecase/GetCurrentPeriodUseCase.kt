package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.QuotaRepository
import javax.inject.Inject

/** Cycle de facturation en cours : celui du pass s'il a une validité, sinon le cycle mensuel. */
class GetCurrentPeriodUseCase @Inject constructor(
    private val quotaRepository: QuotaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(nowMillis: Long = clock.nowMillis()): MonthPeriod =
        quotaRepository.currentPeriod(nowMillis)
}
