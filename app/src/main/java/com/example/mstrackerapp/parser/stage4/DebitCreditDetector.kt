package com.example.mstrackerapp.parser.stage4

import com.example.mstrackerapp.domain.models.SmsTransactionSubType
import com.example.mstrackerapp.domain.models.TransactionType

data class DebitCreditResult(
    val transactionType: TransactionType,
    val subType: SmsTransactionSubType
)

object DebitCreditDetector {

    // Credit Card Bill Payment acknowledgment (e.g., "Online Payment of Rs.14504 vide Ref# ... was credited to your card")
    private val CARD_BILL_PAYMENT = Regex("""(?i)\b(?:payment\s+of\s+rs\.?\s*[\d,]+.*credited\s+to\s+(?:your\s+)?card|payment\s+received\s+towards\s+(?:your\s+)?credit\s+card|credited\s+to\s+your\s+(?:credit\s+)?card|online\s+payment.*credited\s+to.*card|payment.*credited\s+to\s+your\s+card)\b""")

    // Income / Credit Patterns
    private val SALARY = Regex("""(?i)\b(?:salary|payroll|sal\s*cr|stipend|wages|salary\s*credited)\b""")
    private val REFUND = Regex("""(?i)\b(?:refund(?:ed)?|reversal|reversed|cashback\s+(?:of|rs|inr|₹))\b""")
    private val CASH_DEPOSIT = Regex("""(?i)\b(?:cash\s*deposit(?:ed)?|deposited\s*at|deposited\s*in)\b""")
    private val INTEREST_CR = Regex("""(?i)\b(?:interest\s*cr(?:edited)?|int(?:erest)?\s*paid\s*to\s*you)\b""")

    // Informational Alerts (Balance checks, info notices, mandate setups)
    private val INFO_NOTICE = Regex("""(?i)\b(?:avail(?:able)?\s*bal(?:ance)?\s*is|bal(?:ance)?\s*in\s*your\s*a/c|mandate\s*registered|statement\s*generated|info:)\b""")

    // Explicit User Account Debit (user's own account moved money out)
    // NOTE: Do NOT match "credited to the beneficiary" — that is a post-NEFT confirmation, not a debit.
    private val USER_ACCOUNT_DEBITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*debited|account\s*XX\d+\s*debited|acc\s*XX\d+\s*debited|debited\s+by|debited\s+for|debited\s+from|spent|paid|withdrawn|withdrawal|wdl|deducted|swiped|used\s+at|sent\s+to|auto[- ]debit|emi\s+deducted|top-up|topup|top\s+up|mandate|payment\s+of|using\s+apay|trf\s+to)\b""")
    
    // Explicit User Account Credit (user's own account received money)
    private val USER_ACCOUNT_CREDITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*is\s+credited|account\s*XX\d+\s*credited|acc\s*XX\d+\s*credited|credited\s+by|credited\s+to\s+(?:hdfc|icici|sbi|axis|kotak|canara|union|bank|your)\s*a/c|credited\s+with|credit\s+alert|transfer\s+from)\b""")

    // HDFC-style inbound: "Received! INR 12,181.00 in HDFC Bank A/c xx0328" / "For IMPS -NAME-"
    private val RECEIVED_IN_ACCOUNT = Regex(
        """(?i)\b(?:
            received[!.,]?\s*(?:\r?\n|\s)+(?:inr|rs\.?|₹)|
            (?:inr|rs\.?|₹)\s*[\d,]+\.?\d*\s+in\s+[\w\s]+(?:bank\s+)?a/?c\s*xx?\d*|
            \bfor\s+(?:imps|neft|rtgs)\s*[-/]
        )""",
        RegexOption.COMMENTS
    )

    private val ATM = Regex("""(?i)\b(?:atm\s*(?:wdl|cash|debit|withdrawal)|cash\s*(?:withdrawal|withdraw)|withdrawn\s*at|atm\s+wdl)\b""")
    private val EMI = Regex("""(?i)\b(?:emi|auto[- ]debit\s*emi|loan\s*emi|equated\s*monthly)\b""")
    private val CARD_PURCHASE = Regex("""(?i)\b(?:pos|swipe|card\s*(?:used|swiped|purchase)|merchant)\b""")
    private val SUBSCRIPTION = Regex("""(?i)\b(?:subscription|auto[- ]pay|recurring|standing\s*instruction|mandate)\b""")
    private val INTEREST_DR = Regex("""(?i)\b(?:interest\s*(?:debited|charged)|int(?:erest)?\s*deducted)\b""")

    fun detect(body: String): DebitCreditResult {
        val lowerBody = body.lowercase()

        // 1. Credit Card Bill Payment Receipt (e.g. "Online Payment of Rs.14504 was credited to your card") -> EXPENSE
        if (CARD_BILL_PAYMENT.containsMatchIn(body)) {
            return DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.CARD_BILL_PAYMENT)
        }

        // 2. Specific Income Types
        if (SALARY.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.SALARY)
        if (REFUND.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.REFUND)
        if (CASH_DEPOSIT.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.CASH_DEPOSIT)
        if (INTEREST_CR.containsMatchIn(body)) return DebitCreditResult(TransactionType.INCOME, SmsTransactionSubType.INTEREST_CREDIT)

        // 3. Info / non-movement notices (balance text without own-account debit/credit)
        if (INFO_NOTICE.containsMatchIn(body) &&
            !USER_ACCOUNT_DEBITED.containsMatchIn(body) &&
            !USER_ACCOUNT_CREDITED.containsMatchIn(body) &&
            !lowerBody.contains("debited")
        ) {
            return DebitCreditResult(TransactionType.JUST_INFO, SmsTransactionSubType.INFO_ALERT)
        }

        // 4. Explicit credit/income on user's own account
        // Avoid treating "X credited" (UPI payee) or "credited to the beneficiary" as income.
        val isOwnAccountCredit = USER_ACCOUNT_CREDITED.containsMatchIn(body) ||
            RECEIVED_IN_ACCOUNT.containsMatchIn(body) ||
            lowerBody.contains("credited by") ||
            (lowerBody.contains("credited") &&
                !lowerBody.contains("debited") &&
                !lowerBody.contains("card") &&
                !lowerBody.contains("beneficiary") &&
                !Regex("""(?i);\s*[A-Za-z].{0,40}\s+credited""").containsMatchIn(body))
        if (isOwnAccountCredit) {
            val subType = when {
                lowerBody.contains("upi") || lowerBody.contains("vpa") -> SmsTransactionSubType.UPI_PAYMENT
                lowerBody.contains("imps") || lowerBody.contains("neft") || lowerBody.contains("rtgs") ||
                    lowerBody.contains("transfer from") || RECEIVED_IN_ACCOUNT.containsMatchIn(body) ->
                    SmsTransactionSubType.TRANSFER_IN
                else -> SmsTransactionSubType.CREDIT
            }
            return DebitCreditResult(TransactionType.INCOME, subType)
        }

        // 5. User's account debited / top-up / mandate / paid
        val isDebited = USER_ACCOUNT_DEBITED.containsMatchIn(body) ||
                lowerBody.contains("debited") ||
                lowerBody.contains("top-up") ||
                lowerBody.contains("topup") ||
                lowerBody.contains("paid") ||
                lowerBody.contains("payment of")

        // EMI keyword alone (without due reminder — those are filtered earlier) → expense
        return when {
            ATM.containsMatchIn(body) -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.ATM)
            EMI.containsMatchIn(body) && isDebited -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.EMI)
            CARD_PURCHASE.containsMatchIn(body) -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.CARD_PURCHASE)
            SUBSCRIPTION.containsMatchIn(body) -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.SUBSCRIPTION)
            INTEREST_DR.containsMatchIn(body) -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.INTEREST_DEBIT)
            isDebited -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.UPI_PAYMENT)
            else -> DebitCreditResult(TransactionType.EXPENSE, SmsTransactionSubType.DEBIT)
        }
    }
}
