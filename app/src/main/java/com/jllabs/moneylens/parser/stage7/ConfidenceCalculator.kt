package com.jllabs.moneylens.parser.stage7

data class ConfidenceResult(
    val score: Int,           // 0-100
    val label: String,        // "100% Verified", "High Confidence", etc.
    val goToQueue: Boolean    // true = review queue; false = auto-accept to ledger
)

object ConfidenceCalculator {

    fun calculate(
        isKnownBankSender: Boolean,
        amountFound: Boolean,
        isMerchantKnown: Boolean,
        hasReferenceNumber: Boolean,
        hasBalance: Boolean,
        isTransactionTypeClear: Boolean,
        hasUpiId: Boolean,
        categorySource: String   // "USER_LEARNED", "MERCHANT_DICT", "KEYWORD", "SUBTYPE", "UNKNOWN"
    ): ConfidenceResult {
        var score = 0
        if (isKnownBankSender) score += 30
        if (amountFound) score += 25
        if (isMerchantKnown) score += 15
        if (hasReferenceNumber) score += 10
        if (hasBalance) score += 5
        if (isTransactionTypeClear) score += 10
        if (hasUpiId) score += 5
        when (categorySource) {
            "USER_LEARNED" -> score += 10
            "MERCHANT_DICT", "MERCHANT_ALIAS" -> score += 8
            "KEYWORD" -> score += 3
            "SUBTYPE" -> score += 5
            "UNKNOWN" -> score -= 10
        }
        score = score.coerceIn(0, 100)

        val label = when {
            score >= 90 -> "100% Verified"
            score >= 80 -> "High Confidence"
            score >= 60 -> "Medium Confidence"
            else -> "Low Confidence"
        }
        // Unknown / low-confidence classifications go to review queue for learning
        val goToQueue = categorySource == "UNKNOWN" || score < 55
        return ConfidenceResult(
            score = score,
            label = label,
            goToQueue = goToQueue
        )
    }
}
