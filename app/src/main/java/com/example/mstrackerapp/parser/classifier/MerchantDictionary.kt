package com.example.mstrackerapp.parser.classifier

import com.example.mstrackerapp.data.database.dao.MerchantMappingDao
import com.example.mstrackerapp.data.database.entities.MerchantMappingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

object MerchantDictionary {

    val DEFAULT_MERCHANT_MAPPINGS: Map<String, String> = mapOf(
        "SWIGGY" to "cat-1",       // Food
        "ZOMATO" to "cat-1",       // Food
        "STARBUCKS" to "cat-1",    // Food
        "DOMINOS" to "cat-1",      // Food
        "MCDONALDS" to "cat-1",    // Food

        "UBER" to "cat-2",         // Transport
        "OLA" to "cat-2",          // Transport
        "RAPIDO" to "cat-2",       // Transport
        "Namma Yatri" to "cat-2",  // Transport

        "AMAZON" to "cat-3",       // Shopping
        "FLIPKART" to "cat-3",     // Shopping
        "MYNTRA" to "cat-3",       // Shopping
        "AJIO" to "cat-3",         // Shopping

        "NETFLIX" to "cat-4",      // Entertainment
        "BOOKMYSHOW" to "cat-4",   // Entertainment
        "PVR" to "cat-4",          // Entertainment
        "SPOTIFY" to "cat-4",      // Entertainment

        "COURSERA" to "cat-5",     // Education
        "UDEMY" to "cat-5",        // Education
        "BYJUS" to "cat-5",        // Education

        "APOLLO" to "cat-6",       // Health
        "1MG" to "cat-6",          // Health
        "PHARMEASY" to "cat-6",    // Health

        "MAKEMYTRIP" to "cat-7",   // Travel
        "CLEARTRIP" to "cat-7",    // Travel
        "GOIBIBO" to "cat-7",      // Travel

        "HPCL" to "cat-8",         // Fuel
        "BPCL" to "cat-8",         // Fuel
        "IOCL" to "cat-8",         // Fuel
        "SHELL" to "cat-8",        // Fuel

        "AIRTEL" to "cat-9",       // Bills
        "JIO" to "cat-9",          // Bills
        "BESCOM" to "cat-9",       // Bills
        "TATA POWER" to "cat-9",   // Bills

        "ZERODHA" to "cat-10",     // Investment
        "GROWW" to "cat-10",       // Investment
        "UPSTOX" to "cat-10"       // Investment
    )

    fun getCategoryForMerchant(merchantName: String, customMappings: Map<String, String> = emptyMap()): String {
        val upperMerchant = merchantName.uppercase()

        // 1. Check custom user-edited mappings first
        for ((pattern, catId) in customMappings) {
            if (upperMerchant.contains(pattern.uppercase())) {
                return catId
            }
        }

        // 2. Check default dictionary
        for ((pattern, catId) in DEFAULT_MERCHANT_MAPPINGS) {
            if (upperMerchant.contains(pattern)) {
                return catId
            }
        }

        // 3. Fallback
        return "cat-14" // Other
    }
}

class EditableMerchantDictionaryRepository(private val mappingDao: MerchantMappingDao) {

    val customMappingsFlow: Flow<Map<String, String>> = mappingDao.getAllMappings().map { list ->
        list.associate { it.rawSmsPattern.uppercase() to it.categoryId }
    }

    suspend fun addOrUpdateMerchantMapping(rawPattern: String, categoryId: String) {
        val entity = MerchantMappingEntity(
            id = UUID.randomUUID().toString(),
            rawSmsPattern = rawPattern.uppercase(),
            merchantId = rawPattern.uppercase(),
            categoryId = categoryId
        )
        mappingDao.insertMapping(entity)
    }
}
