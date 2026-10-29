package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.repository.UsageRepository

/**
 * Depuis le solde relevé ce cycle : ce qui était déjà parti du forfait plus la data mobile
 * mesurée depuis. Sinon, la data mobile mesurée depuis le début du cycle.
 */
internal suspend fun UsageRepository.quotaUsedBytes(quota: DataQuota, period: MonthPeriod): Long {
    val balance = quota.balanceIn(period) ?: return getUsage(period).mobileBytes
    val sinceBalance = getUsage(period.copy(startMillis = balance.atMillis)).mobileBytes
    return quota.limitBytes - balance.remainingBytes + sinceBalance
}
