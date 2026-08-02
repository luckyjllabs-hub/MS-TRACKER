package com.example.mstrackerapp.parser.stage5

import com.example.mstrackerapp.parser.classifier.MerchantNormalizer

/**
 * Extracts and normalizes merchant names from bank SMS bodies.
 * Supports UPI, POS, Card, ATM, NEFT, IMPS, RTGS, Wallet, Refund, Salary patterns.
 */
object MerchantExtractor {

    private val PERSON_MERCHANT_PATTERNS = listOf(
        // ICICI UPI: "; YASHIKA CHICKEN credited."
        Regex("""(?i);\s*([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,40})\s+credited"""),
        // POS / spent at
        Regex("""(?i)\b(?:pos\s+(?:txn\s+)?at|at|@|spent\s+at|used\s+at|spent\s+on|towards|info:)\s+([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,40})(?:\s*\.|\s+on\s+\d|\s+via|\s+ref|\s+avail|$)"""),
        // from person / VPA
        Regex("""(?i)\bfrom\s+(?:VPA\s+)?([A-Za-z0-9][A-Za-z0-9\s@&'.-]{2,40})(?:\s*\.|\s+on\s+\d|\s+via|\s+UPI|\s+ref|$)"""),
        // paid / transferred to
        Regex("""(?i)\b(?:paid\s+to|sent\s+to|transferred\s+to|trf\s+to|to\s+VPA)\s+([A-Za-z0-9][A-Za-z0-9\s@&'.-]{2,40})(?:\s*\.|\s+on\s+\d|\s+via|\s+UPI|\s+ref|$)"""),
        // IMPS inbound: "For IMPS -AMITKUMAR-"
        Regex("""(?i)\b(?:IMPS|NEFT|RTGS)\s*[-/]\s*([A-Za-z][A-Za-z0-9\s&'.-]{2,30})"""),
        // InfoBIL / Info ACH
        Regex("""(?i)\bInfo(?:BIL)?[*:\s]+([A-Za-z][A-Za-z0-9\s&*.-]{2,30})"""),
        Regex("""(?i)(?:merchant|store):\s*([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,25})""")
    )

    private val UPI_REGEX = Regex(
        """(?i)(?:UPI[-/]|VPA\s+)([\w.\-\s]+?)(?:@|\s+Ref|\s+on|\s+dt|\s+balance|\.|$)"""
    )
    private val ATM_REGEX = Regex(
        """(?i)ATM[* ]([A-Z0-9.\-\s]{3,20})"""
    )

    private val IGNORE_WORDS = setOf(
        "CRED", "CALL", "SMS", "BLOCK", "DISPUTE", "YOUR", "ACCOUNT", "A/C", "ACCT", "BANK",
        "REF", "UPI", "DEBIT", "CREDIT", "PREPAID", "CARD", "PREPAID CARD", "CREDIT CARD",
        "DEBIT CARD", "ICICI BANK PREPAID", "HDFC BANK", "SBI", "SUCCESSFUL", "AVBL", "BAL",
        "NEFT", "IMPS", "RTGS", "INFT", "ACH", "INFO"
    )

    fun extractMerchant(body: String, extraAliases: Map<String, String> = emptyMap()): String {
        val raw = extractRaw(body)
        return MerchantNormalizer.normalize(raw, extraAliases)
    }

    /** Raw extraction before alias normalization (useful for learning aliases). */
    fun extractRaw(body: String): String {
        val lowerBody = body.lowercase()

        // 1. Brand overrides from body (high-confidence tokens)
        when {
            lowerBody.contains("medplus") -> return "MEDPLUS"
            lowerBody.contains("blinkit") -> return "BLINKIT"
            lowerBody.contains("zepto") -> return "ZEPTO"
            lowerBody.contains("hyderabad irani") || lowerBody.contains("irani cafe") -> return "HYDERABAD IRANI"
            lowerBody.contains("apay") || lowerBody.contains("a.in") || lowerBody.contains("amazon") -> return "AMAZON"
            lowerBody.contains("upi lite") || lowerBody.contains("top-up") || lowerBody.contains("topup") -> return "UPI LITE TOP-UP"
            lowerBody.contains("swiggy") -> return "SWIGGY"
            lowerBody.contains("zomato") -> return "ZOMATO"
            lowerBody.contains("uber") -> return "UBER"
            lowerBody.contains("yashika chicken") -> return "YASHIKA CHICKEN"
            lowerBody.contains("green city") -> return "GREEN CITY SUPERMARKET"
            lowerBody.contains("bescom") -> return "BESCOM"
            lowerBody.contains("jio prepaid") || (lowerBody.contains("jio") && lowerBody.contains("prepaid")) -> return "JIO"
            lowerBody.contains("aster") -> return "ASTER"
            lowerBody.contains("gail gas") || lowerBody.contains("gailin") -> return "GAIL GAS"
            lowerBody.contains("salary") -> return "SALARY"
            lowerBody.contains("refund") -> return "REFUND"
        }

        // 2. UPI VPA / paid-to patterns from MerchantParser
        UPI_REGEX.find(body)?.groupValues?.getOrNull(1)?.let { candidate ->
            val cleaned = sanitizeCandidate(candidate)
            if (cleaned != null) return cleaned
        }

        // 3. Person / merchant regex patterns
        for (pattern in PERSON_MERCHANT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val cleaned = sanitizeCandidate(match.groupValues[1]) ?: continue
            return cleaned
        }

        // 4. ATM terminal
        ATM_REGEX.find(body)?.groupValues?.getOrNull(1)?.let { terminal ->
            val cleaned = sanitizeCandidate(terminal)
            if (cleaned != null) return "ATM $cleaned"
        }

        // 5. Known brand scan via normalizer aliases
        for (alias in MerchantNormalizer.BUILTIN_ALIASES.keys.sortedByDescending { it.length }) {
            if (body.contains(alias, ignoreCase = true)) return alias
        }

        // 6. Typed placeholders (never raw DEBIT/CREDIT)
        return when {
            lowerBody.contains("salary") || lowerBody.contains("payroll") -> "SALARY"
            lowerBody.contains("refund") || lowerBody.contains("reversal") -> "REFUND"
            lowerBody.contains("atm") -> "ATM"
            lowerBody.contains("credited") || lowerBody.contains("received") -> "BANK DEPOSIT"
            lowerBody.contains("upi") -> "UPI TRANSFER"
            lowerBody.contains("neft") || lowerBody.contains("imps") || lowerBody.contains("rtgs") -> "BANK TRANSFER"
            lowerBody.contains("card") || lowerBody.contains("pos") -> "CARD PURCHASE"
            else -> "BANK TRANSACTION"
        }
    }

    private fun sanitizeCandidate(raw: String): String? {
        var name = raw.trim().replace(Regex("""\s+"""), " ")
        name = name.split(Regex("""(?i)\s+(?:updated|ref|refno|if|call|is|with|from|to|a/c|balance|card|on|at|avail|avl)\b"""))
            .first().trim()
        if (name.endsWith(".")) name = name.dropLast(1).trim()
        name = name.replace(Regex("""\*+"""), " ").replace(Regex("""\s+"""), " ").trim()

        val nameUpper = name.uppercase()
        if (name.length < 3) return null
        if (IGNORE_WORDS.contains(nameUpper)) return null
        if (nameUpper.contains("PREPAID CARD") || nameUpper.contains("CREDIT CARD") || nameUpper.contains("DEBIT CARD")) return null
        if (name.all { it.isDigit() || it == 'X' || it == 'x' }) return null
        return nameUpper.take(32)
    }
}
