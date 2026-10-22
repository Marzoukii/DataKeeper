package com.marzouki.datasaver.data.quota

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.OperatorBalance
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuotaRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : QuotaRepository {

    override val quota: Flow<DataQuota?> = dataStore.data
        .map { prefs ->
            prefs[KEY_QUOTA_BYTES]?.takeIf { it > 0 }?.let {
                DataQuota(it, alertEnabled = prefs[KEY_ALERT_ENABLED] ?: true, balance = readBalance(prefs, limitBytes = it))
            }
        }
        .distinctUntilChanged()

    override val lastAlertedMonth: Flow<String?> = dataStore.data.map { it[KEY_LAST_ALERTED_MONTH] }

    override val cycleStartDay: Flow<Int> = dataStore.data
        .map { it[KEY_CYCLE_START_DAY] ?: 1 }
        .distinctUntilChanged()

    override val pricePerGb: Flow<PricePerGb?> = dataStore.data
        .map { prefs -> prefs[KEY_PRICE_PER_GB_MILLIMES]?.takeIf { it in 1..PricePerGb.MAX_MILLIMES }?.let(::PricePerGb) }
        .distinctUntilChanged()

    override suspend fun setQuota(quota: DataQuota?) {
        dataStore.edit { prefs ->
            if (quota == null) {
                // La préférence d'alerte est conservée pour le prochain quota.
                prefs.remove(KEY_QUOTA_BYTES)
                prefs.remove(KEY_BALANCE_REMAINING_BYTES)
                prefs.remove(KEY_BALANCE_AT_MILLIS)
            } else {
                prefs[KEY_QUOTA_BYTES] = quota.limitBytes
                prefs[KEY_ALERT_ENABLED] = quota.alertEnabled
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
        val KEY_PRICE_PER_GB_MILLIMES = intPreferencesKey("price_per_gb_millimes")
        val KEY_BALANCE_REMAINING_BYTES = longPreferencesKey("operator_balance_remaining_bytes")
        val KEY_BALANCE_AT_MILLIS = longPreferencesKey("operator_balance_at_millis")
    }
}
