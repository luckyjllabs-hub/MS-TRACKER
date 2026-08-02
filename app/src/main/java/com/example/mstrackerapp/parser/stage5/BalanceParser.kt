package com.example.mstrackerapp.parser.stage5

object BalanceParser {
    private val BALANCE_PATTERN = Regex(
        """(?i)(?:avail(?:able)?\s*bal(?:ance)?|bal(?:ance)?|avl\s*bal)\s*[:\.\-]?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)"""
    )

    /** Returns available balance in paisa, or null if not found */
    fun extractBalanceMinor(body: String): Long? {
        val rupees = BALANCE_PATTERN.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        return rupees?.let { (it * 100).toLong() }
    }
}
