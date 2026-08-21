package com.cardbill.data.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cardbill.data.local.PreferenceManager
import com.cardbill.data.local.dao.BillDao
import com.cardbill.data.local.dao.CreditCardDao
import com.cardbill.data.local.dao.SmsRecordDao
import com.cardbill.data.local.entity.Bill
import com.cardbill.data.local.entity.CreditCard
import com.cardbill.data.local.entity.SmsRecord
import com.cardbill.data.log.ActivityLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

@HiltWorker
class SmsScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val smsReader: SmsReader,
    private val cardDao: CreditCardDao,
    private val billDao: BillDao,
    private val smsRecordDao: SmsRecordDao,
    private val activityLogger: ActivityLogger,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsScanWorker"

        // Process-wide lock: the periodic scan ("sms_scan") and on-demand scans
        // ("sms_scan_now") use different WorkManager unique names and could otherwise
        // run at the same time, parsing the same SMS twice.
        private val scanMutex = kotlinx.coroutines.sync.Mutex()
    }

    override suspend fun doWork(): Result {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            scanMutex.withLock {
                activityLogger.info(TAG, "Starting SMS scan...")
                try {
                    val isFirstScan = !preferenceManager.isFirstScanComplete.first()
                    val afterTimestamp = if (isFirstScan) 0L else System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)

                    activityLogger.info(TAG, "Scan mode: ${if (isFirstScan) "FIRST (all SMS)" else "incremental (60 days)"}")

                    var totalRead = 0
                    var parsedCount = 0
                    var errorCount = 0

                    smsReader.readAllSmsStream(afterTimestamp = afterTimestamp).collect { batch ->
                        for (message in batch) {
                            try {
                                totalRead++
                                val parsed = processMessage(message)
                                if (parsed != null) {
                                    parsedCount++
                                }
                            } catch (e: Exception) {
                                errorCount++
                                activityLogger.error(TAG, "Error processing SMS #$totalRead: ${e.message}")
                            }
                        }

                        activityLogger.info(TAG, "Batch done: $totalRead total, $parsedCount parsed, $errorCount errors")
                    }

                    // Remove legacy duplicate rows (same card + month + currency) created
                    // before period normalization existed, then keep only the latest unpaid
                    // bill per card+currency active — older unpaid bills are superseded
                    // (their dues are included in the newer bill).
                    removeDuplicateBills()
                    billDao.markAllButLatestSuperseded()

                    activityLogger.info(TAG, "SMS scan completed. Total: $totalRead, Parsed: $parsedCount, Errors: $errorCount")

                    val now = System.currentTimeMillis()
                    val ninetyDaysAgo = now - (90L * 24 * 60 * 60 * 1000)
                    smsRecordDao.deleteOldParsedSms(ninetyDaysAgo)

                    // Clean up SMS rows duplicated by the old concurrent-scan race:
                    // keep one row per body, carrying over the parsed flag.
                    smsRecordDao.mergeParsedStatusOfDuplicates()
                    smsRecordDao.deleteDuplicateSmsRecords()

                    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                    billDao.clearSmsBodyForOldPaidBills(thirtyDaysAgo)

                    preferenceManager.setFirstScanComplete(true)

                    Result.success()
                } catch (e: Exception) {
                activityLogger.error(TAG, "Fatal error during SMS scan: ${e.message}")
                Result.retry()
                }
            }
        }
    }

    private suspend fun processMessage(message: SmsMessage): ParsedBill? {
        val existingRecord = smsRecordDao.findByBody(message.body)
        var smsId = existingRecord?.id ?: 0L

        if (existingRecord == null) {
            smsId = smsRecordDao.insert(
                SmsRecord(
                    address = message.address,
                    body = message.body,
                    timestamp = message.date
                )
            )
        }

        val parsed = BankSmsParser.parse(message.body, message.address) ?: return null

        if (smsId > 0) {
            smsRecordDao.updateParsedStatus(smsId, true)
        }

        var card = cardDao.findCard(parsed.bankName, parsed.cardLast4)
        if (card == null) {
            activityLogger.info(TAG, "New card: ${parsed.bankName} •••• ${parsed.cardLast4}")
            cardDao.insertCard(
                CreditCard(
                    bankName = parsed.bankName,
                    cardLast4 = parsed.cardLast4,
                    clientId = parsed.clientId
                )
            )
            card = cardDao.findCard(parsed.bankName, parsed.cardLast4)
        }

        if (card != null) {
            // Compare periods in canonical form so format changes ("Aug-2026" vs
            // "AUG-26") don't create duplicate bills for the same month. Currency is
            // part of the identity: an EBL card can have a BDT and a USD bill for the
            // same statement month.
            val normalizedPeriod = BankSmsParser.normalizeBillPeriod(parsed.billPeriod)
            val isDuplicate = billDao.getBillsForCardOnce(card.id).any {
                BankSmsParser.normalizeBillPeriod(it.billPeriod) == normalizedPeriod &&
                    it.currency.equals(parsed.currency, ignoreCase = true)
            }

            // Retire older unpaid bills of the SAME currency even when this bill already
            // exists — a repeated SMS must still supersede any older unpaid bill that
            // slipped through. Other-currency bills (e.g. USD) are independent ledgers.
            billDao.markOlderBillsSuperseded(card.id, parsed.currency, parsed.dueDate)

            if (!isDuplicate) {
                billDao.insertBill(
                    Bill(
                        cardId = card.id,
                        billPeriod = normalizedPeriod,
                        totalDue = parsed.totalDue,
                        minDue = parsed.minDue,
                        dueDate = parsed.dueDate,
                        smsBody = message.body,
                        currency = parsed.currency
                    )
                )
            }
        }

        return parsed
    }

    private suspend fun removeDuplicateBills() {
        val allBills = billDao.getAllBillsOnce()
        val duplicateIds = allBills
            .groupBy { Triple(it.cardId, BankSmsParser.normalizeBillPeriod(it.billPeriod), it.currency.uppercase()) }
            .values
            .flatMap { group ->
                if (group.size <= 1) emptyList()
                else group.sortedBy { it.id }.dropLast(1).map { it.id }
            }
        if (duplicateIds.isNotEmpty()) {
            activityLogger.info(TAG, "Removing ${duplicateIds.size} duplicate bill row(s)")
            billDao.deleteBillsByIds(duplicateIds)
        }
    }
}
