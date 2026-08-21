package com.finsignal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.finsignal.data.local.entity.ActivityLog

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insert(log: ActivityLog): Long

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ActivityLog>

    @Query("DELETE FROM activity_log")
    suspend fun clearAll()
}
