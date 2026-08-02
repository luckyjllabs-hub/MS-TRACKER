package com.example.mstrackerapp.parser.classifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryEngineTest {

    @Test
    fun testPriority1UserCustomMapping() {
        val customRules = mapOf("SWIGGY" to "cat-8") // Custom override to Fuel
        val result = CategoryEngine.resolveCategory("Swiggy", "Paid Rs 450 at Swiggy", customRules)

        assertEquals("cat-8", result.categoryId)
        assertEquals("Fuel", result.categoryName)
        assertEquals(ClassificationConfidenceScore.HIGH, result.confidence)
        assertTrue(result.reason.contains("custom user merchant mapping"))
    }

    @Test
    fun testPriority2BuiltInMerchantDictionary() {
        val result = CategoryEngine.resolveCategory("Uber", "INR 280.00 debited for Uber ride")

        assertEquals("cat-2", result.categoryId)
        assertEquals("Transport", result.categoryName)
        assertEquals(ClassificationConfidenceScore.HIGH, result.confidence)
        assertTrue(result.reason.contains("built-in merchant dictionary"))
    }

    @Test
    fun testPriority3KeywordMatching() {
        val result = CategoryEngine.resolveCategory("Unknown Meds", "Paid Rs 1,500 at local pharmacy store")

        assertEquals("cat-6", result.categoryId)
        assertEquals("Health", result.categoryName)
        assertEquals(ClassificationConfidenceScore.MEDIUM, result.confidence)
        assertTrue(result.reason.contains("keyword", ignoreCase = true))
    }

    @Test
    fun testPriority4UnknownFallback() {
        val result = CategoryEngine.resolveCategory("Random Vendor", "Txn of Rs 500 completed successfully")

        assertEquals("cat-14", result.categoryId)
        assertEquals("Other", result.categoryName)
        assertEquals(ClassificationConfidenceScore.UNKNOWN, result.confidence)
        assertTrue(result.reason.contains("Unrecognized merchant"))
    }
}
