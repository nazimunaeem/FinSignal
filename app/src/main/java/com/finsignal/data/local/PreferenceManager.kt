package com.finsignal.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val ALERT_RULES = stringSetPreferencesKey("alert_rules")
    private val ALERT_TIME = stringPreferencesKey("alert_time")
    private val FIRST_SCAN_COMPLETE = booleanPreferencesKey("first_scan_complete")

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val alertRules: Flow<Set<String>> = context.dataStore.data.map {
        it[ALERT_RULES] ?: setOf("DAILY")
    }
    val alertTime: Flow<String> = context.dataStore.data.map { it[ALERT_TIME] ?: "09:00" }
    val isFirstScanComplete: Flow<Boolean> = context.dataStore.data.map { it[FIRST_SCAN_COMPLETE] ?: false }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAlertRules(rules: Set<String>) {
        context.dataStore.edit { it[ALERT_RULES] = rules }
    }

    suspend fun toggleAlertRule(rule: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[ALERT_RULES] ?: setOf("DAILY")
            val newSet = if (current.contains(rule)) {
                current - rule
            } else {
                current + rule
            }
            preferences[ALERT_RULES] = newSet
        }
    }

    suspend fun setAlertTime(time: String) {
        context.dataStore.edit { it[ALERT_TIME] = time }
    }

    suspend fun setFirstScanComplete(complete: Boolean) {
        context.dataStore.edit { it[FIRST_SCAN_COMPLETE] = complete }
    }
}
