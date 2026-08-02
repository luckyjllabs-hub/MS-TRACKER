package com.example.mstrackerapp.parser.stage4

import com.example.mstrackerapp.domain.models.SmsTransactionSubType
import com.example.mstrackerapp.domain.models.TransactionType

data class DebitCreditResult(
    val transactionType: TransactionType,
    val subType: SmsTransactionSubType
)

object DebitCreditDetector {

    // Income / Credit Patterns
    private val SALARY = Regex("""(?i)\b(?:salary|payroll|sal\s*cr|stipend|wages|salary\s*credited)\b""")
    private val REFUND = Regex("""(?i)\b(?:refund(?:ed)?|reversal|reversed|cashback\s+(?:of|rs|inr|₹))\b""")
    private val CASH_DEPOSIT = Regex("""(?i)\b(?:cash\s*deposit(?:ed)?|deposited\s*at|deposited\s*in)\b""")
    private val INTEREST_CR = Regex("""(?i)\b(?:interest\s*cr(?:edited)?|int(?:erest)?\s*paid\s*to\s*you)\b""")

    // Explicit User Account Debit (User's account debited or sent out to recipient/beneficiary)
    private val USER_ACCOUNT_DEBITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*debited|account\s*XX\d+\s*debited|debited\s+for|debited\s+by|debited\s+from|spent|paid|withdrawn|withdrawal|wdl|deducted|swiped|used\s+at|sent\s+to|auto[- ]debit|emi\s+deducted|credited\s+to\s+(?:the\s+)?beneficiary)\b""")
    
    // Explicit User Account Credit (User's account credited)
    private val USER_ACCOUNT_CREDITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*is\s+credited|account\s*XX\d+\s*credited:|credited\s+to\s+(?:hdfc|icici|sbi|axis|kotak|canara|union|bank|your)?\s*a/c|credited\s+with|credit\s+alert)\b""")

    private val ATM = Regex("""(?i)\b(?:atm\s*(?:wdl|cash|debit|withdrawal)|cash\s*(?:withdrawal|withdraw)|withdrawn\s*at|atm\s+wdl)\b""")
    private val EMI = Regex("""(?i)\b(?:emi|auto[- ]debit\s*emi|loan\s*emi|equated\s*monthly)\b""")
    private val CARD_PURCHASE = Regex("""(?i)\b(?:pos|swipe|card\s*(?:used|swiped|purchase)|merchant)\b""")
    private val SUBSCRIPTION = Regex("""(?i)\b(?:subscription|auto[- ]pay|recurring|standing\s*instruction|mandate\s*executed)\b""")
    private val INTEREST_DR = Regex("""(?i)\b(?:interest\s*(?:debited|charged)|int(?:erest)?\s*deducted)\b""")

    fun detect(body: String): DebitCreditResult {
        val lowerBody = body.lowercase()

        // 1. Specific Income Types
        if (SALARY.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.SALARY)
        if (REFUND.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.REFUND)
        if (CASH_DEPOSIT.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.CASH_DEPOSIT)
        if (INTEREST_CR.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.INTEREST_CREDIT)

        // 2. Check if User's Account is Debited (or Beneficiary Credited)
        val isDebited = USER_ACCOUNT_DEBITED.containsMatchIn(body) ||
                lowerBody.contains("debited") ||
                lowerBody.contains("spent") ||
                lowerBody.contains("paid") ||
                lowerBody.contains("withdrawn") ||
                lowerBody.contains("wdl") ||
                lowerBody.contains("beneficiary account")

        if (isDebited) {
            val subType = when {
                ATM.containsMatchIn(body) -> SmsTransactionSubType.ATM
                EMI.containsMatchIn(body) -> SmsTransactionSubType.EMI
                CARD_PURCHASE.containsMatchIn(body) -> SmsTransactionSubType.CARD_PURCHASE
                SUBSCRIPTION.containsMatchIn(body) -> SmsTransactionSubType.SUBSCRIPTION
                INTEREST_DR.containsMatchIn(body) -> SmsTransactionSubType.INTEREST_DEBIT
                lowerBody.contains("upi") || lowerBody.contains("vpa") -> SmsTransactionSubType.UPI_PAYMENT
                else -> SmsTransactionSubType.DEBIT
            }
            return DebitCreditResult(TransactionType.EXPENSE, subType)
        }

        // 3. Check if User's Account is Credited (and not debited)
        if (USER_ACCOUNT_CREDITED.containsMatchIn(body) || (lowerBody.contains("credited") && !lowerBody.contains("debited"))) {
            val subType = if (lowerBody.contains("upi") || lowerBody.contains("vpa")) SmsTransactionSubType.UPI_PAYMENT else SmsTransactionSubType.CREDIT
            return DebitCreditResult(TransactionType.INCOME, subType)
        }

        // Default: Treat as Expense / Debit
        return DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.DEBIT)
    }
}
