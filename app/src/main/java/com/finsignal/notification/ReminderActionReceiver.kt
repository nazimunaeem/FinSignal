package com.finsignal.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.finsignal.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.finsignal.ACTION_MARK_PAID") {
            val billId = intent.getLongExtra(NotificationHelper.EXTRA_BILL_ID, -1)
            if (billId != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getInstance(context)
                    db.billDao().markAsPaid(billId)
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                    manager.cancel(billId.toInt())
                    manager.cancel(billId.toInt() + 50000)
                }
            }
        }
    }
}
