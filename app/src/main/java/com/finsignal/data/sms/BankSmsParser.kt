package com.finsignal.data.sms

import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedBill(
    val bankName: String,
    val cardLast4: String,
    val billPeriod: String,
    val totalDue: Double,
    val minDue: Double,
    val dueDate: String,
    val clientId: String? = null,
    val currency: String = "BDT"
)

object BankSmsParser {

    private val primeBankPattern = Regex(
        """Monthly[\s\u00A0]*bill[\s\u00A0]*(\w+[- ]\d{4}|\w+)[\s\u00A0]*for[\s\u00A0]*your[\s\u00A0]*Credit[\s\u00A0]*Card#?[\s\u00A0]*\*?(\d{4})[\s\u00A0]*""" +
        """(?:Total[\s\u00A0]*due|Bill[\s\u00A0]*Outstanding|Total[\s\u00A0]*Outstanding|Outstanding)[\s\u00A0]*:?[\s\u00A0]*(BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)[\s\u00A0]*Minimum[\s\u00A0]*due[\s\u00A0]*:?[\s\u00A0]*(?:BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)\.?[\s\u00A0]*""" +
        """Last[\s\u00A0]*date[\s\u00A0]*of[\s\u00A0]*payment[\s\u00A0]*:?[\s\u00A0]*(\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{1,2}[-/.]\w{3}[-/.]\d{2,4})(?:\.?[\s\u00A0]*Client[\s\u00A0]*ID[\s\u00A0]*:?[\s\u00A0]*(\d+))?""",
        RegexOption.IGNORE_CASE
    )

    private val pubaliBankPattern = Regex(
        """PBPLC[\s\u00A0]*CREDIT[\s\u00A0]*CARD[\s\u00A0]*\*(\d{4}),CustID:[\s\u00A0]*(\d+)[\s\u00A0]*Bill[\s\u00A0]*for[\s\u00A0]*(\w{3}-\d{2,4}),Total[\s\u00A0]*Due:[\s\u00A0]*(BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*),Min[\s\u00A0]*Due:[\s\u00A0]*(?:BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*),Pay[\s\u00A0]*By:(\d{1,2}[-/.]\w{3}[-/.]\d{2,4}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4})""",
        RegexOption.IGNORE_CASE
    )

    private val cityBankAmexPattern = Regex(
        """AMEX[\s\u00A0]*Bill[\s\u00A0]*(\w+'?\d{2,4}|\w+)[\s\u00A0]*(?:Total[\s\u00A0]*Due|Total[\s\u00A0]*Outstanding|Bill[\s\u00A0]*Outstanding|Outstanding)[\s\u00A0]*:?[\s\u00A0]*(?:\n|[\s\u00A0])*(BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)[\s\u00A0]+""" +
        """Min[\s\u00A0]*Due[\s\u00A0]*:?[\s\u00A0]*(?:\n|[\s\u00A0])*(?:BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)[\s\u00A0]*CARD[\s\u00A0]*:?[\s\u00A0]*(?:\d{3}\*{3}|\*{2,4})(\d{3,4})[\s\u00A0]*""" +
        """(?:Client[\s\u00A0]*ID[\s\u00A0]*:?[\s\u00A0]*(\d+)[\s\u00A0]*)?Pay[\s\u00A0]*by[\s\u00A0]*(\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{1,2}[-/.]\w{3}[-/.]\d{2,4})""",
        RegexOption.IGNORE_CASE
    )

    private val bracBankPattern = Regex(
        """Credit[\s\u00A0]*card#?[\s\u00A0]*\*+(\d{4})[\s\u00A0]*bill[\s\u00A0]*for[\s\u00A0]*(\w+[\s\u00A0]*\d{2,4}|\w+)[\s\u00A0]*-[\s\u00A0]*""" +
        """(?:Total[\s\u00A0]*due|Bill[\s\u00A0]*outstanding|Total[\s\u00A0]*outstanding|Outstanding)[\s\u00A0]*:?[\s\u00A0]*(BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*);?[\s\u00A0]*Min[\s\u00A0]*due[\s\u00A0]*:?[\s\u00A0]*(?:BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)\.?[\s\u00A0]*""" +
        """Last[\s\u00A0]*day[\s\u00A0]*for[\s\u00A0]*payment[\s\u00A0]*:?[\s\u00A0]*(\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{1,2}[-/.]\w{3}[-/.]\d{2,4})""",
        RegexOption.IGNORE_CASE
    )

    private val eblPattern = Regex(
        """Monthly[\s\u00A0]*bill[\s\u00A0]*([\d*]+)[\s\u00A0]*(\w{2,4}[-/]?\d{2,4}|\w+);?[\s\u00A0]*""" +
        """(?:Total[\s\u00A0]*Due|Total[\s\u00A0]*Outstanding|Bill[\s\u00A0]*Outstanding|Outstanding)[\s\u00A0]*:?[\s\u00A0]*(BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)[;,]?[\s\u00A0]*Min[\s\u00A0]*Due[\s\u00A0]*:?[\s\u00A0]*(?:BDT|Tk|TK|৳|\$|USD)?[\s\u00A0]*([\d,]+\.?\d*)[;,]?[\s\u00A0]*""" +
        """Last[\s\u00A0]*(?:Pmt|Payment)[\s\u00A0]*(?:Date|Dt)?[\s\u00A0]*:?[\s\u00A0]*(\d{1,2}[-/.]\w{3}[-/.]\d{2,4}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4})""",
        RegexOption.IGNORE_CASE
    )

    private val senderPatterns = mapOf(
        "PRIMEBANK" to BankType.PRIME_BANK,
        "PRIME BANK" to BankType.PRIME_BANK,
        "PUBALIBANK" to BankType.PUBALI_BANK,
        "PUBALI BANK" to BankType.PUBALI_BANK,
        "PBL" to BankType.PUBALI_BANK,
        "BRACBANK" to BankType.BRAC_BANK,
        "BRAC-BANK" to BankType.BRAC_BANK,
        "CITYBANK" to BankType.CITY_BANK,
        "CITY BANK" to BankType.CITY_BANK,
        "EBL" to BankType.EBL,
        "EBLSKY" to BankType.EBL,
        "ONELINK" to BankType.EBL,
        "SKYBANKING" to BankType.EBL,
        "EASTERNBANK" to BankType.EBL,
        "01944-400032" to BankType.UNKNOWN,
        "01944400032" to BankType.UNKNOWN
    )

    enum class BankType {
        PRIME_BANK, PUBALI_BANK, CITY_BANK, BRAC_BANK, EBL, UNKNOWN
    }

    // Statement/tracking links embedded in bill SMS reveal the bank even when the
    // sender is a plain number and the body never mentions the bank name.
    private val linkBankKeywords = listOf(
        "eblskybanking" to BankType.EBL,
        "easternbank" to BankType.EBL,
        "onelink.to" to BankType.EBL,
        "bracbank" to BankType.BRAC_BANK,
        "pubali" to BankType.PUBALI_BANK,
        "pbplc" to BankType.PUBALI_BANK,
        "primebank" to BankType.PRIME_BANK,
        "citybank" to BankType.CITY_BANK,
        "amex" to BankType.CITY_BANK
    )

    private fun identifyBankFromLink(body: String?): BankType? {
        if (body.isNullOrBlank()) return null
        val links = Regex("""(?:https?://|www\.)\S+""")
            .findAll(body.replace('\u00A0', ' '))
            .map { it.value.lowercase() }
            .toList()
        if (links.isEmpty()) return null
        for ((keyword, type) in linkBankKeywords) {
            if (links.any { it.contains(keyword) }) return type
        }
        return null
    }

    fun identifyBank(sender: String?, body: String? = null): BankType {
        if (sender == null && body == null) return BankType.UNKNOWN
        val normalized = sender?.trim()?.uppercase()?.replace(Regex("""[\s\u00A0-]+"""), "") ?: ""

        // Check sender patterns with simplified normalized sender
        for ((key, type) in senderPatterns) {
            val normalizedKey = key.uppercase().replace(Regex("""[\s\u00A0-]+"""), "")
            if (normalized == normalizedKey && type != BankType.UNKNOWN) return type
        }

        // Links are the strongest signal for numeric/unknown senders
        identifyBankFromLink(body)?.let { return it }

        // Check body for keywords if sender is numeric or unknown
        val b = (body?.uppercase()?.replace('\u00A0', ' ') ?: "").replace(Regex("\\s+"), " ")
        when {
            // Pubali/Prime textual check
            normalized.contains("PRIME") || b.contains("PRIMEBANK") || b.contains("PRIME BANK") -> return BankType.PRIME_BANK
            normalized.contains("PUBALI") || normalized.contains("PBL") || normalized.contains("PBLC") || normalized.contains("PBPLC") || b.contains("PUBALI BANK") || b.contains("PUBALIBANK") || b.contains("HELPLINE 16218") || b.contains("PBLC") || b.contains("PBPLC") -> return BankType.PUBALI_BANK
            
            // BRAC
            normalized.contains("BRAC") || b.contains("BRACBANK") || b.contains("BRAC-BANK") || b.contains("BRAC BANK") || b.contains("S.BRACBANK") -> return BankType.BRAC_BANK
            
            // City Bank AMEX
            normalized.contains("CITY") || b.contains("AMEX") || b.contains("CITY BANK") || b.contains("CITYBANK") || b.contains("CITY-BANK") -> return BankType.CITY_BANK
            
            // EBL
            normalized.contains("EBL") || b.contains("EBLSKY") || b.contains("ONELINK") || b.contains("EASTERN BANK") || b.contains("EBL-BANK") ||
                (normalized.contains("01944") && b.contains("MONTHLY BILL") && !b.contains("FOR YOUR CREDIT CARD")) -> return BankType.EBL
        }

        return BankType.UNKNOWN
    }

    fun parse(smsBody: String, sender: String? = null): ParsedBill? {
        val bankType = identifyBank(sender, smsBody)

        return when (bankType) {
            BankType.PRIME_BANK -> parsePrimeBank(smsBody, "Prime Bank")
            BankType.PUBALI_BANK -> parsePubaliBank(smsBody)
            BankType.CITY_BANK -> parseCityBank(smsBody)
            BankType.BRAC_BANK -> parseBracBank(smsBody)
            BankType.EBL -> parseEbl(smsBody)
            BankType.UNKNOWN -> null
        }
    }

    private fun parsePubaliBank(smsBody: String): ParsedBill? {
        val match = pubaliBankPattern.find(smsBody)
        if (match != null) {
            return ParsedBill(
                bankName = "Pubali Bank",
                cardLast4 = match.groupValues[1],
                clientId = match.groupValues[2],
                billPeriod = normalizeBillPeriod(match.groupValues[3]),
                currency = normalizeCurrency(match.groupValues[4]),
                totalDue = parseAmount(match.groupValues[5]),
                minDue = parseAmount(match.groupValues[6]),
                dueDate = normalizeDate(match.groupValues[7])
            )
        }
        return parsePrimeBank(smsBody, "Pubali Bank")
    }

    private fun parsePrimeBank(smsBody: String, bankName: String): ParsedBill? {
        val match = primeBankPattern.find(smsBody) ?: return null
        return ParsedBill(
            bankName = bankName,
            billPeriod = normalizeBillPeriod(match.groupValues[1]),
            cardLast4 = match.groupValues[2],
            currency = normalizeCurrency(match.groupValues[3]),
            totalDue = parseAmount(match.groupValues[4]),
            minDue = parseAmount(match.groupValues[5]),
            dueDate = normalizeDate(match.groupValues[6]),
            clientId = match.groupValues.getOrNull(7)?.ifBlank { null }
        )
    }

    private fun parseCityBank(smsBody: String): ParsedBill? {
        val match = cityBankAmexPattern.find(smsBody) ?: return null
        return ParsedBill(
            bankName = "City Bank (Amex)",
            billPeriod = normalizeBillPeriod(match.groupValues[1]),
            currency = normalizeCurrency(match.groupValues[2]),
            totalDue = parseAmount(match.groupValues[3]),
            minDue = parseAmount(match.groupValues[4]),
            cardLast4 = match.groupValues[5],
            clientId = match.groupValues.getOrNull(6)?.ifBlank { null },
            dueDate = normalizeDate(match.groupValues[7])
        )
    }

    private fun parseBracBank(smsBody: String): ParsedBill? {
        val match = bracBankPattern.find(smsBody) ?: return null
        return ParsedBill(
            bankName = "BRAC Bank",
            cardLast4 = match.groupValues[1],
            billPeriod = normalizeBillPeriod(match.groupValues[2]),
            currency = normalizeCurrency(match.groupValues[3]),
            totalDue = parseAmount(match.groupValues[4]),
            minDue = parseAmount(match.groupValues[5]),
            dueDate = normalizeDate(match.groupValues[6])
        )
    }

    private fun parseEbl(smsBody: String): ParsedBill? {
        val match = eblPattern.find(smsBody) ?: return null
        val cardFull = match.groupValues[1]
        return ParsedBill(
            bankName = "EBL",
            billPeriod = normalizeBillPeriod(match.groupValues[2]),
            cardLast4 = cardFull.takeLast(4),
            currency = normalizeCurrency(match.groupValues[3]),
            totalDue = parseAmount(match.groupValues[4]),
            minDue = parseAmount(match.groupValues[5]),
            dueDate = normalizeDate(match.groupValues[6])
        )
    }

    private fun parseAmount(amount: String): Double {
        return amount.replace(",", "").replace("৳", "").trim().toDoubleOrNull() ?: 0.0
    }

    private fun normalizeCurrency(currency: String?): String {
        val c = currency?.uppercase() ?: "BDT"
        return when (c) {
            "TK", "BDT", "৳" -> "BDT"
            "USD", "$" -> "USD"
            else -> c
        }
    }

    fun normalizeDate(dateStr: String): String {
        val dateClean = dateStr.trim()
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "dd/MM/yy", "dd-MM-yy", "dd.MM.yy",
            "dd-MMM-yy", "dd-MMM-yyyy", "dd MMM yyyy", "dd MMM yy",
            "yyyy-MM-dd"
        )

        for (fmt in formats) {
            try {
                val input = SimpleDateFormat(fmt, Locale.US)
                input.isLenient = false
                val parsed = input.parse(dateClean)
                if (parsed != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsed
                    var year = cal.get(java.util.Calendar.YEAR)
                    if (year < 100) {
                        year += 2000
                        cal.set(java.util.Calendar.YEAR, year)
                    }
                    val output = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    return output.format(cal.time)
                }
            } catch (e: Exception) {}
        }
        return dateClean
    }

    private val monthNumbers = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    private val monthNames = mapOf(
        1 to "January", 2 to "February", 3 to "March", 4 to "April", 5 to "May", 6 to "June",
        7 to "July", 8 to "August", 9 to "September", 10 to "October", 11 to "November", 12 to "December"
    )

    /**
     * Canonicalizes any bill period variant ("Aug-2026", "AUG-26", "Aug 26", "APR2026",
     * "Jun'25") into a single stable form ("August 2026") so duplicate detection works
     * even when a bank changes its SMS format.
     */
    fun normalizeBillPeriod(period: String): String {
        val cleaned = period.trim().replace(Regex("['\u2019]"), "")
        val match = Regex("""^([A-Za-z]{3,})[\s\-/]??(\d{2,4})?$""").find(cleaned) ?: return period
        val monthNum = monthNumbers[match.groupValues[1].lowercase().take(3)] ?: return period
        val yearStr = match.groupValues[2]
        val year = when {
            yearStr.isEmpty() -> java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            yearStr.length <= 2 -> 2000 + yearStr.toInt()
            else -> yearStr.toInt()
        }
        return "${monthNames[monthNum]} $year"
    }

    fun formatBillPeriod(period: String): String {
        return try {
            val input = SimpleDateFormat("MMM yyyy", Locale.US)
            val parsed = input.parse(period)
            val output = SimpleDateFormat("MMMM yyyy", Locale.US)
            parsed?.let { output.format(it) } ?: period
        } catch (e: Exception) {
            period
        }
    }
}
