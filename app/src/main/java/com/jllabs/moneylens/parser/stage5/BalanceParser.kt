package com.jllabs.moneylens.parser.stage5

object BalanceParser {
    private val BALANCE_PATTERN = Regex(
        """(?i)(?:avail(?:able)?\s*bal(?:ance)?|total\s*avail\.?\s*bal(?:ance)?|total\s*(?:avail(?:able)?\.?\s*)?bal(?:ance)?|bal(?:ance)?|avl\s*bal|avbl\s*bal|avb\s*bal)\s*(?:is|:|\.|-)?\s*(?:INR|Rs\.?|₹)?\s*([-]?[\d,]+\.?\d*)"""
    )

    /** "available balance of INR 0.01" / "unclaimed balance of Rs 0.01" */
    private val BALANCE_OF_PATTERN = Regex(
        """(?i)(?:avail(?:able)?|unclaimed)\s+balance\s+of\s+(?:INR|Rs\.?|₹)\s*([-]?[\d,]+\.?\d*)"""
    )

    /** Credit card: "Avl Limit: INR 3,40,000.00" */
    private val LIMIT_PATTERN = Regex(
        """(?i)(?:avl|available|avail\.?)\s*limit\s*(?:is|:|\.|-)?\s*(?:INR|Rs\.?|₹)?\s*([-]?[\d,]+\.?\d*)"""
    )

    /** EPFO: "passbook balance against BGBNG***2889 is Rs. 41,85,400/-" */
    private val EPFO_BALANCE_PATTERN = Regex(
        """(?i)(?:passbook\s+)?bal(?:ance)?(?:\s+against\s+[A-Z0-9*]+)?\s*(?:is|:)\s*(?:INR|Rs\.?|₹)?\s*([-]?[\d,]+\.?\d*)"""
    )

    fun extractBalanceMinor(body: String): Long? {
        parseRupees(BALANCE_PATTERN.find(body)?.groupValues?.get(1))?.let { return it }
        parseRupees(BALANCE_OF_PATTERN.find(body)?.groupValues?.get(1))?.let { return it }
        parseRupees(EPFO_BALANCE_PATTERN.find(body)?.groupValues?.get(1))?.let { return it }
        return null
    }

    fun extractCreditLimitMinor(body: String): Long? =
        parseRupees(LIMIT_PATTERN.find(body)?.groupValues?.get(1))

    /** Balance for bank SMS, or available credit limit for card SMS. */
    fun extractDisplayBalanceMinor(body: String): Long? {
        if (AccountParser.isCreditCardSms(body)) {
            extractCreditLimitMinor(body)?.let { return it }
        }
        return extractBalanceMinor(body) ?: extractCreditLimitMinor(body)
    }

    private fun parseRupees(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(",", "").replace("/-", "").trim()
        val rupees = cleaned.toDoubleOrNull() ?: return null
        return (rupees * 100).toLong()
    }
}
