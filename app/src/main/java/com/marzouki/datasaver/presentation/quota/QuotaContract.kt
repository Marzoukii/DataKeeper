package com.marzouki.datasaver.presentation.quota

import androidx.annotation.StringRes
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.QuotaStatus

data class QuotaState(
    val quota: DataQuota? = null,
    val usedMobileBytes: Long? = null,
    val hasUsageAccess: Boolean = true,
    /** Saisie du quota en Go, telle que tapée par l'utilisateur. */
    val input: String = "",
    val inputError: Boolean = false,
    /** Jour du mois où le forfait repart à zéro. */
    val cycleStartDay: Int = 1,
    /** Cycle en cours (date de remise à zéro, jours restants). */
    val period: MonthPeriod? = null,
) {
    val status: QuotaStatus? get() = if (quota != null && usedMobileBytes != null) QuotaStatus(quota, usedMobileBytes) else null
}

sealed interface QuotaIntent {
    data object Refresh : QuotaIntent
    data class InputChanged(val value: String) : QuotaIntent
    data class PresetSelected(val gigabytes: Int) : QuotaIntent
    data object Save : QuotaIntent
    data object RemoveQuota : QuotaIntent
    data object GrantAccessClicked : QuotaIntent
    data class AlertToggled(val enabled: Boolean) : QuotaIntent
    data class CycleStartDaySelected(val day: Int) : QuotaIntent
}

sealed interface QuotaEffect {
    data object OpenUsageAccessSettings : QuotaEffect
    /** Nécessaire pour l'alerte à 80 % (Android 13+). */
    data object RequestNotificationPermission : QuotaEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : QuotaEffect
}
