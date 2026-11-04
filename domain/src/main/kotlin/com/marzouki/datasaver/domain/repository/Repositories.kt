package com.marzouki.datasaver.domain.repository

import com.marzouki.datasaver.domain.enums.AppLanguageChoice
import com.marzouki.datasaver.domain.enums.DownloadSchedule
import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.enums.ThemeMode
import com.marzouki.datasaver.domain.enums.WifiAvailability
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.ForfaitSnapshot
import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.domain.model.SpeedTestProgress
import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringSession
import com.marzouki.datasaver.domain.model.WifiScan
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun hasUsageAccess(): Boolean

    /** @throws com.marzouki.datasaver.domain.model.UsageAccessDeniedException */
    suspend fun getUsage(period: MonthPeriod): DataUsage

    /** Trafic des [days] derniers jours calendaires, aujourd'hui compris, du plus ancien au plus récent. */
    suspend fun getDailyUsage(days: Int, nowMillis: Long): List<DayUsage>

    /** Applications ayant consommé le plus de data mobile sur la période, par ordre décroissant. */
    suspend fun getTopMobileApps(period: MonthPeriod, limit: Int): List<AppDataUsage>
}

interface TetheringRepository {
    /** Partage de connexion allumé (point d'accès Wi-Fi, USB ou Bluetooth), relu à chaque changement. */
    fun observeHotspotActive(): Flow<Boolean>

    /** État du partage à cet instant, pour le travail en arrière-plan. */
    fun isHotspotActive(): Boolean

    /** Data mobile envoyée et reçue par le téléphone depuis son démarrage, en temps réel ; null si illisible. */
    fun mobileCounterBytes(): Long?

    /**
     * Tient à jour la session d'après l'état du partage : début et compteur [counterBytes] retenus
     * quand on le voit allumé pour la première fois, session oubliée quand il s'éteint.
     * Renvoie la session en cours, null s'il est éteint.
     */
    suspend fun syncSession(active: Boolean, nowMillis: Long, counterBytes: Long): TetheringSession?

    val alert: Flow<TetheringAlert>
    suspend fun setAlert(alert: TetheringAlert)

    /** Début de la session ([TetheringSession.startMillis]) déjà signalée par une notification ; null si aucune. */
    val alertedSessionStart: Flow<Long?>
    suspend fun setAlertedSessionStart(startMillis: Long?)
}

/** Planifie la surveillance du partage de connexion, tant que son alerte est active. */
interface TetheringMonitorScheduler {
    fun start()
    fun stop()
}

interface QuotaRepository {
    val quota: Flow<DataQuota?>
    val lastAlertedMonth: Flow<String?>
    /** Jour du mois (1 à 28) où le forfait est remis à zéro. */
    val cycleStartDay: Flow<Int>
    /** Prix d'un Go mobile, pour chiffrer les économies du Wi-Fi ; null tant qu'il n'est pas renseigné. */
    val pricePerGb: Flow<PricePerGb?>
    suspend fun setQuota(quota: DataQuota?)
    suspend fun setLastAlertedMonth(monthKey: String?)
    suspend fun setCycleStartDay(day: Int)
    suspend fun setPricePerGb(price: PricePerGb?)
    /** Date du dernier rappel « recale ton solde » envoyé ; null si jamais. */
    val balanceReminderAt: Flow<Long?>
    suspend fun setBalanceReminderAt(millis: Long?)
    /** Cycle ([MonthPeriod.key]) dont la fin du forfait a déjà été rappelée ; null si jamais. */
    val lastRenewalReminderCycle: Flow<String?>
    suspend fun setLastRenewalReminderCycle(cycleKey: String?)
    /** Vrai une fois l'écran de configuration du forfait validé ou passé (« pas de forfait »). */
    val planSetupDone: Flow<Boolean>
    suspend fun setPlanSetupDone(done: Boolean)
    /** [com.marzouki.datasaver.domain.model.MobileOperator.key] de la SIM pour laquelle le forfait a été saisi. */
    val planOperatorKey: Flow<String?>
    suspend fun setPlanOperatorKey(key: String?)
    /** Pass en cours (date d'achat et validité) ; null pour un cycle mensuel selon [cycleStartDay]. */
    val planValidity: Flow<PlanValidity?>
    suspend fun setPlanValidity(validity: PlanValidity?)
    /** Forfaits déjà saisis, tous opérateurs confondus, du plus récent au plus ancien. */
    val savedPlans: Flow<List<SavedPlan>>
    suspend fun setSavedPlans(plans: List<SavedPlan>)
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<ScheduledDownload>>
    fun schedule(url: String, fileName: String, schedule: DownloadSchedule)
    fun cancel(id: String)
    fun clearFinished()
}

interface OperatorRepository {
    /** Opérateur de la SIM des données mobiles ; null sans SIM prête ou illisible. */
    fun dataOperator(): MobileOperator?

    /** [dataOperator], relu à chaque changement de SIM (carte changée, SIM des données basculée). */
    fun observeDataOperator(): Flow<MobileOperator?>

    /** Permission « Téléphone » (READ_PHONE_STATE), nécessaire pour connaître la 4G ou la 5G. */
    fun hasPhoneStatePermission(): Boolean

    /** 2G à 5G pour la SIM des données, mise à jour en continu ; null sans permission ou hors réseau. */
    fun observeDataNetworkGeneration(): Flow<NetworkGeneration?>
}

/**
 * Récupère le forfait en cours auprès d'une API forfait, pour éviter la saisie manuelle.
 *
 * L'implémentation vise un serveur **maison** (démo de PFE), dont l'URL se règle par
 * `BuildConfig.FORFAIT_API_URL` ; URL vide, elle renvoie un forfait de démonstration. Elle ne
 * s'adresse jamais au backend privé d'un opérateur : l'utilisateur garde toujours la saisie manuelle.
 */
interface ForfaitRepository {
    /**
     * Interroge l'API pour l'opérateur [operatorKey] (code MCC/MNC de la SIM, ex. « 60503 » pour
     * Ooredoo). Renvoie le forfait courant, ou null si l'API ne couvre pas cet opérateur.
     * @throws java.io.IOException si le réseau ou le serveur fait défaut.
     */
    suspend fun fetchForfait(operatorKey: String?): ForfaitSnapshot?
}

interface LanguageRepository {
    fun current(): AppLanguageChoice

    /** Applique la langue tout de suite : l'écran se recrée dans la nouvelle langue. À appeler sur le fil principal. */
    fun set(choice: AppLanguageChoice)
}

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>

    /** Enregistre le thème et l'applique tout de suite : l'écran se recrée. À appeler sur le fil principal. */
    suspend fun setThemeMode(mode: ThemeMode)

    /** Applique le thème enregistré, au démarrage de l'appli. */
    suspend fun applySavedThemeMode()
}

interface ConnectivityRepository {
    /** Vrai tant que l'appareil est sur un réseau non facturé (Wi-Fi), condition des téléchargements. */
    fun observeUnmeteredConnection(): Flow<Boolean>
}

interface WifiRepository {
    /** Permission, Wi-Fi et localisation : relu à chaque appel, l'utilisateur peut tout changer hors de l'appli. */
    fun availability(): WifiAvailability

    /**
     * Wi-Fi ouverts du dernier scan, relus à chaque nouveau scan et à chaque connexion. Tant qu'il est
     * collecté, retient aussi pour chaque Wi-Fi ouvert rejoint s'il passe par un portail de connexion.
     */
    fun observeOpenNetworks(): Flow<WifiScan>

    /**
     * Lance un nouveau scan ; faux s'il n'a pas pu partir (Android en limite le nombre), les
     * résultats précédents restent alors affichés.
     */
    fun requestScan(): Boolean
}

interface SpeedTestRepository {
    /** Derniers tests terminés, du plus récent au plus ancien. */
    val history: Flow<List<SpeedTestResult>>
    suspend fun addToHistory(result: SpeedTestResult)

    /**
     * Mesure ping, téléchargement puis envoi ; le flux se termine une fois l'envoi mesuré.
     * [maxBytesPerPhase] arrête chaque sens plus tôt une fois ce volume transféré.
     * @throws java.io.IOException si le réseau ou le serveur de mesure fait défaut.
     */
    fun measure(maxBytesPerPhase: Long?): Flow<SpeedTestProgress>
}

/** Planifie la vérification périodique du quota en arrière-plan. */
interface QuotaMonitorScheduler {
    fun start()
    fun stop()
}

interface QuotaAlertNotifier {
    fun notifyThresholdReached(status: QuotaStatus)

    /** Invite à re-saisir le solde réel chez l'opérateur, relevé pour la dernière fois à [sinceMillis]. */
    fun notifyBalanceRecalibration(remainingBytes: Long, sinceMillis: Long)

    /** Prévient que le forfait arrive à son terme le [renewalMillis] et qu'il faudra le recharger. */
    fun notifyPlanRenewal(renewalMillis: Long)

    /** Prévient que les appareils connectés au partage ont consommé [usedBytes], au-delà de [thresholdBytes]. */
    fun notifyTetheringThreshold(usedBytes: Long, thresholdBytes: Long)
}

/** Remonte les erreurs rattrapées qui méritent d'être corrigées, sans donnée personnelle. */
interface CrashReporter {
    fun recordException(throwable: Throwable)
}

/** Mesure d'audience anonyme : écrans consultés. */
interface AnalyticsTracker {
    fun logScreenView(screenName: String)
}
