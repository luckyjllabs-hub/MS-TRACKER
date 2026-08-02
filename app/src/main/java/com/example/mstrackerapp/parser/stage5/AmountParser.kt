package com.example.mstrackerapp.parser.stage5

object AmountParser {

    // Verb + amount, including HDFC "Received!\nINR 12,181.00"
    private val VERB_AMOUNT = Regex(
        """(?i)\b(?:debited|credited|spent|sent|paid|transferred|withdrawn|received|deposited|deducted)[!.,]?\s+(?:for|by|of|with|is|on|to|in|at|rs\.?|inr|₹)*\s*(?:rs\.?|inr|₹)?\s*([\d,]+\.?\d*)"""
    )
    // Currency prefix: "INR 1,200" or "Rs. 1,200" or "₹1200" or "Rs 500.00"
    private val CURRENCY_PREFIX = Regex(
        """(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE
    )
    // Currency suffix: "1,200 INR"
    private val CURRENCY_SUFFIX = Regex(
        """([\d,]+\.?\d*)\s*(?:INR|Rs\.?)""", RegexOption.IGNORE_CASE
    )
    // Decimal fallback: "1,234.56" pattern
    private val DECIMAL_FALLBACK = Regex("""([\d,]+\.\d{2})""")

    /** Returns amount in paisa (minor units, 1 INR = 100 paisa) */
    fun parseAmountMinor(text: String): Long? {
        val rupees = parseRupees(text) ?: return null
        if (rupees <= 0) return null
        return (rupees * 100).toLong()
    }

    fun parseRupees(text: String): Double? {
        VERB_AMOUNT.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        CURRENCY_PREFIX.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        CURRENCY_SUFFIX.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        DECIMAL_FALLBACK.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        return null
    }
}
