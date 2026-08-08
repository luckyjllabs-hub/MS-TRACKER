package com.jllabs.moneylens.parser.pipeline

enum class SmsMessageType { FINANCIAL_TRANSACTION, BANK_ALERT, OTP, PROMOTIONAL, SHOPPING, DELIVERY, RECHARGE, BILL_REMINDER, LOAN, KYC, SECURITY_ALERT, SERVICE_MESSAGE, TELECOM, PERSONAL, UNKNOWN }

/** Cheap first-pass classifier. It intentionally does not infer a transaction from an amount alone. */
object MessageTypeClassifier {
    private val rules = listOf(
        SmsMessageType.OTP to Regex("""\b(otp|one time password|verification code|passcode|do not share)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.BILL_REMINDER to Regex("""\b(payment due|minimum due|pending dues|reminder|pay .* immediately|scheduled payment)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.PROMOTIONAL to Regex("""\b(offer|sale|discount|cashback offer|congratulations|check now|apply now|reward points)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.KYC to Regex("""\b(kyc|pan update|aadhaar update)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.SECURITY_ALERT to Regex("""\b(password reset|login|pin generated|card blocked|card generated|card delivered|security alert)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.DELIVERY to Regex("""\b(delivery|courier|out for delivery|shipment|token number)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.RECHARGE to Regex("""\b(recharge|data pack|validity)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.LOAN to Regex("""\b(loan offer|pre-approved loan|emi offer)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.FINANCIAL_TRANSACTION to Regex("""\b(debited|credited|spent|paid|refund|auto debit|cash withdrawal|atm withdrawal|pos (?:txn|purchase)|upi[: ]|imps|neft|rtgs|card purchase)\b""", RegexOption.IGNORE_CASE),
        SmsMessageType.BANK_ALERT to Regex("""\b(bank|account|a/c|acct|statement)\b""", RegexOption.IGNORE_CASE)
    )

    fun classify(sender: String, body: String): SmsMessageType = rules.firstOrNull { it.second.containsMatchIn(body) }?.first ?: SmsMessageType.UNKNOWN
}
