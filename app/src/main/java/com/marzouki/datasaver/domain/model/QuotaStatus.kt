package com.marzouki.datasaver.domain.model

/** Quota mensuel de data mobile choisi par l'utilisateur. */
data class DataQuota(
    val limitBytes: Long,
    val alertThresholdPercent: Int = DEFAULT_ALERT_THRESHOLD_PERCENT,
    /** Notification à [alertThresholdPercent] % du quota. */
    val alertEnabled: Boolean = true,
) {
    init {
        require(limitBytes > 0) { "Le quota doit être positif" }
        require(alertThresholdPercent in 1..100)
    }

    /** Volume à partir duquel l'alerte est déclenchée. */
    val alertThresholdBytes: Long get() = limitBytes / 100 * alertThresholdPercent

    companion object {
        const val DEFAULT_ALERT_THRESHOLD_PERCENT = 80
    }
}

/** Consommation mobile du mois rapportée au quota. */
data class QuotaStatus(
    val quota: DataQuota,
    val usedBytes: Long,
) {
    val usedRatio: Float get() = usedBytes.toFloat() / quota.limitBytes
    val remainingBytes: Long get() = (quota.limitBytes - usedBytes).coerceAtLeast(0)
    val exceededBytes: Long get() = (usedBytes - quota.limitBytes).coerceAtLeast(0)

    // Comparaison entière : évite qu'un arrondi flottant déclenche l'alerte trop tôt.
    val isAlertReached: Boolean get() = usedBytes * 100 >= quota.limitBytes * quota.alertThresholdPercent
    val isExceeded: Boolean get() = usedBytes >= quota.limitBytes
}
