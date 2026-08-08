package com.jllabs.moneylens.parser.stage5

object AmountParser {

    // Verb + currency amount, including HDFC "Received!\nINR 12,181.00" and ICICI "credited:Rs.456666.00"
    // Also SBI "has a credit by Cheque of Rs 19,20,000.00"
    private val VERB_CURRENCY_AMOUNT = Regex(
        """(?i)\b(?:debited|credited|spent|sent|paid|transferred|withdrawn|received|deposited|deducted|(?:has\s+a\s+)?credit\s+by(?:\s+\w+)?)[!.,:]?\s*(?:(?:for|by|of|with|is|to|in|at)\s+)?(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*)"""
    )
    // SBI-style without currency token: "debited by 40.00" / "credited by 6000"
    private val VERB_BARE_AMOUNT = Regex(
        """(?i)\b(?:debited|credited|spent|sent|paid|withdrawn|deducted)\s+(?:by|with|for|of)\s+([\d,]+\.?\d*)"""
    )
    // EPFO: "Contribution of Rs. 62,462/- for due month Jun-26 has been received."
    private val EPFO_CONTRIBUTION = Regex(
        """(?i)contribution\s+of\s+(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*)"""
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

    /** EPFO contribution credited (not passbook balance). */
    fun parseEpfoContributionMinor(text: String): Long? {
        val rupees = EPFO_CONTRIBUTION.find(text)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null
        if (rupees <= 0) return null
        return (rupees * 100).toLong()
    }

    fun parseRupees(text: String): Double? {
        // EPFO SMS leads with passbook balance; prefer contribution when present
        EPFO_CONTRIBUTION.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        VERB_CURRENCY_AMOUNT.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        VERB_BARE_AMOUNT.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            if (it > 0) return it
        }
        // Prefer txn amount over trailing available-balance when both exist:
        // take the first currency amount that is not immediately after a balance phrase.
        findTxnCurrencyAmount(text)?.let { if (it > 0) return it }

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

    private fun findTxnCurrencyAmount(text: String): Double? {
        val balanceNear = Regex(
            """(?i)(?:(?:avail(?:able)?|avl|avbl)\s*bal(?:ance)?|(?:passbook\s+)?bal(?:ance)?(?:\s+against\s+[A-Z0-9*]+)?)\s*(?:is|:)?\s*(?:rs\.?|inr|₹)?\s*[\d,]+\.?\d*"""
        )
        val balanceSpans = balanceNear.findAll(text).map { it.range }.toList()
        for (match in CURRENCY_PREFIX.findAll(text)) {
            val overlapsBalance = balanceSpans.any { bal ->
                match.range.first >= bal.first && match.range.last <= bal.last
            }
            if (overlapsBalance) continue
            val value = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            if (value > 0) return value
        }
        return null
    }
}
