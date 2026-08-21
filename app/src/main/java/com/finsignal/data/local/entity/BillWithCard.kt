package com.finsignal.data.local.entity

data class BillWithCard(
    val billId: Long,
    val cardId: Long,
    val bankName: String,
    val cardLast4: String,
    val cardNickname: String,
    val billPeriod: String,
    val totalDue: Double,
    val minDue: Double,
    val dueDate: String,
    val isPaid: Boolean,
    val paidAmount: Double,
    val paidAt: Long?,
    val smsBody: String?,
    val currency: String,
    val detectedAt: Long
) {
    val displayName: String
        get() = cardNickname.ifBlank { "$bankName •••• $cardLast4" }

    val formattedTotal: String
        get() = "${getCurrencySymbol(currency)}${String.format(java.util.Locale.US, "%,.2f", totalDue)}"

    val formattedRemaining: String
        get() = "${getCurrencySymbol(currency)}${String.format(java.util.Locale.US, "%,.2f", totalDue - paidAmount)}"

    private fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            "USD" -> "$"
            "BDT", "TK" -> "৳"
            else -> currency
        }
    }

    private val _computedDueDate by lazy { computeDueDate() }
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
        get() = _computedDueDate

    private fun computeDueDate(): String {
        return try {
            val normalized = com.finsignal.data.sms.BankSmsParser.normalizeDate(dueDate)
            val input = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            val output = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
            val date = input.parse(normalized)
            date?.let { output.format(it) } ?: dueDate
        } catch (e: Exception) {
            dueDate
        }
    }

    private fun computeDaysUntilDue(): Long {
        return try {
            val normalized = com.finsignal.data.sms.BankSmsParser.normalizeDate(dueDate)
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            val due = sdf.parse(normalized) ?: return Long.MAX_VALUE
            val todayStr = sdf.format(System.currentTimeMillis())
            val now = sdf.parse(todayStr) ?: return Long.MAX_VALUE
            val diff = due.time - now.time
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
}
