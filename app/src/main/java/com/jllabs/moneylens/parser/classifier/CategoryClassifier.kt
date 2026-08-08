package com.jllabs.moneylens.parser.classifier

object CategoryClassifier {
    fun classifyMerchant(merchant: String): String {
        val lower = merchant.lowercase()
        return when {
            lower.contains("starbucks") || lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") -> "cat-1"
            lower.contains("uber") || lower.contains("ola") || lower.contains("metro") || lower.contains("fuel") -> "cat-2"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "cat-3"
            lower.contains("netflix") || lower.contains("cinema") || lower.contains("spotify") -> "cat-6"
            else -> "cat-8"
        }
    }
}
