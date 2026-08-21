package com.cardbill.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_cards",
    indices = [Index(value = ["bankName", "cardLast4"], unique = true)]
)
data class CreditCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val cardLast4: String,
    val cardNickname: String = "",
    val clientId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = cardNickname.ifBlank { "$bankName •••• $cardLast4" }

    val maskedCard: String
        get() = "•••• •••• •••• $cardLast4"
}
