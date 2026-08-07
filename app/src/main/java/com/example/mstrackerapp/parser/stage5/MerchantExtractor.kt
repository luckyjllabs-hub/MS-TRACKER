package com.example.mstrackerapp.parser.stage5

import com.example.mstrackerapp.parser.classifier.MerchantNormalizer

/**
 * Extracts and normalizes merchant names from bank SMS bodies.
 * Supports UPI, POS, Card, ATM, NEFT, IMPS, RTGS, Wallet, Refund, Salary patterns.
 */
object MerchantExtractor {

    /** "from VPA r.rajeshbarath-2@oksbi" — capture handle, never the word VPA. */
    private val FROM_VPA_REGEX = Regex(
        """(?i)\bfrom\s+VPA\s+([a-zA-Z0-9][\w.\-]{1,40})@([\w.\-]+)"""
    )
    private val ANY_VPA_HANDLE_REGEX = Regex(
        """(?i)\bVPA\s+([a-zA-Z0-9][\w.\-]{1,40})@([\w.\-]+)"""
    )

    private val PERSON_MERCHANT_PATTERNS = listOf(
        // ICICI UPI: "; YASHIKA CHICKEN credited."
        Regex("""(?i);\s*([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,40})\s+credited"""),
        // Inbound: "credited with Rs 275.00 on 07-Aug-26 from RAVI SANKAR GUN" (not VPA)
        Regex("""(?i)\b(?:credited|deposited)\b.{0,60}?\bfrom\s+(?!VPA\b)([A-Z][A-Za-z][A-Za-z.\s]{1,40}?)(?:\.|\s+UPI\b|\s+RRN|\s+IMPS|\s+Ref|\s+NEFT|\s+\(|$)"""),
        // Inbound credit: "credited with/by Rs… on DATE by PERSON"
        Regex("""(?i)\bcredited\s+(?:with|by)\s+(?:rs\.?|inr|₹)\s*[\d,]+\.?\d*.{0,48}?\bby\s+([A-Z][A-Za-z][A-Za-z.\s]{1,40}?)(?:\.|\s+RRN|\s+IMPS|\s+Ref|\s+UPI|\s+NEFT|\s+\(|$)"""),
        // "transfer from PERSON"
        Regex("""(?i)\btransfer\s+from\s+(?!VPA\b)([A-Z][A-Za-z][A-Za-z.\s]{1,40}?)(?:\s+Ref|\s+UPI|\s+on\b|\.|$)"""),
        // HDFC deposit: "for XXXXXXXXXX7408-TPT-HDFC72732E5BFB93-POLAMREDDY KARTHIK REDDY"
        Regex("""(?i)\bfor\s+[A-Z0-9*]{6,}-[A-Z0-9-]*-([A-Z][A-Z\s.]{2,40}?)(?:\.|Avl|Available|$)"""),
        // Generic trailing person after last hyphen before Avl bal
        Regex("""(?i)-([A-Z][A-Z]+(?:\s+[A-Z][A-Z]+){1,3})\s*\.?\s*(?:Avl|Available)\b"""),
        // POS / spent at (do NOT use bare "at" — too greedy)
        Regex("""(?i)\b(?:pos\s+(?:txn\s+)?at|spent\s+at|used\s+at|spent\s+on|towards)\s+([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,40})(?:\s*\.|\s+on\s+\d|\s+via|\s+ref|\s+avail|$)"""),
        // from person (standalone, not VPA)
        Regex("""(?i)\bfrom\s+(?!VPA\b)([A-Z][A-Za-z][A-Za-z.\s]{1,40}?)(?:\s*\.|\s+on\s+\d|\s+via|\s+UPI\b|\s+ref|\s+RRN|$)"""),
        // paid / transferred to
        Regex("""(?i)\b(?:paid\s+to|sent\s+to|transferred\s+to|trf\s+to|to\s+VPA)\s+([A-Za-z0-9][A-Za-z0-9\s@&'.-]{2,40})(?:\s*\.|\s+on\s+\d|\s+via|\s+UPI|\s+ref|$)"""),
        // IMPS/NEFT inbound name: "For IMPS -AMITKUMAR-" or "NEFT-HSBCN…-SAMSUN"
        Regex("""(?i)\b(?:IMPS|NEFT|RTGS)\s*[-/]\s*([A-Za-z][A-Za-z0-9\s&'.-]{2,30})"""),
        Regex("""(?i)\b(?:IMPS|NEFT|RTGS)[-/][A-Z0-9]+[-/]([A-Za-z][A-Za-z]+)"""),
        // InfoBIL / Info ACH
        Regex("""(?i)\bInfo(?:BIL)?[*:\s]+([A-Za-z][A-Za-z0-9\s&*.-]{2,30})"""),
        Regex("""(?i)(?:merchant|store):\s*([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,25})""")
    )

    /** Only real VPAs like name@upi — never UPI:ref numbers. */
    private val UPI_VPA_REGEX = Regex(
        """(?i)(?:VPA\s+|UPI[-/])([a-zA-Z][\w.\-]{1,30})@([\w.\-]+)"""
    )
    private val ATM_REGEX = Regex(
        """(?i)ATM[* ]([A-Z0-9.\-\s]{3,20})"""
    )

    private val IGNORE_WORDS = setOf(
        "CRED", "CALL", "SMS", "BLOCK", "DISPUTE", "YOUR", "ACCOUNT", "A/C", "ACCT", "BANK",
        "REF", "UPI", "DEBIT", "CREDIT", "PREPAID", "CARD", "PREPAID CARD", "CREDIT CARD",
        "DEBIT CARD", "ICICI BANK PREPAID", "HDFC BANK", "SBI", "SUCCESSFUL", "AVBL", "BAL",
        "NEFT", "IMPS", "RTGS", "INFT", "ACH", "INFO", "A/C LINKED", "AC LINKED", "LINKED",
        "MOBILE", "NO", "RS", "TPT", "HDFC", "ICICI", "UPDATE", "VPA"
    )

    fun extractMerchant(body: String, extraAliases: Map<String, String> = emptyMap()): String {
        val raw = extractRaw(body)
        return MerchantNormalizer.normalize(raw, extraAliases)
    }

    /** Raw extraction before alias normalization (useful for learning aliases). */
    fun extractRaw(body: String): String {
        val lowerBody = body.lowercase()

        // 0. VPA handle first — "from VPA r.rajeshbarath-2@oksbi"
        FROM_VPA_REGEX.find(body)?.groupValues?.getOrNull(1)?.let { handle ->
            val cleaned = sanitizeCandidate(handle)
            if (cleaned != null) return cleaned
        }
        ANY_VPA_HANDLE_REGEX.find(body)?.groupValues?.getOrNull(1)?.let { handle ->
            val cleaned = sanitizeCandidate(handle)
            if (cleaned != null) return cleaned
        }

        // 1. Brand overrides from body (high-confidence tokens) — whole word only for short brands
        when {
            lowerBody.contains("medplus") -> return "MEDPLUS"
            lowerBody.contains("blinkit") -> return "BLINKIT"
            lowerBody.contains("zepto") -> return "ZEPTO"
            lowerBody.contains("hyderabad irani") || lowerBody.contains("irani cafe") -> return "HYDERABAD IRANI"
            Regex("""(?i)\b(?:apay|a\.in|amazon)\b""").containsMatchIn(body) -> return "AMAZON"
            lowerBody.contains("upi lite") || lowerBody.contains("top-up") || lowerBody.contains("topup") -> return "UPI LITE TOP-UP"
            Regex("""(?i)\bswiggy\b""").containsMatchIn(body) -> return "SWIGGY"
            Regex("""(?i)\bzomato\b""").containsMatchIn(body) -> return "ZOMATO"
            Regex("""(?i)\buber\b""").containsMatchIn(body) -> return "UBER"
            Regex("""(?i)\bola\b""").containsMatchIn(body) -> return "OLA"
            lowerBody.contains("yashika chicken") -> return "YASHIKA CHICKEN"
            lowerBody.contains("green city") -> return "GREEN CITY SUPERMARKET"
            lowerBody.contains("bescom") -> return "BESCOM"
            lowerBody.contains("jio prepaid") || (Regex("""(?i)\bjio\b""").containsMatchIn(body) && lowerBody.contains("prepaid")) -> return "JIO"
            Regex("""(?i)\baster\b""").containsMatchIn(body) -> return "ASTER"
            lowerBody.contains("gail gas") || lowerBody.contains("gailin") -> return "GAIL GAS"
            Regex("""(?i)\bsalary\b""").containsMatchIn(body) -> return "SALARY"
            Regex("""(?i)\brefund\b""").containsMatchIn(body) -> return "REFUND"
        }

        // 2. Person / merchant regex patterns (before UPI ref / brand scan)
        for (pattern in PERSON_MERCHANT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val cleaned = sanitizeCandidate(match.groupValues[1]) ?: continue
            return cleaned
        }

        // 3. Real UPI VPA only
        UPI_VPA_REGEX.find(body)?.groupValues?.getOrNull(1)?.let { candidate ->
            val cleaned = sanitizeCandidate(candidate)
            if (cleaned != null) return cleaned
        }

        // 4. ATM terminal
        ATM_REGEX.find(body)?.groupValues?.getOrNull(1)?.let { terminal ->
            val cleaned = sanitizeCandidate(terminal)
            if (cleaned != null) return "ATM $cleaned"
        }

        // 5. Known brand scan via normalizer aliases (word-boundary safe)
        for (alias in MerchantNormalizer.BUILTIN_ALIASES.keys.sortedByDescending { it.length }) {
            if (MerchantNormalizer.aliasMatchesText(body, alias)) return alias
        }

        // 6. Typed placeholders (never raw DEBIT/CREDIT)
        return when {
            lowerBody.contains("salary") || lowerBody.contains("payroll") -> "SALARY"
            lowerBody.contains("refund") || lowerBody.contains("reversal") -> "REFUND"
            lowerBody.contains("atm") -> "ATM"
            lowerBody.contains("deposited") || lowerBody.contains("credited") || lowerBody.contains("received") -> "BANK DEPOSIT"
            lowerBody.contains("upi") -> "UPI TRANSFER"
            lowerBody.contains("neft") || lowerBody.contains("imps") || lowerBody.contains("rtgs") -> "BANK TRANSFER"
            lowerBody.contains("card") || lowerBody.contains("pos") -> "CARD PURCHASE"
            else -> "BANK TRANSACTION"
        }
    }

    private fun sanitizeCandidate(raw: String): String? {
        var name = raw.trim().replace(Regex("""\s+"""), " ")
        name = name.split(Regex("""(?i)\s+(?:updated|ref|refno|if|call|is|with|from|to|a/c|balance|card|on\s+\d|at|avail|avl|upi)\b"""))
            .first().trim()
        if (name.endsWith(".")) name = name.dropLast(1).trim()
        name = name.replace(Regex("""\*+"""), " ").replace(Regex("""\s+"""), " ").trim()
        // Drop trailing bank suffixes
        name = name.replace(Regex("""(?i)\s*-\s*(?:ICICI|HDFC|SBI|AXIS)\s*BANK.*$"""), "").trim()
        // Strip leading VPA token if present
        name = name.replace(Regex("""(?i)^VPA\s+"""), "").trim()

        val nameUpper = name.uppercase()
        if (name.length < 3) return null
        if (IGNORE_WORDS.contains(nameUpper)) return null
        if (nameUpper.startsWith("A/C") || nameUpper.startsWith("ACCT") || nameUpper.startsWith("ACCOUNT")) return null
        if (nameUpper.contains("PREPAID CARD") || nameUpper.contains("CREDIT CARD") || nameUpper.contains("DEBIT CARD")) return null
        if (name.all { it.isDigit() || it == 'X' || it == 'x' || it == '-' }) return null
        // Keep VPA handles like r.rajeshbarath-2 as-is (don't title-case later via token clip of "VPA")
        if (name.contains('.') || name.contains('-') || name.contains('_')) {
            return name.lowercase().take(40)
        }
        val tokens = nameUpper.split(" ").filter { it.isNotBlank() && it != "VPA" }
        val clipped = tokens.take(3).joinToString(" ")
        return clipped.take(40).ifBlank { null }
    }
}
