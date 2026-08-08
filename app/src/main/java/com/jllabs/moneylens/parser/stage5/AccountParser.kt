package com.jllabs.moneylens.parser.stage5

/**
 * Extracts masked account / card / FASTag last digits from Indian bank SMS.
 *
 * Handles:
 * - `A/c XX0328`, `A/C x5247`, `Acct XX346`, `XXXX1640`
 * - `a/c no. XXXXXXXX9642`, `A/C XXXXX082985` (take last 3–4 visible digits)
 * - `Card XX0018`, `Credit Card Account 4xxx0018`
 * - `Loan A/c XX8508`, `Home Loan XX8970`
 * - `ICICI Bank FASTag` (+ vehicle `KA03MK9502`) — never Bal Rs.240 as last4
 * - EPFO `BGBNG**************2889`
 */
object AccountParser {

    private val CREDIT_CARD_PATTERNS = listOf(
        Regex("""(?i)(?:credit\s*card\s*(?:account|a/?c)?|card\s+account)\s*(?:no\.?|number)?\s*:?\s*\d?[xX*]{2,}(\d{3,4})\b"""),
        Regex("""(?i)(?:ICICI|HDFC|SBI|AXIS|KOTAK)?\s*Bank\s+Card\s*[xX*]{0,12}(\d{3,4})\b"""),
        Regex("""(?i)card\s+ending(?:\s*(?:in|with))?\s*[xX*]{0,12}(\d{4})\b"""),
        Regex("""(?i)\bCard\s+[xX*]{1,12}(\d{3,4})\b""")
    )

    private val ACCOUNT_PATTERNS = listOf(
        Regex("""(?i)(?:home\s+)?loan\s+(?:a/?c|account)?\s*[xX*]{0,16}(\d{3,4})\b"""),
        Regex("""(?i)ending(?:\s+with)?\s*[xX*]{2,16}(\d{3,4})\b"""),
        Regex("""(?i)(?:A/C|Acct\.?|Account)\s*(?:No\.?|No\s*:|ending(?:\s*(?:in|with))?)?\s*[xX*]{0,16}(\d{3,6})\b"""),
        Regex("""(?i)\bAcc\s+(?:No\.?|No\s*:|ending(?:\s*(?:in|with))?)\s*[xX*]{0,16}(\d{3,6})\b"""),
        Regex("""(?i)\bAcc\s+[xX*]{2,16}(\d{3,6})\b"""),
        Regex("""(?i)\b(?:your\s+)?(?:a/?c|acct|account)\b[^0-9xX*]{0,12}[xX*]{2,}(\d{3,6})\b"""),
        Regex("""(?i)\b(?:from|in|to)\s+[\w\s]*?(?:a/?c|acct|account)\s*[xX*]{1,16}(\d{3,6})\b"""),
        Regex("""(?i)(?:passbook\s+balance\s+against\s+)?[A-Z]{3,8}\*{6,}(\d{3,4})\b"""),
        // Require at least 2 mask chars so "Bank FASTag. Bal Rs.240" cannot match
        Regex("""(?i)\b(?:bank)\b[^0-9xX*]{0,24}[xX*]{2,16}(\d{3,6})\b""")
    )

    private val CREDIT_CARD_HINT = Regex(
        """(?i)\b(?:
            credit\s*card\s+(?:account|a/?c|xx|ending)|
            avl\s*limit|available\s*limit|
            card\s+[xX*]+\d{3,4}|
            bank\s+card\s+[xX*]*\d{3,4}|
            card\s+ending|
            spent\s+using.{0,40}card|
            (?:using|on)\s+.{0,30}credit\s*card\s+[xX*]
        )\b""",
        RegexOption.COMMENTS
    )

    private val CREDIT_CARD_FACILITY_ON_BANK_AC = Regex(
        """(?i)credit\s*card\s+facility.{0,100}(?:bank\s+)?(?:a/?c|account)\s*[xX*]"""
    )

    private val FASTAG_HINT = Regex("""(?i)\bfastag\b""")

    /** Life/insurance policy SMS — not a bank account by themselves. */
    private val INSURANCE_POLICY_HINT = Regex(
        """(?i)\b(?:
            icicipru|icici\s*pru|prudential|
            \bpolicy\b.{0,40}\bis\s+due\b|
            premium\s+(?:of|debit|notice|will\s+be\s+deducted)|
            standing\s+instructions|
            hdfc\s*life|sbi\s*life|lic\s+of\s+india|\blic\b|
            ipru\.co|max\s*life|bajaj\s*allianz
        )\b""",
        RegexOption.COMMENTS
    )

    private val EXPLICIT_BANK_AC_IN_SMS = Regex(
        """(?i)(?:bank\s+)?(?:a/?c|acct|account)\s*(?:no\.?)?\s*(?:ending(?:\s*(?:in|with))?)?\s*[xX*]+\d{3,}"""
    )

    /** Indian vehicle reg e.g. KA03MK9502 */
    private val VEHICLE_REG = Regex("""(?i)\b([A-Z]{2}\d{1,2}[A-Z]{1,3}\d{3,4})\b""")

    private val BALANCE_SPAN = Regex(
        """(?i)(?:bal(?:ance)?|avl\s*bal|avbl\s*bal|avb\s*bal)\s*(?:is|:|\.)?\s*(?:INR|Rs\.?|₹)?\s*[\d,]+\.?\d*"""
    )

    fun isInsuranceOrPolicySms(body: String): Boolean =
        INSURANCE_POLICY_HINT.containsMatchIn(body)

    fun isFasTagSms(body: String): Boolean = FASTAG_HINT.containsMatchIn(body)

    fun isCreditCardSms(body: String): Boolean {
        if (body.isBlank() || isFasTagSms(body) || isInsuranceOrPolicySms(body)) return false
        if (CREDIT_CARD_FACILITY_ON_BANK_AC.containsMatchIn(body)) return false
        if (CREDIT_CARD_HINT.containsMatchIn(body)) return true
        val cardLast4 = extractCardLast4(body)
        if (cardLast4.isBlank()) return false
        val hasBankAc = Regex("""(?i)\b(?:a/?c|acct|account)\s*[xX*]""").containsMatchIn(body)
        return !hasBankAc || CREDIT_CARD_HINT.containsMatchIn(body)
    }

    fun extractCardLast4(body: String): String {
        for (p in CREDIT_CARD_PATTERNS) {
            val m = p.find(body)?.groupValues?.get(1).orEmpty()
            if (m.isNotBlank()) return normalizeLastDigits(m)
        }
        return ""
    }

    /**
     * FASTag wallet id: prefer vehicle last 4 digits; else stable "FTAG".
     * Never use Bal Rs.####.
     */
    fun extractFasTagLast4(body: String): String {
        VEHICLE_REG.find(body)?.groupValues?.get(1)?.let { vehicle ->
            val digits = vehicle.filter { it.isDigit() }
            if (digits.length >= 4) return digits.takeLast(4)
            if (digits.isNotEmpty()) return digits
        }
        return "FTAG"
    }

    fun extractVehicleNumber(body: String): String =
        VEHICLE_REG.find(body)?.groupValues?.get(1).orEmpty().uppercase()

    fun extractAccountLast4(body: String): String {
        if (body.isBlank()) return ""
        if (isFasTagSms(body)) return extractFasTagLast4(body)
        // Insurance/policy due SMS: only accept last4 when a real bank a/c mask is present
        if (isInsuranceOrPolicySms(body) && !EXPLICIT_BANK_AC_IN_SMS.containsMatchIn(body)) {
            return ""
        }
        if (isCreditCardSms(body)) {
            extractCardLast4(body).takeIf { it.isNotBlank() }?.let { return it }
        }
        val cleaned = stripUrls(stripBalanceSpans(body))
        for (p in ACCOUNT_PATTERNS) {
            val m = p.find(cleaned)?.groupValues?.get(1).orEmpty()
            if (m.isNotBlank()) return normalizeLastDigits(m)
        }
        return extractCardLast4(cleaned)
    }

    fun extractAccountOrCardLast4(body: String): String =
        extractAccountLast4(body).ifBlank { extractCardLast4(body) }

    fun isLoanAccountSms(body: String): Boolean =
        !isFasTagSms(body) &&
            !isInsuranceOrPolicySms(body) &&
            Regex("""(?i)\b(?:home\s+)?loan\s+(?:a/?c|account)?\b""").containsMatchIn(body)

    /** Keep last 4 digits when longer (e.g. 082985 → 2985); keep 3 for ICICI-style XX346. */
    fun normalizeLastDigits(raw: String): String {
        if (raw.equals("FTAG", ignoreCase = true)) return "FTAG"
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            digits.length <= 4 -> digits
            else -> digits.takeLast(4)
        }
    }

    fun preferCanonicalLast4(candidates: Collection<String>): String {
        val clean = candidates.map { normalizeLastDigits(it) }.filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) return ""
        return clean.maxWithOrNull(
            compareBy<String> { it.length }
                .thenByDescending { candidate ->
                    clean.count { other -> other != candidate && (candidate.endsWith(other) || other.endsWith(candidate)) }
                }
                .thenBy { it }
        ) ?: clean.first()
    }

    /** Remove balance phrases so Bal Rs.240 cannot be mistaken for an account mask. */
    fun stripBalanceSpans(body: String): String =
        BALANCE_SPAN.replace(body, " ")

    private fun stripUrls(body: String): String =
        body.replace(Regex("""(?i)https?://\S+"""), " ")
}
