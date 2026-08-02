package com.example.mstrackerapp.parser.stage5

object MerchantExtractor {

    private val PERSON_MERCHANT_PATTERNS = listOf(
        // ICICI UPI debit pattern: "; SANCHI SIVAKUMA credited."
        Regex("""(?i);\s*([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,30})\s+credited"""),
        // Credit/Debit from person: "from SANCHI SIVAKUMA." or "from VPA x@y"
        Regex("""(?i)\bfrom\s+(?:VPA\s+)?([A-Za-z0-9][A-Za-z0-9\s@&'.-]{2,30})(?:\s*\.|\s+on|\s+via|\s+UPI|$)"""),
        // Debit to person/merchant: "paid to Swiggy", "transferred to SANCHI SIVAKUMA"
        Regex("""(?i)\b(?:paid\s+to|sent\s+to|transferred\s+to|to\s+VPA)\s+([A-Za-z0-9][A-Za-z0-9\s@&'.-]{2,30})(?:\s*\.|\s+on|\s+via|\s+UPI|$)"""),
        // Spent at/on merchant: "spent at Starbucks", "used at DMART"
        Regex("""(?i)\b(?:at|@|spent\s+at|used\s+at|spent\s+on|towards)\s+([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,30})(?:\s*\.|\s+on|\s+via|$)"""),
        Regex("""(?i)(?:merchant|store):\s*([A-Za-z0-9][A-Za-z0-9\s&'.-]{2,25})""")
    )

    private val KNOWN_MERCHANTS = listOf(
        "Swiggy", "Zomato", "Uber", "Ola", "Amazon", "Flipkart", "Myntra",
        "Starbucks", "Netflix", "Spotify", "Hotstar", "Jio", "Airtel", "BSNL",
        "BigBasket", "Blinkit", "Dunzo", "Zepto", "PharmEasy", "1mg",
        "Practo", "Apollo", "MakeMyTrip", "Goibibo", "IRCTC",
        "PayTM", "PhonePe", "Google Pay", "Groww", "Zerodha",
        "Paytm Mall", "Reliance", "DMart", "BigBazaar", "BESCOM"
    )

    private val IGNORE_WORDS = setOf(
        "CRED", "CALL", "SMS", "BLOCK", "DISPUTE", "YOUR", "ACCOUNT", "A/C", "ACCT", "BANK", "REF", "UPI"
    )

    fun extractMerchant(body: String): String {
        // 1. Check person/merchant patterns (e.g. "; SANCHI SIVAKUMA credited.", "from SANCHI SIVAKUMA.")
        for (pattern in PERSON_MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                var name = match.groupValues[1].trim()
                    .replace(Regex("""\s+"""), " ")
                if (name.endsWith(".")) name = name.dropLast(1).trim()
                if (name.length >= 3 && !IGNORE_WORDS.contains(name.uppercase()) && !name.all { it.isDigit() }) {
                    return name.uppercase()
                }
            }
        }

        // 2. Known merchant name scan
        for (merchant in KNOWN_MERCHANTS) {
            if (body.contains(merchant, ignoreCase = true)) return merchant.uppercase()
        }

        return "DEBIT"
    }
}
