package com.marzouki.datasaver.domain.model

/** Résumé affiché hors de l'application : widgets de l'écran d'accueil et tuiles des réglages rapides. */
data class HomeSummary(
    val period: MonthPeriod,
    val hasUsageAccess: Boolean,
    /** Null sans accès aux données d'usage ou si la lecture a échoué. */
    val usage: DataUsage?,
    val quota: DataQuota?,
    /** Téléchargements en attente du Wi-Fi ou en cours. */
    val pendingDownloads: Int,
    val pricePerGb: PricePerGb? = null,
    /** Data mobile décomptée du forfait (recalée sur le solde opérateur) ; null sans quota. */
    val quotaUsedBytes: Long? = null,
) {
    val quotaStatus: QuotaStatus? get() = if (quota != null && quotaUsedBytes != null) QuotaStatus(quota, quotaUsedBytes) else null

    /**
     * Data mobile estimée en fin de cycle : le forfait décompté plus le rythme mesuré depuis le
     * début du cycle sur les jours restants. Le solde recalé n'est pas extrapolé (voir [QuotaForecast]).
     */
    val projectedMobileBytes: Long?
        get() {
            val measured = usage?.mobileBytes ?: return null
            val daysAfterToday = (period.totalDays - period.currentDay).coerceAtLeast(0)
            return (quotaUsedBytes ?: measured) + measured * daysAfterToday / period.currentDay.coerceAtLeast(1)
        }
}
