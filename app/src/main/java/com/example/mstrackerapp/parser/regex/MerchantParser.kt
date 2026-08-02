package com.example.mstrackerapp.parser.regex

object MerchantParser {

    private val UPI_REGEX = Regex(
        """(?:UPI[-/]|paid to |VPA\s+)([\w\.\-\s]+?)(?:@|\s+Ref|\s+on|\s+dt|\s+balance|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    private val CARD_POS_REGEX = Regex(
        """(?:POS txn at|POS transaction at|spent at|\bat)\s+([A-Z0-9\.\-\s&']+?)(?:\s+on|\s+ref|\s+card|\s+avail|\s+credited|\s+debited|\.|\d{2}/|$)""",
        RegexOption.IGNORE_CASE
    )

    private val ATM_REGEX = Regex(
        """(?:ATM\s+WDL|ATM\s+withdrawal\s+at|ATM)\s+([A-Z0-9\.\-\s]+?)(?:\s+on|\s+ref|\s+avail|\s+credited|\s+debited|\.|\d{2}/|$)""",
        RegexOption.IGNORE_CASE
    )

    private val NEFT_REGEX = Regex(
        """(?:NEFT\s*Cr-|NEFT-|NEFT/)\s*([\w\.\-\s]+?)(?:\s+ref|\s+on|\s+credited|\s+debited|\.|\d{2}/|$)""",
        RegexOption.IGNORE_CASE
    )

    private val IMPS_REGEX = Regex(
        """(?:IMPS/P2A/\d+/|IMPS/|IMPS-)\s*([\w\.\-\s]+?)(?:\s+ref|\s+on|\s+credited|\s+debited|\.|\d{2}/|$)""",
        RegexOption.IGNORE_CASE
    )

    fun parseMerchant(smsText: String): String {
        if (smsText.isBlank()) return "Unknown Merchant"

        // 1. Try UPI Pattern
        val upiMatch = UPI_REGEX.find(smsText)
        if (upiMatch != null) {
            val merchant = cleanMerchantName(upiMatch.groupValues[1])
            if (merchant.isNotEmpty()) return merchant
        }

        // 2. Try Card / POS Pattern
        val cardMatch = CARD_POS_REGEX.find(smsText)
        if (cardMatch != null) {
            val merchant = cleanMerchantName(cardMatch.groupValues[1])
            if (merchant.isNotEmpty()) return merchant
        }

        // 3. Try ATM Pattern
        val atmMatch = ATM_REGEX.find(smsText)
        if (atmMatch != null) {
            val merchant = cleanMerchantName(atmMatch.groupValues[1])
            if (merchant.isNotEmpty()) return "ATM - $merchant"
        }

        // 4. Try NEFT Pattern
        val neftMatch = NEFT_REGEX.find(smsText)
        if (neftMatch != null) {
            val merchant = cleanMerchantName(neftMatch.groupValues[1])
            if (merchant.isNotEmpty()) return merchant
        }

        // 5. Try IMPS Pattern
        val impsMatch = IMPS_REGEX.find(smsText)
        if (impsMatch != null) {
            val merchant = cleanMerchantName(impsMatch.groupValues[1])
            if (merchant.isNotEmpty()) return merchant
        }

        // Fallback common merchant detection
        return when {
            smsText.contains("Starbucks", ignoreCase = true) -> "Starbucks"
            smsText.contains("Swiggy", ignoreCase = true) -> "Swiggy"
            smsText.contains("Zomato", ignoreCase = true) -> "Zomato"
            smsText.contains("Uber", ignoreCase = true) -> "Uber"
            smsText.contains("Amazon", ignoreCase = true) -> "Amazon"
            smsText.contains("Flipkart", ignoreCase = true) -> "Flipkart"
            else -> "Bank Transaction"
        }
    }

    private fun cleanMerchantName(rawName: String): String {
        val trimmedPrefix = rawName.replace(Regex("""(?i)^pos\s+txn\s+at\s+"""), "")
        return trimmedPrefix
            .replace(Regex("""\d{4,}"""), "") // Remove long reference numbers
            .replace(Regex("""\s+"""), " ")
            .trim()
            .split(" ")
            .take(4) // Keep max 4 words
            .joinToString(" ")
            .uppercase()
    }
}
