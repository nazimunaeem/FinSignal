package com.finsignal.data.repository

import com.finsignal.data.local.dao.BillDao
import com.finsignal.data.local.entity.Bill
import com.finsignal.data.local.entity.BillWithCard
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillRepository @Inject constructor(
    private val billDao: BillDao
) {
    fun getUnpaidBills(): Flow<List<BillWithCard>> = billDao.getUnpaidBillsWithCards()

    fun getAllBills(): Flow<List<BillWithCard>> = billDao.getAllBillsWithCards()

    fun getBillsForCard(cardId: Long): Flow<List<BillWithCard>> = billDao.getBillsForCard(cardId)

    fun getUnpaidCount(): Flow<Int> = billDao.getUnpaidCount()

    fun getPaidTotalForCard(cardId: Long): Flow<Double?> = billDao.getPaidTotalForCard(cardId)

    suspend fun addBill(bill: Bill): Long = billDao.insertBill(bill)

    suspend fun markAsPaid(billId: Long) = billDao.markAsPaid(billId)

    suspend fun updatePaidAmount(billId: Long, amount: Double) = billDao.updatePaidAmount(billId, amount)

    suspend fun markAsUnpaid(billId: Long) = billDao.markAsUnpaid(billId)

    suspend fun updateBillDetails(billId: Long, totalDue: Double, minDue: Double, dueDate: String) =
        billDao.updateBillDetails(billId, totalDue, minDue, dueDate)

    suspend fun findExistingBill(cardId: Long, period: String): Bill? =
        billDao.findExistingBill(cardId, period)

    suspend fun findBillBySms(smsBody: String): Bill? = billDao.findBillBySms(smsBody)

    suspend fun getBillsForCardOnce(cardId: Long): List<Bill> = billDao.getBillsForCardOnce(cardId)

    suspend fun getAllBillsOnce(): List<Bill> = billDao.getAllBillsOnce()

    suspend fun deleteBillsByIds(ids: List<Long>) = billDao.deleteBillsByIds(ids)

    suspend fun markOlderBillsSuperseded(cardId: Long, currency: String, newBillDueDate: String) =
        billDao.markOlderBillsSuperseded(cardId, currency, newBillDueDate)

    suspend fun markAllButLatestSuperseded() = billDao.markAllButLatestSuperseded()

    suspend fun clearSmsBodyForOldPaidBills(beforeTimestamp: Long) =
        billDao.clearSmsBodyForOldPaidBills(beforeTimestamp)
}
