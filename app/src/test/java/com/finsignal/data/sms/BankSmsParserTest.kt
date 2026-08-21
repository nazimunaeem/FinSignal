package com.finsignal.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun parsePrimeBankSms() {
        val sms = "Monthly bill Aug-2026 for your Credit Card# *2472 Total due: BDT 1,02,679.23 Minimum due: BDT 5,133.96. Last date of payment 27/08/2026. Client ID: 2630132. Helpline 16218"
        val parsed = BankSmsParser.parse(sms, "PRIMEBANK")

        assertNotNull(parsed)
        assertEquals("Prime Bank", parsed?.bankName)
        assertEquals("2472", parsed?.cardLast4)
        assertEquals(102679.23, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(5133.96, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("27/08/2026", parsed?.dueDate)
        assertEquals("2630132", parsed?.clientId)
    }

    @Test
    fun parsePubaliBankSms() {
        val sms = "Monthly bill Aug-2026 for your Credit Card# *2472 Total due: BDT 1,02,679.23 Minimum due: BDT 5,133.96. Last date of payment 27/08/2026. Client ID: 2630132. Helpline 16218"
        val parsed = BankSmsParser.parse(sms, "PUBALI BANK")

        assertNotNull(parsed)
        assertEquals("Pubali Bank", parsed?.bankName)
        assertEquals("2472", parsed?.cardLast4)
        assertEquals(102679.23, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(5133.96, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("27/08/2026", parsed?.dueDate)
        assertEquals("2630132", parsed?.clientId)
    }

    @Test
    fun parsePubaliBankNewFormatSms() {
        val sms = "PBPLC CREDIT CARD *2402,CustID: 30062088 Bill for AUG-26,Total Due: TK 1,986.00,Min Due: TK 1,986.00,Pay By:01-SEP-26.Helpline 16253"
        val parsed = BankSmsParser.parse(sms, "PBPLC")

        assertNotNull(parsed)
        assertEquals("Pubali Bank", parsed?.bankName)
        assertEquals("2402", parsed?.cardLast4)
        assertEquals("30062088", parsed?.clientId)
        assertEquals("August 2026", parsed?.billPeriod)
        assertEquals(1986.0, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(1986.0, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("01/09/2026", parsed?.dueDate)
    }

    @Test
    fun parsePubaliBankSmsVariations() {
        // Variation: Multiple spaces, non-breaking spaces, newlines
        val sms1 = "Monthly  bill\u00A0Aug-2026\nfor your Credit Card# *2472\r\nTotal due  BDT 1,02,679.23\nMinimum due: BDT 5,133.96. Last date of payment 27/08/2026. Client ID 2630132."
        val parsed1 = BankSmsParser.parse(sms1, "PUBALI BANK")
        assertNotNull("Failed on whitespace/newline variations", parsed1)
        assertEquals("2630132", parsed1?.clientId)

        // Variation: No colon after "Total due", different currency format, dot instead of comma
        val sms2 = "Monthly bill Aug-2026 for your Credit Card# *2472 Total due 102679.23 Minimum due BDT 5133.96 Last date of payment: 27/08/2026"
        val parsed2 = BankSmsParser.parse(sms2, "PUBALI BANK")
        assertNotNull("Failed on punctuation/currency variations", parsed2)
        assertEquals(102679.23, parsed2?.totalDue ?: 0.0, 0.01)

        // Variation: Date with colon, different date separator
        val sms3 = "Monthly bill Aug-2026 for your Credit Card# *2472 Total due: BDT 1,02,679.23 Minimum due: BDT 5,133.96. Last date of payment: 27-08-2026. Client ID: 2630132"
        val parsed3 = BankSmsParser.parse(sms3, "PUBALI BANK")
        assertNotNull("Failed on date variations", parsed3)
        assertEquals("27/08/2026", parsed3?.dueDate)
    }

    @Test
    fun parseCityBankAmexSms() {
        val sms = "AMEX Bill Jun'25\nTotal Due\nTk 15158.49\nMin Due\nTk 2926.67\nCARD: 376***945\nClient ID: 2233552\nPay by 08-07-25\neStatement:t.ly/NbH5"
        val parsed = BankSmsParser.parse(sms, "01944-400032")

        assertNotNull(parsed)
        assertEquals("City Bank (Amex)", parsed?.bankName)
        assertEquals("945", parsed?.cardLast4)
        assertEquals(15158.49, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(2926.67, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("08/07/2025", parsed?.dueDate)
        assertEquals("2233552", parsed?.clientId)
    }

    @Test
    fun parseBracBankSms() {
        val sms = "Credit card#**8043 bill for Jul 26 - Total due: BDT 4401; Min due: BDT 500. Last day for payment: 06-08-26. Statement: https://s.bracbank.com/3HkgOC"
        val parsed = BankSmsParser.parse(sms, "BRAC-BANK")

        assertNotNull(parsed)
        assertEquals("BRAC Bank", parsed?.bankName)
        assertEquals("8043", parsed?.cardLast4)
        assertEquals(4401.0, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(500.0, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("06/08/2026", parsed?.dueDate)
    }

    @Test
    fun parseEblSms() {
        val sms = "Monthly bill 423800****5428 APR2026; Total Due: BDT 8754.72, Min Due: BDT 500.00, Last Pmt: 20-APR-26. Statement link https://onelink.to/eblskybanking"
        val parsed = BankSmsParser.parse(sms, "01944-400032")

        assertNotNull(parsed)
        assertEquals("EBL", parsed?.bankName)
        assertEquals("5428", parsed?.cardLast4)
        assertEquals(8754.72, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(500.0, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("20/04/2026", parsed?.dueDate)
        assertEquals("BDT", parsed?.currency)
        assertEquals("April 2026", parsed?.billPeriod)
    }

    @Test
    fun parseEblRealBdtSms() {
        val sms = "Monthly bill 423800******5428 DEC2025; Total Due: BDT 27991.57, Min Due: BDT 839.75, Last Pmt: 20-DEC-25. Statement link https://onelink.to/eblskybanking"
        val parsed = BankSmsParser.parse(sms, "01944-400032")

        assertNotNull(parsed)
        assertEquals("EBL", parsed?.bankName)
        assertEquals("5428", parsed?.cardLast4)
        assertEquals(27991.57, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(839.75, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("20/12/2025", parsed?.dueDate)
        assertEquals("BDT", parsed?.currency)
        assertEquals("December 2025", parsed?.billPeriod)
    }

    @Test
    fun parseEblRealUsdSms() {
        val sms = "Monthly bill 423800******5428 NOV2025; Total Due: USD 3.19, Min Due: USD 3.19, Last Pmt: 20-NOV-25. Statement link https://onelink.to/eblskybanking"
        val parsed = BankSmsParser.parse(sms, "01944-400032")

        assertNotNull(parsed)
        assertEquals("EBL", parsed?.bankName)
        assertEquals(3.19, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals(3.19, parsed?.minDue ?: 0.0, 0.01)
        assertEquals("20/11/2025", parsed?.dueDate)
        assertEquals("USD", parsed?.currency)
        assertEquals("November 2025", parsed?.billPeriod)
    }

    @Test
    fun parseEblSmsVariations() {
        // Newline inside "Monthly bill", hyphenated period, "Last Payment Date"
        val sms1 = "Monthly\nBill 423800****5428 Apr-26; Total Due : BDT 12,340.50; Min Due : BDT 620.25; Last Payment Date: 05-MAY-26"
        val parsed1 = BankSmsParser.parse(sms1, "01944400032")
        assertNotNull("Failed on newline/hyphenated-period variation", parsed1)
        assertEquals("EBL", parsed1?.bankName)
        assertEquals(12340.50, parsed1?.totalDue ?: 0.0, 0.01)
        assertEquals("05/05/2026", parsed1?.dueDate)

        // Semicolon-separated amounts
        val sms2 = "Monthly bill 423800****1111 JUL2026; Total Due: BDT 900; Min Due: BDT 50; Last Pmt: 01-AUG-26."
        val parsed2 = BankSmsParser.parse(sms2, "EBL")
        assertNotNull(parsed2)
        assertEquals("July 2026", parsed2?.billPeriod)
    }

    @Test
    fun billPeriodNormalizationIsConsistent() {
        // All variants of the same month must canonicalize identically so no duplicates are created
        val expected = "August 2026"
        assertEquals(expected, BankSmsParser.normalizeBillPeriod("Aug-2026"))
        assertEquals(expected, BankSmsParser.normalizeBillPeriod("AUG-26"))
        assertEquals(expected, BankSmsParser.normalizeBillPeriod("Aug 26"))
        assertEquals(expected, BankSmsParser.normalizeBillPeriod("AUGUST 2026"))

        assertEquals("June 2025", BankSmsParser.normalizeBillPeriod("Jun'25"))
        assertEquals("April 2026", BankSmsParser.normalizeBillPeriod("APR2026"))
        assertEquals("July 2026", BankSmsParser.normalizeBillPeriod("Jul 26"))
    }

    @Test
    fun parseEblUsdSms() {
        val sms = "Monthly bill 423800****5428 APR2026; Total Due: USD 3.19, Min Due: USD 1.00, Last Pmt: 20-APR-26."
        val parsed = BankSmsParser.parse(sms, "01944-400032")

        assertNotNull(parsed)
        assertEquals("EBL", parsed?.bankName)
        assertEquals(3.19, parsed?.totalDue ?: 0.0, 0.01)
        assertEquals("USD", parsed?.currency)
    }

    @Test
    fun identifyBankFromStatementLinkWhenSenderUnknown() {
        val eblSms = "Monthly bill 423800******5428 DEC2025; Total Due: BDT 27991.57, Min Due: BDT 839.75, Last Pmt: 20-DEC-25. Statement link https://onelink.to/eblskybanking"
        val ebl = BankSmsParser.parse(eblSms, "+8801000000000")
        assertNotNull(ebl)
        assertEquals("EBL", ebl?.bankName)

        val bracSms = "Credit card#**8043 bill for Jul 26 - Total due: BDT 4401; Min due: BDT 500. Last day for payment: 06-08-26. Statement: https://s.bracbank.com/3HkgOC"
        val brac = BankSmsParser.parse(bracSms, "+8801000000001")
        assertNotNull(brac)
        assertEquals("BRAC Bank", brac?.bankName)
    }

    @Test
    fun parseUnsupportedBankSms() {
        val sms = "Dear Customer, Total Outstanding for your Credit Card ending with 9876 is BDT 45,250.00. Min Due BDT 2,500.00. Pay before 15-09-2026."
        val parsed = BankSmsParser.parse(sms, "SCB")
        assertNull(parsed)
    }
}
