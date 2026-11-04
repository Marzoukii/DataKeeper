package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.domain.enums.NetworkGeneration
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
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.model.TetheringSession
import com.marzouki.datasaver.domain.model.WifiScan
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.ForfaitRepository
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.TetheringMonitorScheduler
import com.marzouki.datasaver.domain.repository.TetheringRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import com.marzouki.datasaver.domain.repository.WifiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

/** Faux dépôts et horloge partagés par les tests de ViewModels. */

const val GB = 1_000_000_000L

val ooredoo = MobileOperator("Ooredoo", "60503")
val tunisieTelecom = MobileOperator("Tunisie Telecom", "60502")

/** 20 octobre 2026 à 15 h, heure locale. */
val OCT_20: Long = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 20, 15, 0) }.timeInMillis

/** Horloge que le test avance à la main. */
class FakeClock(var nowMillis: Long = OCT_20) : Clock {
    override fun nowMillis(): Long = nowMillis
}

class FakeQuotaRepository(quota: DataQuota? = null) : QuotaRepository {
    override val quota = MutableStateFlow(quota)
    override val lastAlertedMonth = MutableStateFlow<String?>(null)
    override val cycleStartDay = MutableStateFlow(1)
    override val pricePerGb = MutableStateFlow<PricePerGb?>(null)
    override suspend fun setQuota(quota: DataQuota?) { this.quota.value = quota }
    override suspend fun setLastAlertedMonth(monthKey: String?) { lastAlertedMonth.value = monthKey }
    override suspend fun setCycleStartDay(day: Int) { cycleStartDay.value = day }
    override suspend fun setPricePerGb(price: PricePerGb?) { pricePerGb.value = price }
    override val balanceReminderAt = MutableStateFlow<Long?>(null)
    override suspend fun setBalanceReminderAt(millis: Long?) { balanceReminderAt.value = millis }
    override val lastRenewalReminderCycle = MutableStateFlow<String?>(null)
    override suspend fun setLastRenewalReminderCycle(cycleKey: String?) { lastRenewalReminderCycle.value = cycleKey }
    override val planSetupDone = MutableStateFlow(false)
    override suspend fun setPlanSetupDone(done: Boolean) { planSetupDone.value = done }
    override val planOperatorKey = MutableStateFlow<String?>(null)
    override suspend fun setPlanOperatorKey(key: String?) { planOperatorKey.value = key }
    override val planValidity = MutableStateFlow<PlanValidity?>(null)
    override suspend fun setPlanValidity(validity: PlanValidity?) { planValidity.value = validity }
    override val savedPlans = MutableStateFlow<List<SavedPlan>>(emptyList())
    override suspend fun setSavedPlans(plans: List<SavedPlan>) { savedPlans.value = plans }
}

class FakeUsageRepository(var mobileBytes: Long = 0) : UsageRepository {
    var hasAccess = true

    /** Lecture du trafic d'un cycle ; un test peut la faire attendre pour simuler un appel lent. */
    var usageFor: suspend (MonthPeriod) -> DataUsage = { DataUsage(0, mobileBytes, it) }

    override fun hasUsageAccess() = hasAccess
    override suspend fun getUsage(period: MonthPeriod) = usageFor(period)
    override suspend fun getDailyUsage(days: Int, nowMillis: Long) = emptyList<DayUsage>()
    override suspend fun getTopMobileApps(period: MonthPeriod, limit: Int) = emptyList<AppDataUsage>()
}

/** SIM des données, que le test change comme on change de carte. */
class FakeOperatorRepository(operator: MobileOperator? = ooredoo) : OperatorRepository {
    val operator = MutableStateFlow(operator)
    var permission = false
    val generation = MutableStateFlow<NetworkGeneration?>(NetworkGeneration.G5)
    override fun dataOperator() = operator.value
    override fun observeDataOperator() = operator
    override fun hasPhoneStatePermission() = permission
    override fun observeDataNetworkGeneration() = generation
}

/** Partage de connexion : session ouverte à 0 tant qu'il est allumé, compteur réglé par le test. */
class FakeTetheringRepository : TetheringRepository {
    val hotspot = MutableStateFlow(false)
    var counterBytes = 0L
    override fun observeHotspotActive() = hotspot
    override fun isHotspotActive() = hotspot.value
    override fun mobileCounterBytes() = counterBytes
    override suspend fun syncSession(active: Boolean, nowMillis: Long, counterBytes: Long) =
        if (active) TetheringSession(startMillis = 0, startKnown = true, baselineBytes = 0) else null
    override val alert = MutableStateFlow(TetheringAlert.DEFAULT)
    override suspend fun setAlert(alert: TetheringAlert) { this.alert.value = alert }
    override val alertedSessionStart = MutableStateFlow<Long?>(null)
    override suspend fun setAlertedSessionStart(startMillis: Long?) { alertedSessionStart.value = startMillis }
}

class FakeWifiRepository : WifiRepository {
    var availability = WifiAvailability.READY
    val scans = MutableStateFlow(WifiScan(emptyList(), scannedAtMillis = null, connectedSsid = null))
    override fun availability() = availability
    override fun observeOpenNetworks() = scans
    override fun requestScan() = true
}

/** API forfait simulée : renvoie [snapshot], ou lève [error] (panne réseau) s'il est non nul. */
class FakeForfaitRepository(
    var snapshot: ForfaitSnapshot? = null,
    var error: Throwable? = null,
) : ForfaitRepository {
    override suspend fun fetchForfait(operatorKey: String?): ForfaitSnapshot? {
        error?.let { throw it }
        return snapshot
    }
}

object NoopQuotaScheduler : QuotaMonitorScheduler {
    override fun start() = Unit
    override fun stop() = Unit
}

object NoopTetheringScheduler : TetheringMonitorScheduler {
    override fun start() = Unit
    override fun stop() = Unit
}
