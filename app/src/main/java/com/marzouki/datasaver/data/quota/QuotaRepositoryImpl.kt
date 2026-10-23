package com.marzouki.datasaver.data.quota

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuotaRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : QuotaRepository {

    override val quota: Flow<DataQuota?> = dataStore.data
        .map(::readQuota)
        .distinctUntilChanged()

    override val lastAlertedMonth: Flow<String?> = dataStore.data.map { it[KEY_LAST_ALERTED_MONTH] }

    override val cycleStartDay: Flow<Int> = dataStore.data
        .map { it[KEY_CYCLE_START_DAY] ?: 1 }
        .distinctUntilChanged()

    /** Celui du forfait s'il y en a un ; sans forfait, le prix du Go saisi tel quel. */
    override val pricePerGb: Flow<PricePerGb?> = dataStore.data
        .map { prefs -> readQuota(prefs)?.pricePerGb ?: readPricePerGb(prefs).takeIf { prefs[KEY_QUOTA_BYTES] == null } }
        .distinctUntilChanged()

    override suspend fun setQuota(quota: DataQuota?) {
        dataStore.edit { prefs ->
            if (quota == null) {
                // Sans forfait, les économies restent chiffrées au prix du Go qu'il donnait.
                readQuota(prefs)?.pricePerGb?.let { prefs[KEY_PRICE_PER_GB_MILLIMES] = it.millimes }
                // La préférence d'alerte est conservée pour le prochain quota.
                prefs.remove(KEY_QUOTA_BYTES)
                prefs.remove(KEY_PLAN_PRICE_MILLIMES)
                prefs.remove(KEY_BALANCE_REMAINING_BYTES)
                prefs.remove(KEY_BALANCE_AT_MILLIS)
            } else {
                prefs[KEY_QUOTA_BYTES] = quota.limitBytes
                prefs[KEY_ALERT_ENABLED] = quota.alertEnabled
                val price = quota.priceMillimes
                if (price == null) prefs.remove(KEY_PLAN_PRICE_MILLIMES) else prefs[KEY_PLAN_PRICE_MILLIMES] = price
                // Le prix du Go d'avant les prix de forfait est désormais porté par le forfait.
                prefs.remove(KEY_PRICE_PER_GB_MILLIMES)
                val balance = quota.balance
                if (balance == null) {
                    prefs.remove(KEY_BALANCE_REMAINING_BYTES)
                    prefs.remove(KEY_BALANCE_AT_MILLIS)
                } else {
                    prefs[KEY_BALANCE_REMAINING_BYTES] = balance.remainingBytes
                    prefs[KEY_BALANCE_AT_MILLIS] = balance.atMillis
                }
            }
        }
    }

    override suspend fun setLastAlertedMonth(monthKey: String?) {
        dataStore.edit { prefs ->
            if (monthKey == null) prefs.remove(KEY_LAST_ALERTED_MONTH) else prefs[KEY_LAST_ALERTED_MONTH] = monthKey
        }
    }

    override suspend fun setCycleStartDay(day: Int) {
        dataStore.edit { it[KEY_CYCLE_START_DAY] = day }
    }

    override suspend fun setPricePerGb(price: PricePerGb?) {
        dataStore.edit { prefs ->
            if (price == null) prefs.remove(KEY_PRICE_PER_GB_MILLIMES) else prefs[KEY_PRICE_PER_GB_MILLIMES] = price.millimes
        }
    }

    override val balanceReminderAt: Flow<Long?> = dataStore.data.map { it[KEY_BALANCE_REMINDER_AT] }

    override suspend fun setBalanceReminderAt(millis: Long?) {
        dataStore.edit { prefs ->
            if (millis == null) prefs.remove(KEY_BALANCE_REMINDER_AT) else prefs[KEY_BALANCE_REMINDER_AT] = millis
        }
    }

    override val lastRenewalReminderCycle: Flow<String?> = dataStore.data.map { it[KEY_LAST_RENEWAL_REMINDER_CYCLE] }

    override suspend fun setLastRenewalReminderCycle(cycleKey: String?) {
        dataStore.edit { prefs ->
            if (cycleKey == null) {
                prefs.remove(KEY_LAST_RENEWAL_REMINDER_CYCLE)
            } else {
                prefs[KEY_LAST_RENEWAL_REMINDER_CYCLE] = cycleKey
            }
        }
    }

    override val planSetupDone: Flow<Boolean> = dataStore.data
        .map { it[KEY_PLAN_SETUP_DONE] ?: false }
        .distinctUntilChanged()

    override suspend fun setPlanSetupDone(done: Boolean) {
        dataStore.edit { it[KEY_PLAN_SETUP_DONE] = done }
    }

    override val planOperatorKey: Flow<String?> = dataStore.data
        .map { it[KEY_PLAN_OPERATOR] }
        .distinctUntilChanged()

    override suspend fun setPlanOperatorKey(key: String?) {
        dataStore.edit { prefs ->
            if (key == null) prefs.remove(KEY_PLAN_OPERATOR) else prefs[KEY_PLAN_OPERATOR] = key
        }
    }

    override val planValidity: Flow<PlanValidity?> = dataStore.data
        .map { prefs ->
            val purchase = prefs[KEY_PASS_PURCHASE_MILLIS] ?: return@map null
            val days = prefs[KEY_PASS_VALIDITY_DAYS]?.takeIf { it in 1..MonthPeriod.MAX_VALIDITY_DAYS } ?: return@map null
            PlanValidity(purchase, days)
        }
        .distinctUntilChanged()

    override suspend fun setPlanValidity(validity: PlanValidity?) {
        dataStore.edit { prefs ->
            if (validity == null) {
                prefs.remove(KEY_PASS_PURCHASE_MILLIS)
                prefs.remove(KEY_PASS_VALIDITY_DAYS)
            } else {
                prefs[KEY_PASS_PURCHASE_MILLIS] = validity.purchaseMillis
                prefs[KEY_PASS_VALIDITY_DAYS] = validity.days
            }
        }
    }

    override val savedPlans: Flow<List<SavedPlan>> = dataStore.data
        .map { prefs -> prefs[KEY_SAVED_PLANS]?.lineSequence()?.mapNotNull(::decodePlan)?.toList().orEmpty() }
        .distinctUntilChanged()

    override suspend fun setSavedPlans(plans: List<SavedPlan>) {
        dataStore.edit { prefs ->
            if (plans.isEmpty()) prefs.remove(KEY_SAVED_PLANS) else prefs[KEY_SAVED_PLANS] = plans.joinToString("\n", transform = ::encodePlan)
        }
    }

    // Une ligne par forfait, champs séparés par une tabulation ; "-" pour une valeur absente.
    private fun encodePlan(plan: SavedPlan): String = listOf(
        plan.operatorKey.replace(Regex("[\t\n]"), " "),
        plan.limitBytes,
        plan.priceMillimes ?: "-",
        plan.validityDays ?: "-",
    ).joinToString("\t")

    private fun decodePlan(line: String): SavedPlan? {
        val fields = line.split('\t')
        if (fields.size != 4) return null
        val limit = fields[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        return SavedPlan(
            operatorKey = fields[0].ifBlank { return null },
            limitBytes = limit,
            priceMillimes = fields[2].toLongOrNull()?.takeIf { it in 1..PricePerGb.MAX_PLAN_MILLIMES },
            validityDays = fields[3].toIntOrNull()?.takeIf { it in 1..MonthPeriod.MAX_VALIDITY_DAYS },
        )
    }

    private fun readQuota(prefs: Preferences): DataQuota? {
        val limitBytes = prefs[KEY_QUOTA_BYTES]?.takeIf { it > 0 } ?: return null
        // Avant les prix de forfait, seul le prix du Go était connu : il en donne le prix du forfait.
        val price = prefs[KEY_PLAN_PRICE_MILLIMES] ?: readPricePerGb(prefs)?.planMillimes(limitBytes)
        return DataQuota(
            limitBytes,
            alertEnabled = prefs[KEY_ALERT_ENABLED] ?: true,
            balance = readBalance(prefs, limitBytes),
            priceMillimes = price?.takeIf { it in 1..PricePerGb.MAX_PLAN_MILLIMES },
        )
    }

    private fun readPricePerGb(prefs: Preferences): PricePerGb? =
        prefs[KEY_PRICE_PER_GB_MILLIMES]?.takeIf { it in 1..PricePerGb.MAX_MILLIMES }?.let(::PricePerGb)

    private fun readBalance(prefs: Preferences, limitBytes: Long): OperatorBalance? {
        val remaining = prefs[KEY_BALANCE_REMAINING_BYTES]?.takeIf { it in 0..limitBytes } ?: return null
        val at = prefs[KEY_BALANCE_AT_MILLIS] ?: return null
        return OperatorBalance(remaining, at)
    }

    private companion object {
        val KEY_QUOTA_BYTES = longPreferencesKey("mobile_quota_bytes")
        val KEY_ALERT_ENABLED = booleanPreferencesKey("quota_alert_enabled")
        val KEY_LAST_ALERTED_MONTH = stringPreferencesKey("last_alerted_month")
        val KEY_CYCLE_START_DAY = intPreferencesKey("cycle_start_day")
        /** Prix du Go, n'est plus utilisé que sans forfait. */
        val KEY_PRICE_PER_GB_MILLIMES = intPreferencesKey("price_per_gb_millimes")
        val KEY_PLAN_PRICE_MILLIMES = longPreferencesKey("plan_price_millimes")
        val KEY_BALANCE_REMINDER_AT = longPreferencesKey("balance_reminder_at")
        val KEY_LAST_RENEWAL_REMINDER_CYCLE = stringPreferencesKey("last_renewal_reminder_cycle")
        val KEY_PLAN_SETUP_DONE = booleanPreferencesKey("plan_setup_done")
        val KEY_PLAN_OPERATOR = stringPreferencesKey("plan_operator_key")
        val KEY_PASS_PURCHASE_MILLIS = longPreferencesKey("pass_purchase_millis")
        val KEY_PASS_VALIDITY_DAYS = intPreferencesKey("pass_validity_days")
        val KEY_SAVED_PLANS = stringPreferencesKey("saved_plans")
        val KEY_BALANCE_REMAINING_BYTES = longPreferencesKey("operator_balance_remaining_bytes")
        val KEY_BALANCE_AT_MILLIS = longPreferencesKey("operator_balance_at_millis")
    }
}
