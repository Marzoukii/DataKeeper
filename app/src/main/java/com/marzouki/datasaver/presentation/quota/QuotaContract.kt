package com.marzouki.datasaver.presentation.quota

import androidx.annotation.StringRes
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaForecast
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
    /** Pass en cours (achat + validité) ; null pour un cycle mensuel selon [cycleStartDay]. */
    val validity: PlanValidity? = null,
    /** Cycle en cours (date de remise à zéro, jours restants). */
    val period: MonthPeriod? = null,
    val pricePerGb: PricePerGb? = null,
    /** Data mobile décomptée chaque jour écoulé du cycle (recalée sur le solde), pour la courbe de prévision. */
    val dailyMobileBytes: List<Long>? = null,
    /** Data mobile mesurée chaque jour écoulé du cycle, sans recalage : rythme réel et conso du jour. */
    val measuredDailyMobileBytes: List<Long>? = null,
    /** Opérateur des données mobiles détecté par la SIM (Ooredoo, Orange, Tunisie Telecom…). */
    val operatorName: String? = null,
) {
    val status: QuotaStatus? get() = if (quota != null && usedMobileBytes != null) QuotaStatus(quota, usedMobileBytes) else null

    /** [period] une fois lu ; avant, celui qu'annoncent les réglages (pass ou cycle mensuel). */
    val displayPeriod: MonthPeriod
        get() = period ?: validity?.periodAt(System.currentTimeMillis()) ?: MonthPeriod.current(cycleStartDay = cycleStartDay)

    val forecast: QuotaForecast?
        get() = if (quota != null && period != null && !dailyMobileBytes.isNullOrEmpty()) {
            QuotaForecast(quota, period, dailyMobileBytes, measuredDailyMobileBytes ?: dailyMobileBytes)
        } else {
            null
        }
}

sealed interface QuotaIntent {
    data object Refresh : QuotaIntent
    data class InputChanged(val value: String) : QuotaIntent
    data class PresetSelected(val gigabytes: Int) : QuotaIntent
    data object Save : QuotaIntent
    data object RemoveQuota : QuotaIntent
    data object GrantAccessClicked : QuotaIntent
    data class AlertToggled(val enabled: Boolean) : QuotaIntent
    /** Jour d'achat et validité du forfait, ou renouvellement chaque mois. */
    data class PlanCycleSaved(val cycle: PlanCycleInput) : QuotaIntent
    /** Sans forfait : prix d'un Go saisi directement. Null efface le prix. */
    data class PricePerGbSaved(val price: PricePerGb?) : QuotaIntent
    /** Prix payé pour le forfait en cours, en millimes. Null efface le prix. */
    data class PlanPriceSaved(val priceMillimes: Long?) : QuotaIntent
    /** Data restante saisie par l'utilisateur ; null revient au décompte depuis le début du cycle. */
    data class BalanceSaved(val remainingBytes: Long?) : QuotaIntent
}

sealed interface QuotaEffect {
    data object OpenUsageAccessSettings : QuotaEffect
    /** Nécessaire pour l'alerte à 80 % (Android 13+). */
    data object RequestNotificationPermission : QuotaEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : QuotaEffect
}
