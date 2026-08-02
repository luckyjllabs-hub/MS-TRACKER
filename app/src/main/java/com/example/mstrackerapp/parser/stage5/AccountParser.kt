package com.example.mstrackerapp.parser.stage5

object AccountParser {
    private val CARD_PATTERN = Regex(
        """(?i)(?:card|ending\s*(?:in|with)|card\s*no\.?)\s*[*X]{0,6}(\d{4})"""
    )
    private val ACCOUNT_PATTERN = Regex(
        """(?i)(?:A/C|Acct\.?|Account)\s*(?:No\.?|ending)?\s*[*X]*(\d{4})"""
    )

    fun extractCardLast4(body: String): String = CARD_PATTERN.find(body)?.groupValues?.get(1) ?: ""
    fun extractAccountLast4(body: String): String = ACCOUNT_PATTERN.find(body)?.groupValues?.get(1) ?: ""
}
