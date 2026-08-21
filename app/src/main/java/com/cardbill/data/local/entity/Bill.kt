package com.cardbill.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId"), Index("dueDate"), Index("isPaid")]
)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val billPeriod: String,
    val totalDue: Double,
    val minDue: Double,
    val dueDate: String,
    val isPaid: Boolean = false,
    val isSuperseded: Boolean = false,
    val paidAmount: Double = 0.0,
    val paidAt: Long? = null,
    val smsBody: String? = null,
    val currency: String = "BDT",
    val detectedAt: Long = System.currentTimeMillis()
) {
    val formattedTotal: String
        get() = "${getCurrencySymbol(currency)}${String.format(Locale.US, "%,.2f", totalDue)}"

    val formattedMin: String
        get() = "${getCurrencySymbol(currency)}${String.format(Locale.US, "%,.2f", minDue)}"

    val formattedRemaining: String
        get() = "${getCurrencySymbol(currency)}${String.format(Locale.US, "%,.2f", totalDue - paidAmount)}"

    private fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            "USD" -> "$"
            "BDT", "TK" -> "৳"
            else -> currency
        }
    }

    private val _computedDaysUntilDue by lazy { computeDaysUntilDue() }

    val daysUntilDue: Long
        get() = _computedDaysUntilDue

    val dueStatus: DueStatus
        get() = when {
            isPaid -> DueStatus.PAID
            daysUntilDue < 0 -> DueStatus.OVERDUE
            daysUntilDue <= 3 -> DueStatus.DUE_SOON
            daysUntilDue <= 7 -> DueStatus.UPCOMING
            else -> DueStatus.SAFE
        }

    val formattedDueDate: String
        get() = try {
            val normalized = com.cardbill.data.sms.BankSmsParser.normalizeDate(dueDate)
            val input = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val output = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val date = input.parse(normalized)
            date?.let { output.format(it) } ?: dueDate
        } catch (e: Exception) {
            dueDate
        }

    private fun computeDaysUntilDue(): Long {
        return try {
            val normalized = com.cardbill.data.sms.BankSmsParser.normalizeDate(dueDate)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val due = sdf.parse(normalized) ?: return Long.MAX_VALUE
            val now = sdf.parse(
                sdf.format(System.currentTimeMillis())
            ) ?: return Long.MAX_VALUE
            val diff = due.time - now.time
            TimeUnit.MILLISECONDS.toDays(diff)
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
}

enum class DueStatus {
    PAID,
    OVERDUE,
    DUE_SOON,
    UPCOMING,
    SAFE
}
