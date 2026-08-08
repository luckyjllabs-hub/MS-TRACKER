package com.jllabs.moneylens.parser.regex

data class ExtractedSmsMetadata(
    val bankName: String? = null,
    val accountLast4: String? = null,
    val cardLast4: String? = null,
    val upiId: String? = null,
    val referenceNumber: String? = null,
    val availableBalance: Double? = null
)

object BankSmsParser {

    private val BANK_PATTERNS = mapOf(
        "HDFC Bank" to Regex("""HDFC\s*Bank|HDFCBK|HDFCB""", RegexOption.IGNORE_CASE),
        "ICICI Bank" to Regex("""ICICI\s*Bank|ICICIB""", RegexOption.IGNORE_CASE),
        "SBI" to Regex("""State\s*Bank|SBI|SBIN""", RegexOption.IGNORE_CASE),
        "Axis Bank" to Regex("""Axis\s*Bank|AXISBK""", RegexOption.IGNORE_CASE),
        "Kotak Bank" to Regex("""Kotak|KOTAKB""", RegexOption.IGNORE_CASE),
        "PNB" to Regex("""Punjab\s*National|PNBK""", RegexOption.IGNORE_CASE),
        "Bank of Baroda" to Regex("""Bank\s*of\s*Baroda|BOBTXN""", RegexOption.IGNORE_CASE),
        "Canara Bank" to Regex("""Canara|CNRB""", RegexOption.IGNORE_CASE),
        "Union Bank" to Regex("""Union\s*Bank|UBOI""", RegexOption.IGNORE_CASE),
        "IDFC FIRST" to Regex("""IDFC|IDFCFB""", RegexOption.IGNORE_CASE),
        "IndusInd Bank" to Regex("""IndusInd|INDUSB""", RegexOption.IGNORE_CASE),
        "Indian Bank" to Regex("""\bIndian\s*Bank\b|IDIBNK|\bIDIB\b""", RegexOption.IGNORE_CASE),
        "Federal Bank" to Regex("""Federal|FEDBNK""", RegexOption.IGNORE_CASE),
        "RBL Bank" to Regex("""RBL|RBLBNK""", RegexOption.IGNORE_CASE),
        "YES Bank" to Regex("""YES\s*Bank|YESBNK""", RegexOption.IGNORE_CASE),
        "Paytm" to Regex("""Paytm|PAYTM""", RegexOption.IGNORE_CASE),
        "PhonePe" to Regex("""PhonePe|PHONEPE""", RegexOption.IGNORE_CASE),
        "Google Pay" to Regex("""Google\s*Pay|GPAY""", RegexOption.IGNORE_CASE),
        "CRED" to Regex("""CRED""", RegexOption.IGNORE_CASE),
        "Amazon Pay" to Regex("""Amazon\s*Pay|AMAZON""", RegexOption.IGNORE_CASE)
    )

    private val ACCOUNT_REGEX = Regex(
        """(?:A/C|Acct\.?|Account|Acc)\s*(?:no\.?|ending(?:\s*(?:in|with))?)?\s*[*X]*(\d{3,4})\b""",
        RegexOption.IGNORE_CASE
    )

    private val CARD_REGEX = Regex(
        """(?:Card|ending in)\s*[*X]*(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    private val UPI_ID_REGEX = Regex(
        """([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)""",
        RegexOption.IGNORE_CASE
    )

    private val REF_NO_REGEX = Regex(
        """(?:Ref(?:erence)?\s*(?:No\.?|Id)?|Txn\s*ID|URN|UPI\s*Ref)\s*[:\.\-]?\s*([A-Za-z0-9]{6,16})""",
        RegexOption.IGNORE_CASE
    )

    private val AVAIL_BAL_REGEX = Regex(
        """(?:Avail(?:able)?\s*Bal(?:ance)?|Bal(?:ance)?)\s*(?:is|:)?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    fun parseMetadata(smsText: String): ExtractedSmsMetadata {
        val bankName = extractBankName(smsText)
        val accountLast4 = ACCOUNT_REGEX.find(smsText)?.groupValues?.get(1)
        val cardLast4 = CARD_REGEX.find(smsText)?.groupValues?.get(1)
        val upiId = UPI_ID_REGEX.find(smsText)?.groupValues?.get(1)
        val referenceNumber = REF_NO_REGEX.find(smsText)?.groupValues?.get(1)
        val availBalStr = AVAIL_BAL_REGEX.find(smsText)?.groupValues?.get(1)?.replace(",", "")
        val availableBalance = availBalStr?.toDoubleOrNull()

        return ExtractedSmsMetadata(
            bankName = bankName,
            accountLast4 = accountLast4,
            cardLast4 = cardLast4,
            upiId = upiId,
            referenceNumber = referenceNumber,
            availableBalance = availableBalance
        )
    }

    private fun extractBankName(text: String): String? {
        for ((bank, regex) in BANK_PATTERNS) {
            if (regex.containsMatchIn(text)) {
                return bank
            }
        }
        return null
    }
}
