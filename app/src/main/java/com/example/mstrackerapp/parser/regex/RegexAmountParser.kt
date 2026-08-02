package com.example.mstrackerapp.parser.regex

object RegexAmountParser {

    private val VERB_AMOUNT_REGEX = Regex(
        """(?:debited|credited|spent|sent|paid|transferred|withdrawn|received)\s+(?:by|of|for)?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    private val AMOUNT_REGEX = Regex(
        """(?:INR|Rs\.?|₹|amt|amount|val|value|sum|for)\s*[:\.\-]?\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:INR|Rs\.?|₹)""",
        RegexOption.IGNORE_CASE
    )

    private val FALLBACK_NUMBER_REGEX = Regex("""([\d,]+\.\d{2})""")

    fun parseAmount(text: String): Double? {
        if (text.isBlank()) return null

        val verbMatch = VERB_AMOUNT_REGEX.find(text)
        if (verbMatch != null) {
            val str = verbMatch.groupValues[1].replace(",", "")
            val parsed = str.toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        val match = AMOUNT_REGEX.find(text)
        if (match != null) {
            val str = (match.groupValues[1].ifEmpty { match.groupValues[2] }).replace(",", "")
            val parsed = str.toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        val fallbackMatch = FALLBACK_NUMBER_REGEX.find(text)
        if (fallbackMatch != null) {
            val str = fallbackMatch.groupValues[1].replace(",", "")
            val parsed = str.toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        return null
    }
}
