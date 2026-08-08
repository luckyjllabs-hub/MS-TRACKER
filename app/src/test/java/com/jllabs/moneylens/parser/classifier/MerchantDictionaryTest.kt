package com.jllabs.moneylens.parser.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantDictionaryTest {

    @Test
    fun testDefaultMerchantMappings() {
        assertEquals("cat-1", MerchantDictionary.getCategoryForMerchant("Swiggy Order #123"))
        assertEquals("cat-2", MerchantDictionary.getCategoryForMerchant("Uber Trip Ride"))
        assertEquals("cat-3", MerchantDictionary.getCategoryForMerchant("Amazon India Store"))
        assertEquals("cat-6", MerchantDictionary.getCategoryForMerchant("Apollo Pharmacy"))
        assertEquals("cat-4", MerchantDictionary.getCategoryForMerchant("Netflix Subscription"))
    }

    @Test
    fun testCustomEditableMappingOverride() {
        val customMappings = mapOf("SWIGGY" to "cat-8") // Custom override to Fuel
        val categoryId = MerchantDictionary.getCategoryForMerchant("Swiggy Order #123", customMappings)
        assertEquals("cat-8", categoryId)
    }

    @Test
    fun testUnmappedMerchantFallback() {
        val categoryId = MerchantDictionary.getCategoryForMerchant("Unknown Local Shop 99")
        assertEquals("cat-14", categoryId)
    }
}
