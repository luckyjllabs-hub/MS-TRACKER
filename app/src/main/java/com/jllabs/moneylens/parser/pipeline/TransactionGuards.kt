package com.jllabs.moneylens.parser.pipeline

object FinancialFilter {
    private val financialContext = Regex("""\b(hdfc|icici|sbi|axis|kotak|upi|debit card|credit card|neft|rtgs|imps|atm|pos|wallet|bank)\b""", RegexOption.IGNORE_CASE)
    private val monetaryDirection = Regex("""\b(debited|auto debit|credited|withdrawn|received|spent|refund|cash deposit|card purchase|upi payment)\b""", RegexOption.IGNORE_CASE)
    fun isFinancialTransaction(body: String, type: SmsMessageType) = type == SmsMessageType.FINANCIAL_TRANSACTION && monetaryDirection.containsMatchIn(body)
}

object SuccessDetector {
    private val rejection = Regex("""\b(due|reminder|pending|failed|reversed?|reversal|cancelled|declined|attempted|scheduled|initiated|could not process|offer|statement ready)\b""", RegexOption.IGNORE_CASE)
    private val completion = Regex("""\b(debited|auto debit|credited|withdrawn|received|spent|purchase|refund|cash deposit|cash withdrawal|payment successful)\b""", RegexOption.IGNORE_CASE)
    fun isCompleted(body: String) = !rejection.containsMatchIn(body) && completion.containsMatchIn(body)
}

enum class DetectedTransactionType { DEBIT, CREDIT, TRANSFER, REFUND, SALARY, INTEREST, CASH_DEPOSIT, CASH_WITHDRAWAL, ATM, CARD_PURCHASE, UPI_PAYMENT, SUBSCRIPTION, EMI, LOAN, UNKNOWN }

object DebitCreditDetector {
    private val credit = Regex("""\b(credited|salary|refund|interest credited|cash deposit|received)\b""", RegexOption.IGNORE_CASE)
    private val debit = Regex("""\b(debited|spent|withdrawn|cash withdrawal|atm|pos|card purchase|upi payment|emi deducted|subscription)\b""", RegexOption.IGNORE_CASE)
    fun detect(body: String): DetectedTransactionType = when {
        Regex("""\bsalary|payroll\b""", RegexOption.IGNORE_CASE).containsMatchIn(body) -> DetectedTransactionType.SALARY
        Regex("""\brefund\b""", RegexOption.IGNORE_CASE).containsMatchIn(body) -> DetectedTransactionType.REFUND
        Regex("""\binterest credited\b""", RegexOption.IGNORE_CASE).containsMatchIn(body) -> DetectedTransactionType.INTEREST
        Regex("""\bcash withdrawal|atm withdrawal\b""", RegexOption.IGNORE_CASE).containsMatchIn(body) -> DetectedTransactionType.CASH_WITHDRAWAL
        Regex("""\bupi[: ]\d+\b""", RegexOption.IGNORE_CASE).containsMatchIn(body) && debit.containsMatchIn(body) -> DetectedTransactionType.UPI_PAYMENT
        credit.containsMatchIn(body) && !debit.containsMatchIn(body) -> DetectedTransactionType.CREDIT
        debit.containsMatchIn(body) && !credit.containsMatchIn(body) -> DetectedTransactionType.DEBIT
        else -> DetectedTransactionType.UNKNOWN
    }
}
