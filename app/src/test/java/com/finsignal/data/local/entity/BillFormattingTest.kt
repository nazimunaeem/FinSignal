package com.finsignal.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class BillFormattingTest {

    @Test
    fun testBillFormattingWithSpecialCurrencySymbols() {
        val bill1 = Bill(
            id = 1,
            cardId = 1,
            billPeriod = "AUG-2026",
            totalDue = 1500.5,
            minDue = 100.0,
            dueDate = "25/08/2026",
            currency = "%"
        )

        val billWithCard1 = BillWithCard(
            billId = 1,
            cardId = 1,
            bankName = "Test Bank",
            cardLast4 = "1234",
            cardNickname = "",
            billPeriod = "AUG-2026",
            totalDue = 1500.5,
            minDue = 100.0,
            dueDate = "25/08/2026",
            isPaid = false,
            paidAmount = 50.0,
            paidAt = null,
            smsBody = null,
            currency = "%",
            detectedAt = System.currentTimeMillis()
        )

        assertEquals("%1,500.50", bill1.formattedTotal)
        assertEquals("%100.00", bill1.formattedMin)
        assertEquals("%1,500.50", bill1.formattedRemaining)

        assertEquals("%1,500.50", billWithCard1.formattedTotal)
        assertEquals("%1,450.50", billWithCard1.formattedRemaining)
    }

    @Test
    fun testBillFormattingWithBdtAndUsd() {
        val billBdt = Bill(
            id = 1,
            cardId = 1,
            billPeriod = "AUG-2026",
            totalDue = 5000.0,
            minDue = 250.0,
            dueDate = "25/08/2026",
            currency = "BDT"
        )

        val billUsd = Bill(
            id = 2,
            cardId = 1,
            billPeriod = "AUG-2026",
            totalDue = 100.0,
            minDue = 10.0,
            dueDate = "25/08/2026",
            currency = "USD"
        )

        assertEquals("৳5,000.00", billBdt.formattedTotal)
        assertEquals("$100.00", billUsd.formattedTotal)
    }
}
