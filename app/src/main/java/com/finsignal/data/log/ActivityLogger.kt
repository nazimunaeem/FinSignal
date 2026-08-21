package com.finsignal.data.log

import android.util.Log
import com.finsignal.data.local.AppDatabase
import com.finsignal.data.local.dao.ActivityLogDao
import com.finsignal.data.local.entity.ActivityLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

@Singleton
class ActivityLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dao: ActivityLogDao by lazy {
        AppDatabase.getInstance(context).activityLogDao()
    }

    fun info(tag: String?, message: String) {
        Log.i(tag ?: "ActivityLogger", message)
        insert("INFO", tag, message)
    }

    fun error(tag: String?, message: String) {
        Log.e(tag ?: "ActivityLogger", message)
        insert("ERROR", tag, message)
    }

    private fun insert(level: String, tag: String?, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.insert(
                    ActivityLog(
                        timestamp = Date().time,
                        level = level,
                        tag = tag,
                        message = message
                    )
                )
            } catch (_: Exception) {
                // best-effort logging; don't crash
            }
        }
    }
}
