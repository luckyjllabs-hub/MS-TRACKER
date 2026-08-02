package com.example.mstrackerapp.parser.classifier

data class CategoryResolutionResult(
    val categoryId: String,
    val categoryName: String,
    val confidence: ClassificationConfidenceScore,
    val reason: String
)

object CategoryEngine {

    private val CATEGORY_NAMES = mapOf(
        "cat-1" to "Food",
        "cat-2" to "Transport",
        "cat-3" to "Shopping",
        "cat-4" to "Entertainment",
        "cat-5" to "Education",
        "cat-6" to "Health",
        "cat-7" to "Travel",
        "cat-8" to "Fuel",
        "cat-9" to "Bills",
        "cat-10" to "Investment",
        "cat-11" to "Salary",
        "cat-12" to "Refund",
        "cat-13" to "Transfer",
        "cat-14" to "Other"
    )

    private val KEYWORD_RULES = mapOf(
        "cat-11" to listOf("salary", "payroll", "stipend", "sal cr", "salary credited", "wages"),
        "cat-12" to listOf("refund", "refunded", "reversed", "reversal", "cashback"),
        "cat-13" to listOf("credited", "received", "deposited", "added rs", "cr to", "tfr to", "tfr from", "transfer", "imps", "neft", "rtgs"),
        "cat-6" to listOf("hospital", "pharmacy", "medical", "doctor", "clinic", "lab", "medicine"),
        "cat-8" to listOf("petrol", "diesel", "fuel", "gas station", "cng"),
        "cat-1" to listOf("food", "restaurant", "cafe", "dining", "eat", "pizza", "burger", "baking", "bakery"),
        "cat-2" to listOf("cab", "ride", "auto", "taxi", "metro", "toll", "parking", "train", "bus"),
        "cat-4" to listOf("movie", "cinema", "theatre", "show", "subscription", "ott", "gaming"),
        "cat-5" to listOf("school", "college", "tuition", "exam", "fee", "course", "university"),
        "cat-7" to listOf("flight", "hotel", "resort", "tour", "airway", "airline", "staycation"),
        "cat-9" to listOf("electricity", "water", "broadband", "recharge", "utility", "bill"),
        "cat-10" to listOf("mutual fund", "stocks", "sip", "investment", "fd", "bonds", "equity"),
        "cat-3" to listOf("mart", "store", "mall", "retail", "fashion", "clothes", "apparel")
    )

    fun resolveCategory(
        merchantName: String,
        smsText: String,
        userCustomMappings: Map<String, String> = emptyMap()
    ): CategoryResolutionResult {
        val cleanMerchant = merchantName.trim().uppercase()
        val cleanText = smsText.lowercase()

        // Priority 1: User Custom Merchant Mapping (Room DB)
        for ((customPattern, catId) in userCustomMappings) {
            val patternUpper = customPattern.trim().uppercase()
            if (patternUpper.isNotEmpty() && (cleanMerchant.contains(patternUpper) || smsText.uppercase().contains(patternUpper))) {
                val catName = CATEGORY_NAMES[catId] ?: "Custom Category"
                return CategoryResolutionResult(
                    categoryId = catId,
                    categoryName = catName,
                    confidence = ClassificationConfidenceScore.HIGH,
                    reason = "Matched custom user merchant mapping for '$customPattern'"
                )
            }
        }

        // Priority 2: Built-in Merchant Dictionary
        if (cleanMerchant.isNotEmpty()) {
            for ((dictPattern, catId) in MerchantDictionary.DEFAULT_MERCHANT_MAPPINGS) {
                if (cleanMerchant.contains(dictPattern)) {
                    val catName = CATEGORY_NAMES[catId] ?: "Merchant Category"
                    return CategoryResolutionResult(
                        categoryId = catId,
                        categoryName = catName,
                        confidence = ClassificationConfidenceScore.HIGH,
                        reason = "Matched built-in merchant dictionary for '$dictPattern'"
                    )
                }
            }
        }

        // Priority 3: Keyword Matching in SMS text
        for ((catId, keywords) in KEYWORD_RULES) {
            for (keyword in keywords) {
                if (cleanText.contains(keyword)) {
                    val catName = CATEGORY_NAMES[catId] ?: "Keyword Category"
                    return CategoryResolutionResult(
                        categoryId = catId,
                        categoryName = catName,
                        confidence = ClassificationConfidenceScore.MEDIUM,
                        reason = "Matched keyword '$keyword' for category '$catName'"
                    )
                }
            }
        }

        // Priority 4: Transaction Classifier Fallback (Salary, Refund, Credit/Debit)
        val regexResult = RegexTransactionClassifier.classify(smsText)
        if (regexResult.category == TransactionClassificationCategory.SALARY) {
            return CategoryResolutionResult(
                categoryId = "cat-11",
                categoryName = "Salary",
                confidence = ClassificationConfidenceScore.HIGH,
                reason = "Matched Salary transaction pattern"
            )
        }
        if (regexResult.category == TransactionClassificationCategory.REFUND) {
            return CategoryResolutionResult(
                categoryId = "cat-12",
                categoryName = "Refund",
                confidence = ClassificationConfidenceScore.HIGH,
                reason = "Matched Refund transaction pattern"
            )
        }
        if (regexResult.category == TransactionClassificationCategory.CREDIT) {
            return CategoryResolutionResult(
                categoryId = "cat-13",
                categoryName = "Transfer",
                confidence = ClassificationConfidenceScore.HIGH,
                reason = "Matched Credit transaction pattern"
            )
        }

        // Priority 5: Fallback Unknown
        return CategoryResolutionResult(
            categoryId = "cat-14",
            categoryName = "Other",
            confidence = ClassificationConfidenceScore.UNKNOWN,
            reason = "Unrecognized merchant or transaction keywords"
        )
    }
}
