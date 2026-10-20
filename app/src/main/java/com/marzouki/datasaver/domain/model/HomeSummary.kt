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
) {
    val quotaStatus: QuotaStatus? get() = if (quota != null && usage != null) QuotaStatus(quota, usage.mobileBytes) else null

    /** Data mobile estimée en fin de cycle, au rythme moyen des jours écoulés. */
    val projectedMobileBytes: Long?
        get() = usage?.let { it.mobileBytes * period.totalDays / period.currentDay.coerceAtLeast(1) }
}
