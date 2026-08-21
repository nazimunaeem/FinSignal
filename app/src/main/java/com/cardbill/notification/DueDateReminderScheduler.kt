package com.cardbill.notification

import android.content.Context
import androidx.work.*
import com.cardbill.data.local.PreferenceManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object DueDateReminderScheduler {

    private const val WORK_NAME = "due_date_check"

    suspend fun schedule(context: Context, preferenceManager: PreferenceManager) {
        val enabled = preferenceManager.notificationsEnabled.first()
        if (!enabled) {
            cancel(context)
            return
        }

        val rules = preferenceManager.alertRules.first()
        if (rules.isEmpty()) {
            cancel(context)
            return
        }

        // Determine the minimum interval needed
        val hasHourly = rules.contains("DUE_1_1H") || rules.contains("DUE_2_6H") || rules.contains("DUE_2_12H") || rules.contains("DUE_1_3H")
        
        val intervalMinutes = if (hasHourly) 15L else 60L // Use 15m or 1h for check granularity
        
        val request = PeriodicWorkRequestBuilder<DueDateCheckWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
