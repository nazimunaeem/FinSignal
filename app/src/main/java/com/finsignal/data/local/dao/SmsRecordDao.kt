package com.finsignal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finsignal.data.local.entity.SmsRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: SmsRecord): Long

    @Query("SELECT * FROM sms_records ORDER BY timestamp DESC")
    fun getAllSms(): Flow<List<SmsRecord>>

    @Query("SELECT * FROM sms_records ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getSmsPage(limit: Int, offset: Int): Flow<List<SmsRecord>>

    @Query("SELECT COUNT(*) FROM sms_records")
    fun getSmsCount(): Flow<Int>

    @Query("SELECT * FROM sms_records WHERE body = :body LIMIT 1")
    suspend fun findByBody(body: String): SmsRecord?

    @Query("UPDATE sms_records SET isParsed = :isParsed WHERE id = :id")
    suspend fun updateParsedStatus(id: Long, isParsed: Boolean)

    @Query("DELETE FROM sms_records WHERE timestamp < :beforeTimestamp AND isParsed = 1")
    suspend fun deleteOldParsedSms(beforeTimestamp: Long)

    @Query("""
        UPDATE sms_records SET isParsed = 1 WHERE isParsed = 0 AND body IN (
            SELECT body FROM sms_records GROUP BY body HAVING MAX(isParsed) = 1
        )
    """)
    suspend fun mergeParsedStatusOfDuplicates()

    @Query("DELETE FROM sms_records WHERE id NOT IN (SELECT MIN(id) FROM sms_records GROUP BY body)")
    suspend fun deleteDuplicateSmsRecords()

    @Query("DELETE FROM sms_records")
    suspend fun clearAll()
}
