package com.example.mstrackerapp.parser.stage3

/**
 * Detects SMS that look financial (amount + verbs) but are NOT actual
 * debit/credit movements on the user's account.
 *
 * Examples from real inbox:
 * - EMI / premium / loan "is due" reminders
 * - EPFO/PF contribution & passbook balance notices
 * - NEFT/RTGS/IMPS "credited to the beneficiary" confirmations
 *   (the user's debit SMS already recorded the spend)
 * - Pure balance / statement / due-date alerts
 */
object NonTransactionAlertFilter {

    private val DUE_REMINDER = Regex(
        """(?i)\b(?:
            (?:emi|loan\s*emi|premium|policy)\b.{0,80}\bis\s+due\b|
            \bis\s+due\b.{0,40}\b(?:emi|premium|loan)\b|
            (?:payment|bill|minimum|amount|total)\s+due\b|
            \bdue\s+(?:date|on|by|shortly)\b|
            maintain\s+adequate\s+balance|
            will\s+be\s+deducted\b|
            avoid\s+(?:lien|bounce|penal)|
            statement\s+is\s+sent.{0,80}\bis\s+due\b
        )""",
        RegexOption.COMMENTS
    )

    private val PF_EPFO = Regex(
        """(?i)\b(?:
            epfo|epassbook|passbook\s+balance|
            contribution\s+of\s+(?:rs\.?|inr|₹).{0,40}has\s+been\s+received|
            pf\s+contribution|
            provident\s+fund
        )\b""",
        RegexOption.COMMENTS
    )

    /** Third-party receipt confirmation after NEFT/RTGS/IMPS — not a new txn. */
    private val BENEFICIARY_CREDIT_CONFIRMATION = Regex(
        """(?i)\b(?:
            credited\s+to\s+the\s+beneficiary(?:\s+account)?|
            credited\s+to\s+(?:the\s+)?beneficiary\s+a/?c|
            has\s+been\s+credited\s+to\s+(?:the\s+)?beneficiary|
            (?:neft|rtgs|imps)\s+transaction.{0,120}credited\s+to
        )\b""",
        RegexOption.COMMENTS
    )

    /** Balance / info-only alerts with no account movement verb for the user. */
    private val BALANCE_ONLY = Regex(
        """(?i)\b(?:
            your\s+(?:a/?c|acct|account|acc).{0,40}balance\s+is|
            (?:available|avl|avbl)\s+bal(?:ance)?\s*(?:is|:)?\s*(?:rs\.?|inr|₹)|
            bal(?:ance)?\s+enquiry|
            balance\s+alert
        )\b""",
        RegexOption.COMMENTS
    )

    private val ACCOUNT_MOVEMENT = Regex(
        """(?i)\b(?:
            (?:acct|a/?c|account|acc)\s*xx?\d+\s*(?:is\s+)?(?:debited|credited)|
            debited\s+(?:for|by|from|rs|inr|₹)|
            credited\s+(?:with|by|to\s+(?:your|hdfc|icici|sbi|axis))|
            (?:inr|rs\.?|₹)\s*[\d,]+\.?\d*\s+(?:debited|credited|deposited|spent|withdrawn)|
            (?:inr|rs\.?|₹)\s*[\d,]+\.?\d*\s+in\s+[\w\s]+(?:bank\s+)?a/?c|
            \breceived[!.,]?\b|
            credit\s+alert|
            deposited\s+in|
            spent|withdrawn|cash\s+withdrawal|
            paid\s+to|sent\s+to|trf\s+to|
            for\s+(?:imps|neft|rtgs)\b|
            interest\s+paid
        )\b""",
        RegexOption.COMMENTS
    )

    fun isNonTransactionAlert(body: String): Boolean {
        if (body.isBlank()) return false
        if (DUE_REMINDER.containsMatchIn(body)) return true
        if (PF_EPFO.containsMatchIn(body)) return true
        if (BENEFICIARY_CREDIT_CONFIRMATION.containsMatchIn(body)) return true
        // Pure balance SMS: has balance phrasing but no debit/credit of user's account
        if (BALANCE_ONLY.containsMatchIn(body) && !ACCOUNT_MOVEMENT.containsMatchIn(body)) return true
        return false
    }
}
