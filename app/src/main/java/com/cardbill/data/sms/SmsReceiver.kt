package com.cardbill.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.cardbill.notification.SmsScheduler

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // New SMS arrived, trigger a scan now
            SmsScheduler.scanNow(context)
        }
    }
}
