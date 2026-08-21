package com.cardbill.data.repository

import com.cardbill.data.local.dao.BillDao
import com.cardbill.data.local.entity.Bill
import com.cardbill.data.local.entity.BillWithCard
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

    fun getTotalUnpaid(): Flow<Double?> = billDao.getTotalUnpaid()

    fun getUnpaidCount(): Flow<Int> = billDao.getUnpaidCount()

    fun getOverdueCount(today: String): Flow<Int> = billDao.getOverdueCount(today)

    fun getUnpaidTotalForCard(cardId: Long): Flow<Double?> = billDao.getUnpaidTotalForCard(cardId)

    suspend fun addBill(bill: Bill): Long = billDao.insertBill(bill)

    suspend fun markAsPaid(billId: Long) = billDao.markAsPaid(billId)

    suspend fun updatePaidAmount(billId: Long, amount: Double) = billDao.updatePaidAmount(billId, amount)

    suspend fun markAsUnpaid(billId: Long) = billDao.markAsUnpaid(billId)

    suspend fun updateBillDetails(billId: Long, totalDue: Double, minDue: Double, dueDate: String) =
        billDao.updateBillDetails(billId, totalDue, minDue, dueDate)

    suspend fun findExistingBill(cardId: Long, period: String): Bill? =
        billDao.findExistingBill(cardId, period)

    suspend fun findBillBySms(smsBody: String): Bill? = billDao.findBillBySms(smsBody)
}
