package com.finsignal.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

data class SmsMessage(
    val address: String?,
    val body: String,
    val date: Long
)

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityLogger: com.finsignal.data.log.ActivityLogger
) {
    companion object {
        private val FILTER_KEYWORDS = listOf(
            "bill", "credit card", "total due", "min due", "minimum due", "due date",
            "AMEX", "payment", "card#", "card:", "last date", "tk", "bdt", "pay by",
            "statement", "monthly bill", "outstanding", "outstanding amount", "total outstanding",
            "PRIME", "PUBALI", "BRAC", "CITY", "EBL", "EASTERN"
        )
        private const val BATCH_SIZE = 200
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Streams SMS messages in batches. Pass 0 for afterTimestamp to scan ALL messages.
     * Default is 60 days for incremental scans. First scan should use 0.
     */
    fun readAllSmsStream(
        afterTimestamp: Long = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000),
        batchSize: Int = BATCH_SIZE
    ): Flow<List<SmsMessage>> = flow {
        if (!hasSmsPermission()) {
            activityLogger.error("SmsReader", "READ_SMS permission missing; emitting empty")
            return@flow
        }

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = if (afterTimestamp > 0) {
            "${Telephony.Sms.DATE} > ?"
        } else null

        val selectionArgs = if (afterTimestamp > 0) {
            arrayOf(afterTimestamp.toString())
        } else null

        val sortOrder = "${Telephony.Sms.DATE} ASC"

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val totalCount = cursor.count
                activityLogger.info("SmsReader", "Streaming $totalCount total messages in batches of $batchSize")

                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                var batch = mutableListOf<SmsMessage>()
                var processedCount = 0
                var matchedCount = 0

                while (cursor.moveToNext()) {
                    try {
                        val body = cursor.getString(bodyIdx) ?: continue
                        val sender = cursor.getString(addressIdx) ?: ""
                        val lowerBody = body.lowercase()
                        val lowerSender = sender.lowercase()

                        val isBankSms = FILTER_KEYWORDS.any { keyword ->
                            val kw = keyword.lowercase()
                            lowerBody.contains(kw) || lowerSender.contains(kw)
                        }

                        if (isBankSms) {
                            batch.add(
                                SmsMessage(
                                    address = sender,
                                    body = body,
                                    date = cursor.getLong(dateIdx)
                                )
                            )
                            matchedCount++
                        }
                    } catch (e: Exception) {
                        activityLogger.error("SmsReader", "Error reading SMS row: ${e.message}")
                    }

                    processedCount++

                    if (batch.size >= batchSize) {
                        activityLogger.info("SmsReader", "Emitting batch: ${batch.size} matched (processed $processedCount/$totalCount)")
                        emit(batch.toList())
                        batch = mutableListOf()
                    }
                }

                if (batch.isNotEmpty()) {
                    activityLogger.info("SmsReader", "Emitting final batch: ${batch.size} matched (total $matchedCount/$processedCount processed)")
                    emit(batch)
                } else {
                    activityLogger.info("SmsReader", "Scan complete: $matchedCount matched out of $processedCount processed")
                }
            } ?: run {
                activityLogger.error("SmsReader", "Query returned NULL cursor")
            }
        } catch (e: SecurityException) {
            activityLogger.error("SmsReader", "SecurityException reading SMS: ${e.message}")
        } catch (e: Exception) {
            activityLogger.error("SmsReader", "Exception reading SMS: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun readRecentSms(count: Int = 500): List<SmsMessage> {
        if (!hasSmsPermission()) {
            activityLogger.error("SmsReader", "readRecentSms: Permission missing")
            return emptyList()
        }

        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            context.contentResolver.query(
                uri, projection, null, null, "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                activityLogger.info("SmsReader", "readRecentSms: Query found ${cursor.count} total messages")
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                var readCount = 0
                while (cursor.moveToNext() && readCount < count) {
                    val body = cursor.getString(bodyIdx) ?: continue
                    messages.add(
                        SmsMessage(
                            address = cursor.getString(addressIdx),
                            body = body,
                            date = cursor.getLong(dateIdx)
                        )
                    )
                    readCount++
                }
            } ?: activityLogger.error("SmsReader", "readRecentSms: Query returned NULL")
        } catch (e: Exception) {
            activityLogger.error("SmsReader", "readRecentSms: Exception: ${e.message}")
            return emptyList()
        }

        return messages
    }
}
