package com.finsignal.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finsignal.data.sms.SmsScanWorker
import java.util.concurrent.TimeUnit

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder

object SmsScheduler {

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SmsScanWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sms_scan",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scanNow(context: Context, force: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SmsScanWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sms_scan_now",
            // KEEP: if a scan is already running/queued (e.g. triggered by a previous
            // SMS or the periodic job), don't restart it — restarting caused the same
            // SMS to be parsed twice. Manual full rescans pass force = true.
            if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("sms_scan")
    }
}
