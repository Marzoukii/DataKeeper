package com.marzouki.datasaver.data.quota

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
        .map { prefs -> prefs[KEY_QUOTA_BYTES]?.takeIf { it > 0 }?.let { DataQuota(it) } }
        .distinctUntilChanged()

    override val lastAlertedMonth: Flow<String?> = dataStore.data.map { it[KEY_LAST_ALERTED_MONTH] }

    override suspend fun setQuota(quota: DataQuota?) {
        dataStore.edit { prefs ->
            if (quota == null) prefs.remove(KEY_QUOTA_BYTES) else prefs[KEY_QUOTA_BYTES] = quota.limitBytes
        }
    }

    override suspend fun setLastAlertedMonth(monthKey: String?) {
        dataStore.edit { prefs ->
            if (monthKey == null) prefs.remove(KEY_LAST_ALERTED_MONTH) else prefs[KEY_LAST_ALERTED_MONTH] = monthKey
        }
    }

    private companion object {
        val KEY_QUOTA_BYTES = longPreferencesKey("mobile_quota_bytes")
        val KEY_LAST_ALERTED_MONTH = stringPreferencesKey("last_alerted_month")
    }
}
