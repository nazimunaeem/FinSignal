package com.cardbill

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CardBillApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .apply {
                if (::workerFactory.isInitialized) {
                    setWorkerFactory(workerFactory)
                }
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val dueDateChannel = NotificationChannel(
            DUE_DATE_CHANNEL,
            "Due Date Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming credit card due dates"
        }

        val overdueChannel = NotificationChannel(
            OVERDUE_CHANNEL,
            "Overdue Bills",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for overdue credit card bills"
        }

        val scanChannel = NotificationChannel(
            SCAN_CHANNEL,
            "SMS Scan",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background SMS scanning status"
        }

        manager.createNotificationChannel(dueDateChannel)
        manager.createNotificationChannel(overdueChannel)
        manager.createNotificationChannel(scanChannel)
    }

    companion object {
        const val DUE_DATE_CHANNEL = "due_date_reminders"
        const val OVERDUE_CHANNEL = "overdue_bills"
        const val SCAN_CHANNEL = "sms_scan"
    }
}
