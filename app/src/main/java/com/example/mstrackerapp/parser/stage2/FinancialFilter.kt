package com.example.mstrackerapp.parser.stage2

object FinancialFilter {

    private val TRANSACTION_VERBS = Regex(
        """(?i)\b(?:debited|credited|withdrawn|deposited|paid|received|transferred|spent|purchase[d]?|sent|deducted)\b"""
    )
    private val AMOUNT_PATTERN = Regex(
        """(?:INR|Rs\.?|₹)?\s*[\d,]+(?:\.\d+)?""", RegexOption.IGNORE_CASE
    )
    // Senders that are definitely financial institutions
    private val FINANCIAL_SENDER_PATTERNS = Regex(
        """(?i)(?:HDFC|ICICI|ICICIT|CBSSBI|SBI|AXIS|KOTAK|YESBNK|YESBK|PNB|BOB|CANBNK|IDFC|RBL|INDUS|INDUSB|CITI|CITIBK|FEDERAL|UNION|UNIONB|CENTRAL|PAYTM|PHONEPE|PHONPE|PHSTPA|GPAY|CRED|AMAZON|MOBIKW|NPSTXN)"""
    )

    fun passes(sender: String, body: String): Boolean {
        val hasFinancialSender = FINANCIAL_SENDER_PATTERNS.containsMatchIn(sender)
        val hasAmount = AMOUNT_PATTERN.containsMatchIn(body)
        val hasTransactionVerb = TRANSACTION_VERBS.containsMatchIn(body)
        return hasFinancialSender && hasAmount && hasTransactionVerb
    }
}
