package com.marzouki.datasaver.data.quota

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marzouki.datasaver.domain.model.DataQuota
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
                DataQuota(it, alertEnabled = prefs[KEY_ALERT_ENABLED] ?: true)
            }
        }
        .distinctUntilChanged()

    override val lastAlertedMonth: Flow<String?> = dataStore.data.map { it[KEY_LAST_ALERTED_MONTH] }

    override val cycleStartDay: Flow<Int> = dataStore.data
        .map { it[KEY_CYCLE_START_DAY] ?: 1 }
        .distinctUntilChanged()

    override suspend fun setQuota(quota: DataQuota?) {
        dataStore.edit { prefs ->
            if (quota == null) {
                // La préférence d'alerte est conservée pour le prochain quota.
                prefs.remove(KEY_QUOTA_BYTES)
            } else {
                prefs[KEY_QUOTA_BYTES] = quota.limitBytes
                prefs[KEY_ALERT_ENABLED] = quota.alertEnabled
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

    private companion object {
        val KEY_QUOTA_BYTES = longPreferencesKey("mobile_quota_bytes")
        val KEY_ALERT_ENABLED = booleanPreferencesKey("quota_alert_enabled")
        val KEY_LAST_ALERTED_MONTH = stringPreferencesKey("last_alerted_month")
        val KEY_CYCLE_START_DAY = intPreferencesKey("cycle_start_day")
    }
}
