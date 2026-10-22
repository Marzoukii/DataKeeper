package com.marzouki.datasaver.domain.model

/** Quota mensuel de data mobile choisi par l'utilisateur. */
data class DataQuota(
    val limitBytes: Long,
    val alertThresholdPercent: Int = DEFAULT_ALERT_THRESHOLD_PERCENT,
    /** Notification à [alertThresholdPercent] % du quota. */
    val alertEnabled: Boolean = true,
    /** Dernier solde relevé chez l'opérateur ; null pour compter depuis le début du cycle. */
    val balance: OperatorBalance? = null,
) {
    init {
        require(limitBytes > 0) { "Le quota doit être positif" }
        require(alertThresholdPercent in 1..100)
        require(balance == null || balance.remainingBytes <= limitBytes) { "Le solde dépasse le forfait" }
    }

    /** Solde relevé pendant [period] : celui d'un cycle précédent ne vaut plus après la remise à zéro. */
    fun balanceIn(period: MonthPeriod): OperatorBalance? =
        balance?.takeIf { it.atMillis in period.startMillis..period.endMillis }

    /** Volume à partir duquel l'alerte est déclenchée. */
    val alertThresholdBytes: Long get() = limitBytes / 100 * alertThresholdPercent

    companion object {
        const val DEFAULT_ALERT_THRESHOLD_PERCENT = 80
    }
}

/**
 * Data restante annoncée par l'opérateur à [atMillis], ex. 17 Go sur un pass Ooredoo de 25 Go.
 * La consommation du forfait vaut alors ce qui était déjà parti plus la data mobile mesurée depuis.
 */
data class OperatorBalance(
    val remainingBytes: Long,
    val atMillis: Long,
) {
    init {
        require(remainingBytes >= 0) { "Le solde ne peut pas être négatif" }
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
