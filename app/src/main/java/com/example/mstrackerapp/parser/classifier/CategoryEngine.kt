package com.example.mstrackerapp.parser.classifier

import com.example.mstrackerapp.domain.models.SmsTransactionSubType
import com.example.mstrackerapp.parser.stage6.SmsCategory

data class CategoryResolutionResult(
    val categoryId: String,
    val categoryName: String,
    val confidence: ClassificationConfidenceScore,
    val reason: String
)

/**
 * Align CategoryEngine with live SmsCategory priority for shared callers/tests.
 */
object CategoryEngine {

    private val CATEGORY_NAMES = SmsCategory.CATEGORY_NAMES

    fun resolveCategory(
        merchantName: String,
        smsText: String,
        userCustomMappings: Map<String, String> = emptyMap()
    ): CategoryResolutionResult {
        val subType = when {
            smsText.contains("salary", ignoreCase = true) -> SmsTransactionSubType.SALARY
            smsText.contains("refund", ignoreCase = true) -> SmsTransactionSubType.REFUND
            smsText.contains("upi", ignoreCase = true) -> SmsTransactionSubType.UPI_PAYMENT
            smsText.contains("atm", ignoreCase = true) -> SmsTransactionSubType.ATM
            else -> SmsTransactionSubType.DEBIT
        }
        val result = SmsCategory.classify(merchantName, smsText, subType, userCustomMappings)
        val confidence = when (result.source) {
            "USER_LEARNED", "MERCHANT_DICT", "MERCHANT_ALIAS" -> ClassificationConfidenceScore.HIGH
            "KEYWORD", "SUBTYPE" -> ClassificationConfidenceScore.MEDIUM
            else -> ClassificationConfidenceScore.UNKNOWN
        }
        val reason = when (result.source) {
            "USER_LEARNED" -> "Matched custom user merchant mapping for '$merchantName'"
            "MERCHANT_DICT", "MERCHANT_ALIAS" -> "Matched built-in merchant dictionary for '$merchantName'"
            "KEYWORD" -> "Matched keyword for category '${result.categoryName}'"
            "SUBTYPE" -> "Matched transaction subtype"
            else -> "Unrecognized merchant or transaction keywords"
        }
        return CategoryResolutionResult(
            categoryId = result.categoryId,
            categoryName = CATEGORY_NAMES[result.categoryId] ?: result.categoryName,
            confidence = confidence,
            reason = reason
        )
    }
}
