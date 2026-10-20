package com.marzouki.datasaver.presentation.usage

import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus

data class UsageState(
    val isLoading: Boolean = true,
    val hasUsageAccess: Boolean = true,
    val usage: DataUsage? = null,
    val loadFailed: Boolean = false,
    /** Cycle affiché dans l'en-tête, connu même avant la fin du chargement. */
    val period: MonthPeriod? = null,
    val quota: DataQuota? = null,
    val dailyUsage: List<DayUsage> = emptyList(),
    val topMobileApps: List<AppDataUsage> = emptyList(),
) {
    val quotaStatus: QuotaStatus? get() = if (quota != null && usage != null) QuotaStatus(quota, usage.mobileBytes) else null
}

sealed interface UsageIntent {
    /** Écran affiché/repris : l'utilisateur revient peut-être des réglages système. */
    data object Refresh : UsageIntent
    data object GrantAccessClicked : UsageIntent
}

sealed interface UsageEffect {
    data object OpenUsageAccessSettings : UsageEffect
}
