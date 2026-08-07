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
    // NOTE: Almost every real credit SMS also contains "Available balance is…".
    // That alone must NOT classify the SMS as JUST_INFO when the account was credited.
    private val INFO_NOTICE = Regex("""(?i)\b(?:avail(?:able)?\s*bal(?:ance)?\s*is|bal(?:ance)?\s*in\s*your\s*a/c|mandate\s*registered|statement\s*generated|info:)\b""")

    // Explicit User Account Debit (user's own account moved money out)
    // NOTE: Do NOT match "credited to the beneficiary" — that is a post-NEFT confirmation, not a debit.
    private val USER_ACCOUNT_DEBITED = Regex("""(?i)\b(?:acct\s*XX\d+\s*debited|account\s*XX\d+\s*debited|acc\s*XX\d+\s*debited|debited\s+by|debited\s+for|debited\s+from|spent|paid|withdrawn|withdrawal|wdl|deducted|swiped|used\s+at|sent\s+to|auto[- ]debit|emi\s+deducted|top-up|topup|top\s+up|mandate|payment\s+of|using\s+apay|trf\s+to)\b""")

    /**
     * Explicit User Account Credit (user's own account received money).
     * Covers common Indian bank templates:
     * - "A/c *1737 is credited with Rs…"
     * - "a/c no. XXXX is credited by Rs…"
     * - "Account XX932 credited:Rs…"
     * - "your A/c X9642-credited by Rs…"
     * - "has been credited" / "is credited"
     */
    private val USER_ACCOUNT_CREDITED = Regex(
        """(?i)\b(?:
            (?:acct|a/?c|account|acc)(?:\s*no\.?)?\s*[*x]*\d+\s*[-:]?\s*(?:is\s+)?credited|
            (?:acct|a/?c|account|acc)\s*xx+\d+\s*(?:is\s+)?credited|
            is\s+credited\s+(?:with|by|for)|
            has\s+been\s+credited|
            credited\s*(?:[:\-]|with|by|for)|
            credited\s+to\s+(?:hdfc|icici|sbi|axis|kotak|canara|union|bank|your|a/?c)|
            credit\s+alert|
            transfer\s+from
        )""",
        RegexOption.COMMENTS
    )

    // HDFC-style inbound: "Received! INR 12,181.00 in HDFC Bank A/c xx0328" / "For IMPS -NAME-"
    private val RECEIVED_IN_ACCOUNT = Regex(
        """(?i)\b(?:
            received[!.,]?\s*(?:\r?\n|\s)+(?:inr|rs\.?|₹)|
            (?:inr|rs\.?|₹)\s*[\d,]+\.?\d*\s+in\s+[\w\s]+(?:bank\s+)?a/?c\s*xx?\d*|
            \bfor\s+(?:imps|neft|rtgs)\s*[-/]
        )""",
        RegexOption.COMMENTS
    )

    /** UPI debit payee line: "; MERCHANT credited." — not an income credit. */
    private val UPI_PAYEE_CREDITED = Regex("""(?i);\s*[A-Za-z].{0,40}\s+credited""")

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

        // 3. Own-account credit BEFORE info/balance short-circuit.
        // Almost every credit SMS includes "Available balance is…"; that used to force JUST_INFO
        // whenever the narrow credit regex missed the bank's template — wiping all credits on some devices.
        val isOwnAccountCredit = isOwnAccountCredit(body, lowerBody)
        if (isOwnAccountCredit) {
            val subType = when {
                lowerBody.contains("upi") || lowerBody.contains("vpa") -> SmsTransactionSubType.UPI_PAYMENT
                lowerBody.contains("imps") || lowerBody.contains("neft") || lowerBody.contains("rtgs") ||
                    lowerBody.contains("transfer from") || lowerBody.contains("rrn") ||
                    RECEIVED_IN_ACCOUNT.containsMatchIn(body) ->
                    SmsTransactionSubType.TRANSFER_IN
                else -> SmsTransactionSubType.CREDIT
            }
            return DebitCreditResult(TransactionType.INCOME, subType)
        }

        // 4. Info / non-movement notices (balance text without own-account debit/credit)
        if (INFO_NOTICE.containsMatchIn(body) &&
            !USER_ACCOUNT_DEBITED.containsMatchIn(body) &&
            !lowerBody.contains("debited") &&
            !lowerBody.contains("credited") &&
            !RECEIVED_IN_ACCOUNT.containsMatchIn(body)
        ) {
            return DebitCreditResult(TransactionType.JUST_INFO, SmsTransactionSubType.INFO_ALERT)
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

    private fun isOwnAccountCredit(body: String, lowerBody: String): Boolean {
        if (lowerBody.contains("beneficiary")) return false
        if (UPI_PAYEE_CREDITED.containsMatchIn(body)) return false
        if (USER_ACCOUNT_CREDITED.containsMatchIn(body)) return true
        if (RECEIVED_IN_ACCOUNT.containsMatchIn(body)) return true
        if (lowerBody.contains("credited by") || lowerBody.contains("credited with") ||
            lowerBody.contains("is credited") || lowerBody.contains("has been credited")
        ) {
            return !lowerBody.contains("debited")
        }
        // Generic "credited" on own-account alerts (not UPI payee / not card-bill)
        return lowerBody.contains("credited") &&
            !lowerBody.contains("debited") &&
            !lowerBody.contains("credited to your card") &&
            !lowerBody.contains("credited to your credit card")
    }
}
