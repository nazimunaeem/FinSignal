package com.cardbill.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cardbill.data.local.PreferenceManager
import com.cardbill.data.local.dao.BillDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class DueDateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val billDao: BillDao,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val enabled = preferenceManager.notificationsEnabled.first()
            if (!enabled) return Result.success()

            val rules = preferenceManager.alertRules.first()
            val alertTime = preferenceManager.alertTime.first()
            val (alertHour, _) = alertTime.split(":").map { it.toInt() }
            
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            val bills = billDao.getUnpaidBillsWithCards().first()

            for (bill in bills) {
                val days = bill.daysUntilDue.toInt()
                var shouldNotify = false

                when {
                    rules.contains("DAILY") && currentHour == alertHour -> shouldNotify = true
                    rules.contains("DUE_4_DAILY") && days == 4 && currentHour == alertHour -> shouldNotify = true
                    rules.contains("DUE_3_DAILY") && days == 3 && currentHour == alertHour -> shouldNotify = true
                    rules.contains("DUE_2_6H") && days == 2 && currentHour % 6 == 0 -> shouldNotify = true
                    rules.contains("DUE_2_12H") && days == 2 && currentHour % 12 == 0 -> shouldNotify = true
                    rules.contains("DUE_1_1H") && days == 1 -> shouldNotify = true
                    rules.contains("DUE_1_3H") && days == 1 && currentHour % 3 == 0 -> shouldNotify = true
                    days < 0 && currentHour == alertHour -> shouldNotify = true // Always notify overdue daily
                }

                if (shouldNotify) {
                    if (days < 0) {
                        NotificationHelper.showOverdueAlert(applicationContext, bill)
                    } else {
                        NotificationHelper.showDueDateReminder(applicationContext, bill, days)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
