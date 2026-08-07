package com.example.mstrackerapp.parser.stage5

/**
 * Extracts masked account / card last digits from Indian bank SMS.
 *
 * ICICI often uses 3 digits after XX (`Acct XX346`, `Acc XX346`, `Account XX932`).
 * HDFC/SBI usually use 4 (`A/c XX0328`).
 * EPFO uses establishment ids like `BGBNG**************2889`.
 */
object AccountParser {

    private val CARD_PATTERN = Regex(
        """(?i)(?:card\s+ending(?:\s*(?:in|with))?|ending(?:\s*(?:in|with))?|card\s*(?:no\.?)?)\s*[*X]{0,12}(\d{4})\b"""
    )

    /**
     * Order: Acct before Acc, Account before Acc.
     * Allows `No` / `No:` / `ending in`.
     * Digits: 3 or 4.
     */
    private val ACCOUNT_PATTERN = Regex(
        """(?i)(?:A/C|Acct\.?|Account|Acc)\s*(?:No\.?|No\s*:|ending(?:\s*(?:in|with))?)?\s*[*X]*(\d{3,4})\b"""
    )

    private val MASKED_ACCOUNT_PATTERN = Regex(
        """(?i)\b(?:A/C|Acct\.?|Account|Acc)\b[^0-9]{0,20}(?:XX|X{2,}|\*{2,})?(\d{3,4})\b"""
    )

    /** EPFO / e-passbook: `BGBNG**************2889` or similar establishment codes. */
    private val EPFO_ACCOUNT_PATTERN = Regex(
        """(?i)(?:passbook\s+balance\s+against\s+)?[A-Z]{3,8}\*{6,}(\d{3,4})\b"""
    )

    /** Bare masked account when bank name precedes: `HDFC Bank A/c xx0328` already covered; also `XX9653` after bank. */
    private val BANK_MASKED_TAIL = Regex(
        """(?i)\b(?:bank|a/?c|acct|account)\b[^0-9]{0,8}(?:XX|X{2,}|\*{2,})(\d{3,4})\b"""
    )

    fun extractCardLast4(body: String): String =
        CARD_PATTERN.find(body)?.groupValues?.get(1).orEmpty()

    fun extractAccountLast4(body: String): String {
        ACCOUNT_PATTERN.find(body)?.groupValues?.get(1)?.let { if (it.isNotBlank()) return it }
        MASKED_ACCOUNT_PATTERN.find(body)?.groupValues?.get(1)?.let { if (it.isNotBlank()) return it }
        EPFO_ACCOUNT_PATTERN.find(body)?.groupValues?.get(1)?.let { if (it.isNotBlank()) return it }
        BANK_MASKED_TAIL.find(body)?.groupValues?.get(1)?.let { if (it.isNotBlank()) return it }
        return ""
    }

    /** Account digits, else card last4 — used when building Finance accounts. */
    fun extractAccountOrCardLast4(body: String): String =
        extractAccountLast4(body).ifBlank { extractCardLast4(body) }
}
