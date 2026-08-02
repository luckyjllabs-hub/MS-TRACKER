package com.example.mstrackerapp.parser.stage6

import com.example.mstrackerapp.domain.models.SmsTransactionSubType

data class CategoryResult(val categoryId: String, val categoryName: String, val source: String)

object SmsCategory {

    val MERCHANT_DICT = mapOf(
        // Food
        "SWIGGY" to "cat-1", "ZOMATO" to "cat-1", "DOMINOS" to "cat-1", "PIZZA" to "cat-1",
        "MCDONALD" to "cat-1", "KFC" to "cat-1", "SUBWAY" to "cat-1", "STARBUCKS" to "cat-1",
        "DUNKIN" to "cat-1", "BURGER" to "cat-1", "BARBEQUE" to "cat-1", "DINEOUT" to "cat-1",
        "EATSURE" to "cat-1", "FAASOS" to "cat-1", "FRESHMENU" to "cat-1",
        // Transport
        "UBER" to "cat-2", "OLA" to "cat-2", "RAPIDO" to "cat-2", "METRO" to "cat-2",
        "IRCTC" to "cat-2", "REDBUS" to "cat-2", "CHALO" to "cat-2",
        // Shopping
        "AMAZON" to "cat-3", "FLIPKART" to "cat-3", "MYNTRA" to "cat-3", "MEESHO" to "cat-3",
        "NYKAA" to "cat-3", "AJIO" to "cat-3", "SNAPDEAL" to "cat-3", "SHOPSY" to "cat-3",
        "BIGBAZAAR" to "cat-3", "DMART" to "cat-3", "RELIANCE" to "cat-3",
        // Entertainment
        "NETFLIX" to "cat-4", "SPOTIFY" to "cat-4", "HOTSTAR" to "cat-4", "PRIME" to "cat-4",
        "SONYLIV" to "cat-4", "ZEENOW" to "cat-4", "BOOKMYSHOW" to "cat-4", "PVR" to "cat-4",
        "INOX" to "cat-4", "GAANA" to "cat-4", "JIOSAAVN" to "cat-4", "WYNK" to "cat-4",
        // Education
        "BYJU" to "cat-5", "UNACADEMY" to "cat-5", "VEDANTU" to "cat-5", "COURSERA" to "cat-5",
        "UDEMY" to "cat-5", "SKILLSHARE" to "cat-5",
        // Health
        "APOLLO" to "cat-6", "1MG" to "cat-6", "PHARMEASY" to "cat-6", "NETMEDS" to "cat-6",
        "PRACTO" to "cat-6", "MEDLIFE" to "cat-6", "HEALTHKART" to "cat-6",
        // Travel
        "MAKEMYTRIP" to "cat-7", "GOIBIBO" to "cat-7", "YATRA" to "cat-7", "CLEARTRIP" to "cat-7",
        "AIRBNB" to "cat-7", "OYO" to "cat-7", "INDIGO" to "cat-7", "SPICEJET" to "cat-7",
        "AIRINDIA" to "cat-7",
        // Fuel
        "PETROL" to "cat-8", "HPCL" to "cat-8", "BPCL" to "cat-8", "IOCL" to "cat-8",
        "SHELL" to "cat-8", "ESSAR" to "cat-8",
        // Bills
        "BESCOM" to "cat-9", "MSEDCL" to "cat-9", "TATAPOWER" to "cat-9", "AIRTEL" to "cat-9",
        "JIO" to "cat-9", "BSNL" to "cat-9", "VODAFONE" to "cat-9", "IDEA" to "cat-9",
        "MAHANAGAR" to "cat-9", "BBMP" to "cat-9",
        // Investment
        "GROWW" to "cat-10", "ZERODHA" to "cat-10", "PAYTMMONEY" to "cat-10", "UPSTOX" to "cat-10",
        "ICICI DIRECT" to "cat-10", "HDFC SECURITIES" to "cat-10", "SBI MF" to "cat-10"
    )

    private val KEYWORD_RULES = listOf(
        // Food
        listOf("restaurant", "cafe", "coffee", "dine", "eat", "food", "bakery", "bake", "hotel restaurant") to "cat-1",
        // Transport
        listOf("cab", "ride", "auto", "taxi", "toll", "parking", "fuel", "petrol", "diesel", "train", "bus", "metro", "ferry") to "cat-2",
        // Shopping
        listOf("mart", "store", "mall", "retail", "fashion", "clothes", "apparel", "footwear", "supermarket") to "cat-3",
        // Entertainment
        listOf("movie", "cinema", "theatre", "show", "stream", "ott", "gaming", "game", "concert", "event") to "cat-4",
        // Education
        listOf("school", "college", "university", "tuition", "exam", "course", "class", "learning") to "cat-5",
        // Health
        listOf("hospital", "pharmacy", "medical", "doctor", "clinic", "lab", "medicine", "diagnostic", "health") to "cat-6",
        // Travel
        listOf("flight", "airline", "airport", "hotel", "resort", "booking", "travel", "trip", "tour", "holiday") to "cat-7",
        // Fuel
        listOf("petrol", "diesel", "cng", "fuel", "gas station", "pump") to "cat-8",
        // Bills
        listOf("electricity", "water", "broadband", "internet", "recharge", "utility", "bill", "emi", "insurance") to "cat-9",
        // Investment
        listOf("mutual fund", "stocks", "sip", "investment", "fixed deposit", "fd", "bonds", "equity", "shares") to "cat-10"
    )

    private val CATEGORY_NAMES = mapOf(
        "cat-1" to "Food", "cat-2" to "Transport", "cat-3" to "Shopping",
        "cat-4" to "Entertainment", "cat-5" to "Education", "cat-6" to "Health",
        "cat-7" to "Travel", "cat-8" to "Fuel", "cat-9" to "Bills",
        "cat-10" to "Investment", "cat-11" to "Salary", "cat-12" to "Refund",
        "cat-13" to "Transfer", "cat-14" to "Other"
    )

    fun classify(
        merchant: String,
        body: String,
        subType: SmsTransactionSubType,
        userMappings: Map<String, String> = emptyMap()
    ): CategoryResult {
        val merchantUpper = merchant.uppercase()
        val bodyLower = body.lowercase()

        // 1. User learned
        for ((pattern, catId) in userMappings) {
            if (merchantUpper.contains(pattern.uppercase())) {
                return CategoryResult(catId, CATEGORY_NAMES[catId] ?: "Custom", "USER_LEARNED")
            }
        }
        // 2. Merchant dictionary
        for ((dictKey, catId) in MERCHANT_DICT) {
            if (merchantUpper.contains(dictKey)) {
                return CategoryResult(catId, CATEGORY_NAMES[catId] ?: "Merchant", "MERCHANT_DICT")
            }
        }
        // 3. Sub-type fallbacks
        when (subType) {
            SmsTransactionSubType.SALARY -> return CategoryResult("cat-11", "Salary", "SUBTYPE")
            SmsTransactionSubType.REFUND -> return CategoryResult("cat-12", "Refund", "SUBTYPE")
            SmsTransactionSubType.TRANSFER_IN, SmsTransactionSubType.TRANSFER_OUT -> return CategoryResult("cat-13", "Transfer", "SUBTYPE")
            SmsTransactionSubType.ATM -> return CategoryResult("cat-13", "Transfer", "SUBTYPE")
            SmsTransactionSubType.EMI -> return CategoryResult("cat-9", "Bills", "SUBTYPE")
            else -> {}
        }
        // 4. Keyword rules on body
        for ((keywords, catId) in KEYWORD_RULES) {
            if (keywords.any { bodyLower.contains(it) }) {
                return CategoryResult(catId, CATEGORY_NAMES[catId] ?: "Category", "KEYWORD")
            }
        }
        // 5. Unknown
        return CategoryResult("cat-14", "Other", "UNKNOWN")
    }
}
