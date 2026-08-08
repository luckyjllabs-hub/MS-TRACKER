package com.jllabs.moneylens.parser.stage5

object BankParser {

    // Maps sender code substring -> Bank name
    private val SENDER_TO_BANK = linkedMapOf(
        "HDFCBK" to "HDFC Bank", "HDFCBN" to "HDFC Bank", "HDFCCC" to "HDFC Bank",
        "ICICIB" to "ICICI Bank", "ICICIT" to "ICICI Bank", "ICICIO" to "ICICI Bank",
        "ICICIP" to "ICICI Pru", "ICICI" to "ICICI Bank",
        "SBIINB" to "SBI", "SBIPSG" to "SBI", "SBIUPI" to "SBI", "CBSSBI" to "SBI", "SBIBNK" to "SBI",
        "ATMSBI" to "SBI", "SBI" to "SBI",
        "AXISBK" to "Axis Bank", "AXISMR" to "Axis Bank", "AXIS" to "Axis Bank",
        "KOTAKB" to "Kotak Bank", "KOTAKA" to "Kotak Bank", "KOTAK" to "Kotak Bank",
        "YESBK" to "YES Bank", "YESBNK" to "YES Bank",
        "PNBSMS" to "PNB", "PNB" to "PNB",
        "BOBTXN" to "Bank of Baroda", "BOB" to "Bank of Baroda",
        "CANBNK" to "Canara Bank", "CNRBNK" to "Canara Bank", "CNRB" to "Canara Bank",
        "IDFCBK" to "IDFC FIRST Bank",
        "RBLBNK" to "RBL Bank",
        "INDBNK" to "IndusInd Bank", "INDUSB" to "IndusInd Bank", "INDUSA" to "IndusInd Bank",
        "IDIBNK" to "Indian Bank", "INDIAN" to "Indian Bank", "IDIB" to "Indian Bank",
        "FEDBNK" to "Federal Bank",
        "CENTBK" to "Central Bank",
        "CITIBK" to "Citi Bank", "CITI" to "Citi Bank",
        "UNIONB" to "Union Bank",
        "PAYTMB" to "Paytm", "PAYTMP" to "Paytm",
        "PHONPE" to "PhonePe", "PHONEPE" to "PhonePe", "PHSTPA" to "PhonePe",
        "GPAYID" to "Google Pay",
        "CREDCL" to "CRED",
        "AMZPAY" to "Amazon Pay",
        "MOBIKW" to "MobiKwik",
        "EPFOHO" to "EPFO", "EPFO" to "EPFO", "EPFIND" to "EPFO"
    )

    // Body-based fallback patterns
    private val BODY_BANK_PATTERNS = linkedMapOf(
        "HDFC Bank" to Regex("""HDFC\s*Bank""", RegexOption.IGNORE_CASE),
        "ICICI Pru" to Regex("""ICICI\s*Pru|ICICIPru|ICICI\s*Prudential""", RegexOption.IGNORE_CASE),
        "ICICI Bank" to Regex("""ICICI\s*Bank""", RegexOption.IGNORE_CASE),
        "SBI" to Regex("""State\s*Bank\s*of\s*India|\bSBI\b""", RegexOption.IGNORE_CASE),
        "Axis Bank" to Regex("""Axis\s*Bank""", RegexOption.IGNORE_CASE),
        "Kotak Bank" to Regex("""Kotak\s*(?:Mahindra)?\s*Bank""", RegexOption.IGNORE_CASE),
        "YES Bank" to Regex("""YES\s*Bank""", RegexOption.IGNORE_CASE),
        "Canara Bank" to Regex("""Canara\s*Bank|\bCNRB\b""", RegexOption.IGNORE_CASE),
        "IDFC FIRST Bank" to Regex("""IDFC\s*(?:FIRST)?\s*Bank""", RegexOption.IGNORE_CASE),
        "Indian Bank" to Regex("""\bIndian\s*Bank\b""", RegexOption.IGNORE_CASE),
        "IndusInd Bank" to Regex("""IndusInd""", RegexOption.IGNORE_CASE),
        "EPFO" to Regex("""\bEPFO\b|epassbook|provident\s+fund|passbook\s+balance""", RegexOption.IGNORE_CASE),
        "Paytm" to Regex("""Paytm""", RegexOption.IGNORE_CASE),
        "PhonePe" to Regex("""PhonePe""", RegexOption.IGNORE_CASE),
        "Google Pay" to Regex("""Google\s*Pay|GPay""", RegexOption.IGNORE_CASE)
    )

    fun extractBank(sender: String, body: String): String {
        val senderUpper = sender.uppercase()
        for ((key, bankName) in SENDER_TO_BANK) {
            if (senderUpper.contains(key)) return bankName
        }
        for ((bankName, pattern) in BODY_BANK_PATTERNS) {
            if (pattern.containsMatchIn(body)) return bankName
        }
        // Return cleaned sender as fallback
        return sender.replace("-", " ").trim().takeIf { it.isNotBlank() } ?: "Unknown Bank"
    }
}
