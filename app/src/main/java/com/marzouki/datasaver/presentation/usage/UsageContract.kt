package com.marzouki.datasaver.presentation.usage
import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.LiveMobileUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.TetheringUsage
import com.marzouki.datasaver.domain.model.heavyAppFor

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
    val pricePerGb: PricePerGb? = null,
    /** Opérateur de la SIM des données mobiles, s'il est connu. */
    val operatorName: String? = null,
    /** Vrai par défaut pour ne pas faire clignoter l'invitation avant la première lecture. */
    val hasPhonePermission: Boolean = true,
    val networkGeneration: NetworkGeneration? = null,
    /** Data mobile décomptée du forfait, recalée sur le solde opérateur s'il a été relevé. */
    val quotaUsedBytes: Long? = null,
    /** Partage de connexion allumé : sa conso est alors relue toutes les quelques secondes. */
    val hotspotActive: Boolean = false,
    /** Partage de connexion en cours et sa conso ; null quand il est éteint. */
    val tethering: TetheringUsage? = null,
    /** Data mobile mesurée en direct depuis la dernière lecture de l'historique d'Android. */
    val liveMobile: LiveMobileUsage? = null,
) {
    private val liveExtraBytes: Long get() = liveMobile?.extraBytes ?: 0L

    /** [usage] avec la data mobile que l'historique n'a pas encore comptée : le total bouge en direct. */
    val liveUsage: DataUsage? get() = usage?.let { it.copy(mobileBytes = it.mobileBytes + liveExtraBytes) }

    private val liveQuotaUsedBytes: Long? get() = quotaUsedBytes?.plus(liveExtraBytes)

    val quotaStatus: QuotaStatus? get() = liveQuotaUsedBytes?.let { used -> quota?.let { QuotaStatus(it, used) } }

    /**
     * Data mobile du cycle selon l'opérateur, quand son solde a été relevé ce cycle : le même chiffre
     * que la carte du quota (25 Go − 16,5 restants = 8,5 Go), et non la mesure du téléphone, qui
     * ignore une partie de ce que l'opérateur facture. Null sans solde relevé.
     */
    val operatorMobileBytes: Long?
        get() {
            val period = period ?: return null
            if (quota?.balanceIn(period) == null) return null
            return liveQuotaUsedBytes
        }

    /** Appli à basculer en Wi-Fi tant elle pèse sur le forfait ; null si aucune ne dépasse le seuil. */
    val heavyApp: AppDataUsage? get() = topMobileApps.heavyAppFor(quota)
}

sealed interface UsageIntent {
    /** Écran affiché/repris : l'utilisateur revient peut-être des réglages système. */
    data object Refresh : UsageIntent
    data object GrantAccessClicked : UsageIntent
    /** Retour de la demande de permission « Téléphone ». */
    data object PhonePermissionResult : UsageIntent
}

sealed interface UsageEffect {
    data object OpenUsageAccessSettings : UsageEffect
}
