package com.jllabs.moneylens.parser.stage3

object SuccessDetector {

    private val FAILURE_PATTERNS = Regex(
        """(?i)\b(?:
            otp|one[- ]time|do not share|don[''`]t share|
            payment\s+due|minimum\s+due|bill\s+due|amount\s+due|due\s+date|
            failed|declined|unsuccessful|could\s+not|not\s+successful|unable\s+to\s+process|
            reversed|cancell?ed|pending|on\s+hold|
            reward\s+points?|cashback\s+offer|earn\s+(?:\d+|reward)|
            loan\s+offer|pre[- ]approved|credit\s+offer|
            kyc|know\s+your\s+customer|
            password\s+(?:reset|changed)|new\s+login|app\s+login|device\s+registered|
            statement\s+ready|monthly\s+statement|account\s+statement|
            card\s+(?:generated|blocked|delivered|activated)|
            pin\s+(?:generated|changed|set)|
            congratulations(?!.*credited)|
            not\s+deducted|refund\s+initiated|refund\s+pending|
            scheduled|reminder
        )\b""", RegexOption.COMMENTS
    )

    /** Dispute / block-card footers must not kill a completed debit/credit SMS. */
    private val COMPLETED_MOVEMENT = Regex(
        """(?i)\b(?:
            (?:acct|a/?c|account|acc).{0,20}(?:debited|credited)|
            debited\s+(?:rs\.?|inr|₹)|
            credited\s*[:\-]?\s*(?:rs\.?|inr|₹)|
            (?:rs\.?|inr|₹)\s*[\d,]+\.?\d*\s+(?:debited|credited)
        )""",
        RegexOption.COMMENTS
    )

    /** Returns true if the message represents a SUCCESSFUL completed transaction */
    fun isSuccessful(body: String): Boolean {
        if (NonTransactionAlertFilter.isNonTransactionAlert(body)) return false
        // Completed debit/credit SMS often include "SMS BLOCK … to dispute" footers — keep them.
        if (COMPLETED_MOVEMENT.containsMatchIn(body)) return true
        return !FAILURE_PATTERNS.containsMatchIn(body)
    }
}
