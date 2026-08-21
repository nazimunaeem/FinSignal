package com.finsignal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_records")
data class SmsRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String?,
    val body: String,
    val timestamp: Long,
    val isParsed: Boolean = false,
    val detectedAt: Long = System.currentTimeMillis()
)
