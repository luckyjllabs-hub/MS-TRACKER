package com.example.mstrackerapp.parser.stage5

object BalanceParser {
    private val BALANCE_PATTERN = Regex(
        """(?i)(?:avail(?:able)?\s*bal(?:ance)?|bal(?:ance)?|avl\s*bal|avbl\s*bal)\s*(?:is|:|\.|-)?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)"""
    )

    /** EPFO: "passbook balance against BGBNG***2889 is Rs. 41,85,400/-" */
    private val EPFO_BALANCE_PATTERN = Regex(
        """(?i)(?:passbook\s+)?bal(?:ance)?(?:\s+against\s+[A-Z0-9*]+)?\s*(?:is|:)\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)"""
    )

    /** Returns available balance in paisa, or null if not found */
    fun extractBalanceMinor(body: String): Long? {
        val rupees = BALANCE_PATTERN.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: EPFO_BALANCE_PATTERN.find(body)?.groupValues?.get(1)?.replace(",", "")?.replace("/-", "")?.toDoubleOrNull()
        return rupees?.let { (it * 100).toLong() }
    }
}
