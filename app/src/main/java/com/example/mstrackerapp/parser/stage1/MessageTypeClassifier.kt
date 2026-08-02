package com.example.mstrackerapp.parser.stage1

import com.example.mstrackerapp.domain.models.MessageType

object MessageTypeClassifier {

    // Known financial institution sender IDs (extracted from real Indian SMS headers like AD-ICICIT-S, VM-HDFCBK-S, AX-ICICIT-S)
    private val BANK_SENDERS = setOf(
        "HDFCBK", "HDFCBN", "HDFCCC", "HDFCLI", "HDFC",
        "ICICIB", "ICICIT", "ICICIO", "ICICIP", "ICIOTP", "ICIBNK", "ICICI",
        "SBIINB", "SBIPSG", "SBIUPI", "CBSSBI", "SBIBNK", "SBI",
        "AXISBK", "AXISMR", "AXIS",
        "KOTAKB", "KOTAKA", "KOTAK",
        "YESBK", "YESBNK",
        "PNBSMS", "PNB",
        "BOBTXN", "BOB",
        "CANBNK", "CANARA",
        "IDFCBK", "IDFC",
        "RBLBNK", "RBL",
        "INDBNK", "INDUSB", "INDUSA", "INDUSO",
        "FEDBNK", "FEDERAL",
        "UBIKOB", "UNIONB",
        "CENTBK", "CITIBK", "CITI", "SCBANK",
        "PAYTMB", "PAYTMP", "IPAYTM",
        "PHONPE", "PHSTPA", "GPAYID", "CREDCL", "MOBIKW", "AMZPAY", "JUSPAY", "ATMMSG", "NPSTXN"
    )

    private val FINANCIAL_KEYWORDS = setOf(
        "debited", "credited", "withdrawn", "deposited", "paid", "received", "transferred",
        "spent", "purchase", "payment", "transaction", "txn", "upi", "neft", "rtgs", "imps",
        "atm", "pos", "emi", "mandate", "auto-debit"
    )

    private val OTP_PATTERN = Regex(
        """(?i)\b(?:otp|one[- ]time|verification code|do not share|don[''\`]t share|passcode|2fa)\b"""
    )
    private val OTP_DIGIT_PATTERN = Regex("""(?i)(?:otp|code|pin|password)\s*(?:is|:)?\s*[0-9]{4,8}\b""")
    private val PROMO_PATTERN = Regex(
        """(?i)\b(?:offer|discount|cashback offer|click here|apply now|win|lucky draw|deal|limited time|sale|hurry|don[''\`]t miss|earn reward|exclusive|bumper)\b"""
    )
    private val DELIVERY_SENDERS = setOf("FEDEXP", "BLUDRK", "BLUDRT", "DTDCCO", "EKARTL", "DELHVR", "DLHVRY", "XPRSBD")
    private val SHOPPING_SENDERS = setOf("AMAZON", "FLIPKR", "FLPKRT", "MYNTRA", "MEESHO", "SNAPDL", "NYKAA", "AJIO", "LENSKT", "LNKART", "LNSKRT")
    private val RECHARGE_PATTERN = Regex("""(?i)\b(?:recharge|talktime|data pack|validity|balance added|mobile pack)\b""")
    private val BILL_PATTERN = Regex("""(?i)\b(?:bill due|payment due|minimum due|due date|amount due|outstanding|emi.{0,40}is due|is due.{0,40}emi|premium.{0,40}due|will be deducted)\b""")
    private val LOAN_PATTERN = Regex("""(?i)\b(?:loan offer|pre-approved|personal loan|home loan offer|credit offer|approved loan)\b""")
    private val KYC_PATTERN = Regex("""(?i)\b(?:kyc|know your customer|complete your kyc|update kyc|kyc pending|video kyc)\b""")
    private val SECURITY_PATTERN = Regex("""(?i)\b(?:login attempt|new login|password (?:changed|reset)|device registered|suspicious|security alert)\b""")
    private val TELECOM_SENDERS = setOf("AIRTEL", "JIO", "VODAFN", "BSNLTX", "IDEACL", "AIRTLM", "JIOMSG", "VICARE")
    private val PERSONAL_SENDER = Regex("""^[+]?[0-9]{10,13}$""")
    private val EPFO_SENDERS = setOf("EPFOHO", "EPFO", "EPFIND", "UANEPF")

    fun classify(sender: String, body: String): MessageType {
        val senderUpper = sender.uppercase().trim()
        val bodyLower = body.lowercase()

        // 1. OTP — highest priority
        if (OTP_PATTERN.containsMatchIn(body) || OTP_DIGIT_PATTERN.containsMatchIn(body)) return MessageType.OTP

        // 2. KYC
        if (KYC_PATTERN.containsMatchIn(body)) return MessageType.KYC

        // 3. Security Alert
        if (SECURITY_PATTERN.containsMatchIn(body)) return MessageType.SECURITY_ALERT

        // 4. Non-txn alerts BEFORE financial (EMI due, PF, NEFT beneficiary confirm, etc.)
        if (com.example.mstrackerapp.parser.stage3.NonTransactionAlertFilter.isNonTransactionAlert(body)) {
            return if (BILL_PATTERN.containsMatchIn(body)) MessageType.BILL_REMINDER else MessageType.BANK_ALERT
        }
        if (EPFO_SENDERS.any { senderUpper.contains(it) }) return MessageType.BANK_ALERT

        // 5. FINANCIAL_TRANSACTION: bank sender + amount + financial verb
        val isBankSender = BANK_SENDERS.any { senderUpper.contains(it) }
        val hasAmount = Regex("""(?:INR|Rs\.?|₹)\s*[\d,]+(?:\.\d+)?""", RegexOption.IGNORE_CASE).containsMatchIn(body) ||
                Regex("""[\d,]+(?:\.\d+)?\s*(?:INR|Rs\.?|₹)""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        val hasFinancialVerb = FINANCIAL_KEYWORDS.any { bodyLower.contains(it) }

        if ((isBankSender || hasAmount) && hasFinancialVerb) return MessageType.FINANCIAL_TRANSACTION

        // 6. BANK_ALERT (bank sender but no clear debit/credit)
        if (isBankSender && hasAmount) return MessageType.BANK_ALERT

        // 7. Delivery
        if (DELIVERY_SENDERS.any { senderUpper.contains(it) }) return MessageType.DELIVERY

        // 8. Shopping
        if (SHOPPING_SENDERS.any { senderUpper.contains(it) }) return MessageType.SHOPPING

        // 9. Recharge
        if (RECHARGE_PATTERN.containsMatchIn(body)) return MessageType.RECHARGE

        // 10. Bill Reminder
        if (BILL_PATTERN.containsMatchIn(body)) return MessageType.BILL_REMINDER

        // 11. Loan
        if (LOAN_PATTERN.containsMatchIn(body)) return MessageType.LOAN

        // 12. Promotional
        if (PROMO_PATTERN.containsMatchIn(body)) return MessageType.PROMOTIONAL

        // 13. Telecom
        if (TELECOM_SENDERS.any { senderUpper.contains(it) }) return MessageType.TELECOM

        // 14. Personal
        if (PERSONAL_SENDER.matches(senderUpper)) return MessageType.PERSONAL

        return MessageType.UNKNOWN
    }
}
