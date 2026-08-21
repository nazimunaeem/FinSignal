package com.finsignal.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finsignal.data.local.PreferenceManager
import com.finsignal.data.sms.SmsScanWorker
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun preferenceManager(): PreferenceManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SmsScheduler.schedule(context)
            
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
            val preferenceManager = entryPoint.preferenceManager()
            
            CoroutineScope(Dispatchers.IO).launch {
                DueDateReminderScheduler.schedule(context, preferenceManager)
            }
        }
    }
}
