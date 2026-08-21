package com.finsignal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finsignal.data.local.entity.Bill
import com.finsignal.data.local.entity.BillWithCard
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("""
        SELECT b.id AS billId, b.cardId, c.bankName, c.cardLast4, COALESCE(c.cardNickname, '') AS cardNickname,
               b.billPeriod, b.totalDue, b.minDue, b.dueDate, b.isPaid, b.paidAmount, b.paidAt,
               b.smsBody, COALESCE(b.currency, 'BDT') AS currency, b.detectedAt
        FROM bills b
        INNER JOIN credit_cards c ON b.cardId = c.id
        WHERE b.isPaid = 0 AND b.isSuperseded = 0
        ORDER BY b.dueDate ASC
    """)
    fun getUnpaidBillsWithCards(): Flow<List<BillWithCard>>

    @Query("""
        SELECT b.id AS billId, b.cardId, c.bankName, c.cardLast4, COALESCE(c.cardNickname, '') AS cardNickname,
               b.billPeriod, b.totalDue, b.minDue, b.dueDate, b.isPaid, b.paidAmount, b.paidAt,
               b.smsBody, COALESCE(b.currency, 'BDT') AS currency, b.detectedAt
        FROM bills b
        INNER JOIN credit_cards c ON b.cardId = c.id
        ORDER BY b.detectedAt DESC
    """)
    fun getAllBillsWithCards(): Flow<List<BillWithCard>>

    @Query("""
        SELECT b.id AS billId, b.cardId, c.bankName, c.cardLast4, COALESCE(c.cardNickname, '') AS cardNickname,
               b.billPeriod, b.totalDue, b.minDue, b.dueDate, b.isPaid, b.paidAmount, b.paidAt,
               b.smsBody, COALESCE(b.currency, 'BDT') AS currency, b.detectedAt
        FROM bills b
        INNER JOIN credit_cards c ON b.cardId = c.id
        WHERE b.cardId = :cardId
        ORDER BY b.detectedAt DESC
    """)
    fun getBillsForCard(cardId: Long): Flow<List<BillWithCard>>

    @Query("""
        SELECT b.id AS billId, b.cardId, c.bankName, c.cardLast4, COALESCE(c.cardNickname, '') AS cardNickname,
               b.billPeriod, b.totalDue, b.minDue, b.dueDate, b.isPaid, b.paidAmount, b.paidAt,
               b.smsBody, COALESCE(b.currency, 'BDT') AS currency, b.detectedAt
        FROM bills b
        INNER JOIN credit_cards c ON b.cardId = c.id
        ORDER BY b.detectedAt DESC
    """)
    fun getAllBillsWithCardsForHistory(): Flow<List<BillWithCard>>

    @Query("SELECT SUM(totalDue - paidAmount) FROM bills WHERE isPaid = 0 AND isSuperseded = 0")
    fun getTotalUnpaid(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM bills WHERE isPaid = 0 AND isSuperseded = 0")
    fun getUnpaidCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM bills WHERE isPaid = 0 AND isSuperseded = 0 AND
        (substr(dueDate,7,4) || substr(dueDate,4,2) || substr(dueDate,1,2)) <
        (substr(:today,7,4) || substr(:today,4,2) || substr(:today,1,2))
    """)
    fun getOverdueCount(today: String): Flow<Int>

    @Query("""
        SELECT SUM(totalDue) FROM bills
        WHERE isPaid = 0 AND isSuperseded = 0 AND cardId = :cardId
    """)
    fun getUnpaidTotalForCard(cardId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Query("DELETE FROM bills WHERE isPaid = 1 AND paidAt < :beforeTimestamp")
    suspend fun deleteOldPaidBills(beforeTimestamp: Long)

    @Query("UPDATE bills SET smsBody = NULL WHERE isPaid = 1 AND paidAt < :beforeTimestamp")
    suspend fun clearSmsBodyForOldPaidBills(beforeTimestamp: Long)

    @Query("UPDATE bills SET isPaid = 1, isSuperseded = 0, paidAmount = totalDue, paidAt = :paidAt WHERE id = :billId")
    suspend fun markAsPaid(billId: Long, paidAt: Long = System.currentTimeMillis())

    @Query("UPDATE bills SET paidAmount = :amount, isPaid = (CASE WHEN :amount >= totalDue THEN 1 ELSE 0 END), paidAt = :paidAt WHERE id = :billId")
    suspend fun updatePaidAmount(billId: Long, amount: Double, paidAt: Long = System.currentTimeMillis())

    @Query("UPDATE bills SET isPaid = 0, isSuperseded = 0, paidAmount = 0, paidAt = NULL WHERE id = :billId")
    suspend fun markAsUnpaid(billId: Long)

    @Query("UPDATE bills SET totalDue = :totalDue, minDue = :minDue, dueDate = :dueDate WHERE id = :billId")
    suspend fun updateBillDetails(billId: Long, totalDue: Double, minDue: Double, dueDate: String)

    @Query("SELECT * FROM bills WHERE cardId = :cardId")
    suspend fun getBillsForCardOnce(cardId: Long): List<Bill>

    @Query("SELECT * FROM bills")
    suspend fun getAllBillsOnce(): List<Bill>

    @Query("DELETE FROM bills WHERE id IN (:ids)")
    suspend fun deleteBillsByIds(ids: List<Long>)

    @Query("SELECT * FROM bills WHERE cardId = :cardId AND billPeriod = :period LIMIT 1")
    suspend fun findExistingBill(cardId: Long, period: String): Bill?

    @Query("SELECT * FROM bills WHERE smsBody = :smsBody LIMIT 1")
    suspend fun findBillBySms(smsBody: String): Bill?

    @Query("""
        UPDATE bills SET isSuperseded = 1 WHERE cardId = :cardId AND currency = :currency AND isPaid = 0 AND
        (substr(dueDate,7,4) || substr(dueDate,4,2) || substr(dueDate,1,2)) <
        (substr(:newBillDueDate,7,4) || substr(:newBillDueDate,4,2) || substr(:newBillDueDate,1,2))
    """)
    suspend fun markOlderBillsSuperseded(cardId: Long, currency: String, newBillDueDate: String)

    @Query("""
        UPDATE bills SET isSuperseded = 1
        WHERE isPaid = 0 AND id IN (
            SELECT b.id FROM bills b
            INNER JOIN (
                SELECT cardId, currency, MAX(substr(dueDate,7,4) || substr(dueDate,4,2) || substr(dueDate,1,2)) AS maxDue
                FROM bills WHERE isPaid = 0 GROUP BY cardId, currency
            ) latest ON b.cardId = latest.cardId AND b.currency = latest.currency
            WHERE (substr(b.dueDate,7,4) || substr(b.dueDate,4,2) || substr(b.dueDate,1,2)) < latest.maxDue
        )
    """)
    suspend fun markAllButLatestSuperseded()
}
