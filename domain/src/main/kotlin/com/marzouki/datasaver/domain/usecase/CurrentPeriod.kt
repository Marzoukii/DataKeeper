package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.first

internal suspend fun QuotaRepository.currentPeriod(nowMillis: Long): MonthPeriod =
    planValidity.first()?.periodAt(nowMillis) ?: MonthPeriod.current(nowMillis, cycleStartDay.first())
