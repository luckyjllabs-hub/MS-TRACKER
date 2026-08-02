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
    private val USER_ACCOUNT_DEBITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*debited|account\s*XX\d+\s*debited|debited\s+by|debited\s+for|debited\s+from|spent|paid|withdrawn|withdrawal|wdl|deducted|swiped|used\s+at|sent\s+to|auto[- ]debit|emi\s+deducted|top-up|topup|top\s+up|mandate|payment\s+of|using\s+apay|trf\s+to|credited\s+to\s+(?:the\s+)?beneficiary)\b""")
    
    // Explicit User Account Credit (User's account credited)
    private val USER_ACCOUNT_CREDITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*is\s+credited|account\s*XX\d+\s*credited|credited\s+by|credited\s+to\s+(?:hdfc|icici|sbi|axis|kotak|canara|union|bank|your)?\s*a/c|credited\s+with|credit\s+alert|transfer\s+from)\b""")

    private val ATM = Regex("""(?i)\b(?:atm\s*(?:wdl|cash|debit|withdrawal)|cash\s*(?:withdrawal|withdraw)|withdrawn\s*at|atm\s+wdl)\b""")
    private val EMI = Regex("""(?i)\b(?:emi|auto[- ]debit\s*emi|loan\s*emi|equated\s*monthly)\b""")
    private val CARD_PURCHASE = Regex("""(?i)\b(?:pos|swipe|card\s*(?:used|swiped|purchase)|merchant)\b""")
    private val SUBSCRIPTION = Regex("""(?i)\b(?:subscription|auto[- ]pay|recurring|standing\s*instruction|mandate)\b""")
    private val INTEREST_DR = Regex("""(?i)\b(?:interest\s*(?:debited|charged)|int(?:erest)?\s*deducted)\b""")

    fun detect(body: String): DebitCreditResult {
        val lowerBody = body.lowercase()

        // 1. Specific Income Types
        if (SALARY.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.SALARY)
        if (REFUND.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.REFUND)
        if (CASH_DEPOSIT.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.CASH_DEPOSIT)
        if (INTEREST_CR.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.INTEREST_CREDIT)

        // 2. Check explicit credit/income (e.g., "credited by Rs.6000 transfer from CHANDRAMOULI SANCHI")
        if (USER_ACCOUNT_CREDITED.containsMatchIn(body) || (lowerBody.contains("credited by") || (lowerBody.contains("credited") && !lowerBody.contains("debited")))) {
            val subType = if (lowerBody.contains("upi") || lowerBody.contains("vpa") || lowerBody.contains("transfer from")) SmsTransactionSubType.UPI_PAYMENT else SmsTransactionSubType.CREDIT
            return DebitCreditResult(TransactionType.INCOME, subType)
        }

        // 3. Check if User's Account is Debited / Top-up / Mandate / Paid
        val isDebited = USER_ACCOUNT_DEBITED.containsMatchIn(body) ||
                lowerBody.contains("debited") ||
                lowerBody.contains("top-up") ||
                lowerBody.contains("topup") ||
                lowerBody.contains("mandate") ||
                lowerBody.contains("payment of") ||
                lowerBody.contains("spent") ||
                lowerBody.contains("paid") ||
                lowerBody.contains("withdrawn") ||
                lowerBody.contains("trf to")

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

        // Default: Treat as Expense / Debit
        return DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.DEBIT)
    }
}
