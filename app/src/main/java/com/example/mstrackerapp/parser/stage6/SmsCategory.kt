package com.example.mstrackerapp.parser.stage6

import com.example.mstrackerapp.domain.models.SmsTransactionSubType
import com.example.mstrackerapp.parser.classifier.MerchantDictionary
import com.example.mstrackerapp.parser.classifier.MerchantNormalizer

data class CategoryResult(val categoryId: String, val categoryName: String, val source: String)

/**
 * Live category classifier.
 *
 * Priority:
 * 1. User learned merchant
 * 2. Merchant alias → dictionary category
 * 3. Merchant dictionary
 * 4. Regex keywords (body + merchant)
 * 5. Transaction subtype
 * 6. Unknown → Other
 */
object SmsCategory {

    val CATEGORY_NAMES = mapOf(
        "cat-1" to "Food", "cat-2" to "Transport", "cat-3" to "Shopping",
        "cat-4" to "Entertainment", "cat-5" to "Education", "cat-6" to "Health",
        "cat-7" to "Travel", "cat-8" to "Fuel", "cat-9" to "Bills",
        "cat-10" to "Investment", "cat-11" to "Salary", "cat-12" to "Refund",
        "cat-13" to "Transfer", "cat-14" to "Other"
    )

    /** Kept for backward-compat with older tests / callers */
    val MERCHANT_DICT: Map<String, String> = MerchantDictionary.DEFAULT_MERCHANT_MAPPINGS

    private val KEYWORD_RULES = listOf(
        // Health
        listOf("medplus", "pharmacy", "chemist", "hospital", "medical", "doctor", "clinic",
            "medicine", "diagnostic", "health", "lab test", "pathology") to "cat-6",
        // Fuel (before transport so petrol pump ≠ cab)
        listOf("petrol pump", "fuel station", "diesel", "petrol", "cng", "gas station") to "cat-8",
        // Food
        listOf("restaurant", "dining", "cafe", "coffee", "bakery", "tiffin", "food court",
            "irani", "chicken", "pizza", "burger", "hotel restaurant", "tea stall", "chai") to "cat-1",
        // Transport
        listOf("cab", "ride", "taxi", "toll", "parking", "train", "bus fare", "metro", "ferry", "auto rickshaw") to "cat-2",
        // Shopping
        listOf("supermarket", "grocery", "mall", "online shopping", "department store",
            "fashion", "apparel", "footwear", "retail", "blinkit", "zepto", "instamart") to "cat-3",
        // Entertainment
        listOf("movie", "cinema", "theatre", "streaming", "subscription renewal", "ott",
            "gaming", "concert", "membership") to "cat-4",
        // Education
        listOf("school fee", "college fee", "university", "tuition", "exam fee", "course", "learning") to "cat-5",
        // Travel
        listOf("flight", "airline", "airport", "hotel booking", "resort", "tour package", "holiday") to "cat-7",
        // Bills
        listOf("electricity", "water bill", "broadband", "internet", "recharge", "utility",
            "gas bill", "insurance premium", "emi deducted") to "cat-9",
        // Investment
        listOf("mutual fund", "stocks", "sip", "investment", "fixed deposit", "bonds", "equity", "shares") to "cat-10",
        // Salary / Refund keyword hints
        listOf("salary", "payroll", "stipend") to "cat-11",
        listOf("refund", "reversal", "cashback") to "cat-12"
    )

    fun classify(
        merchant: String,
        body: String,
        subType: SmsTransactionSubType,
        userMappings: Map<String, String> = emptyMap(),
        aliasToCanonical: Map<String, String> = emptyMap()
    ): CategoryResult {
        val canonical = MerchantNormalizer.normalize(merchant, aliasToCanonical)
        val merchantUpper = canonical.uppercase()
        val rawUpper = merchant.uppercase()
        val bodyLower = body.lowercase()

        // 1. User learned (match canonical or raw)
        for ((pattern, catId) in userMappings) {
            val p = pattern.uppercase()
            if (p.isNotEmpty() && (merchantUpper.contains(p) || rawUpper.contains(p) || p.contains(merchantUpper))) {
                return result(catId, "USER_LEARNED")
            }
        }

        // 2+3. Merchant alias/dictionary (canonical first, then raw)
        val dictCat = MerchantDictionary.getCategoryForMerchant(canonical)
        if (dictCat != "cat-14") {
            return result(dictCat, "MERCHANT_DICT")
        }
        val dictCatRaw = MerchantDictionary.getCategoryForMerchant(merchant)
        if (dictCatRaw != "cat-14") {
            return result(dictCatRaw, "MERCHANT_ALIAS")
        }

        // 4. Keyword rules on body + merchant
        for ((keywords, catId) in KEYWORD_RULES) {
            if (keywords.any { bodyLower.contains(it) || merchantUpper.contains(it.uppercase()) }) {
                return result(catId, "KEYWORD")
            }
        }

        // 5. Transaction subtype fallbacks (only after keywords fail)
        when (subType) {
            SmsTransactionSubType.SALARY -> return result("cat-11", "SUBTYPE")
            SmsTransactionSubType.REFUND -> return result("cat-12", "SUBTYPE")
            SmsTransactionSubType.TRANSFER_IN,
            SmsTransactionSubType.TRANSFER_OUT,
            SmsTransactionSubType.CREDIT, // inbound person/bank credit without IMPS/NEFT keyword
            SmsTransactionSubType.ATM -> return result("cat-13", "SUBTYPE")
            SmsTransactionSubType.EMI -> return result("cat-9", "SUBTYPE")
            SmsTransactionSubType.UPI_PAYMENT -> {
                // Unknown UPI merchant → Transfer (P2P), not Other
                return result("cat-13", "SUBTYPE")
            }
            SmsTransactionSubType.INTEREST_CREDIT -> return result("cat-10", "SUBTYPE")
            else -> {}
        }

        // 6. Unknown
        return result("cat-14", "UNKNOWN")
    }

    private fun result(catId: String, source: String) =
        CategoryResult(catId, CATEGORY_NAMES[catId] ?: "Other", source)
}
