package com.jllabs.moneylens.parser.classifier

import com.jllabs.moneylens.data.database.dao.MerchantMappingDao
import com.jllabs.moneylens.data.database.entities.MerchantMappingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Comprehensive offline Indian merchant → category dictionary.
 * All keys uppercase. Category IDs match seeded Room categories.
 */
object MerchantDictionary {

    val DEFAULT_MERCHANT_MAPPINGS: Map<String, String> = linkedMapOf(
        // Food
        "SWIGGY" to "cat-1", "ZOMATO" to "cat-1", "DOMINOS" to "cat-1", "PIZZA HUT" to "cat-1",
        "PIZZAHUT" to "cat-1", "KFC" to "cat-1", "MCDONALD" to "cat-1", "STARBUCKS" to "cat-1",
        "CAFE COFFEE DAY" to "cat-1", "CCD" to "cat-1", "BARBEQUE" to "cat-1", "BBQ NATION" to "cat-1",
        "DINEOUT" to "cat-1", "EATSURE" to "cat-1", "FAASOS" to "cat-1", "FRESHMENU" to "cat-1",
        "HYDERABAD IRANI" to "cat-1", "IRANI" to "cat-1", "YASHIKA CHICKEN" to "cat-1",
        "FRESHTOHOME" to "cat-1", "SUBWAY" to "cat-1", "BURGER KING" to "cat-1", "DUNKIN" to "cat-1",

        // Transport
        "UBER" to "cat-2", "OLA" to "cat-2", "RAPIDO" to "cat-2", "REDBUS" to "cat-2",
        "IRCTC" to "cat-2", "INDIAN RAILWAYS" to "cat-2", "NAMMA METRO" to "cat-2",
        "NAMMA YATRI" to "cat-2", "CHALO" to "cat-2", "METRO" to "cat-2",

        // Shopping / quick commerce
        "AMAZON" to "cat-3", "AMZN" to "cat-3", "FLIPKART" to "cat-3", "MYNTRA" to "cat-3",
        "AJIO" to "cat-3", "MEESHO" to "cat-3", "NYKAA" to "cat-3", "SNAPDEAL" to "cat-3",
        "RELIANCE DIGITAL" to "cat-3", "CROMA" to "cat-3", "DMART" to "cat-3", "BIGBAZAAR" to "cat-3",
        "BLINKIT" to "cat-3", "ZEPTO" to "cat-3", "INSTAMART" to "cat-3", "DUNZO" to "cat-3",
        "BIGBASKET" to "cat-3", "GREEN CITY" to "cat-3", "SPAR" to "cat-3", "LENSKART" to "cat-3",
        "BHIMA" to "cat-3", "SAMSUNG" to "cat-3", "RELIANCE" to "cat-3", "URBAN COMPANY" to "cat-3",
        "URBANCOMPANY" to "cat-3",

        // Entertainment
        "NETFLIX" to "cat-4", "SPOTIFY" to "cat-4", "HOTSTAR" to "cat-4", "PRIME VIDEO" to "cat-4",
        "SONYLIV" to "cat-4", "BOOKMYSHOW" to "cat-4", "PVR" to "cat-4", "INOX" to "cat-4",
        "YOUTUBE PREMIUM" to "cat-4", "YOUTUBE" to "cat-4", "GAANA" to "cat-4", "JIOSAAVN" to "cat-4",

        // Education
        "COURSERA" to "cat-5", "UDEMY" to "cat-5", "BYJU" to "cat-5", "UNACADEMY" to "cat-5",
        "VEDANTU" to "cat-5", "SKILLSHARE" to "cat-5",

        // Health
        "APOLLO" to "cat-6", "MEDPLUS" to "cat-6", "PHARMEASY" to "cat-6", "1MG" to "cat-6",
        "NETMEDS" to "cat-6", "PRACTO" to "cat-6", "ASTER" to "cat-6", "SVASTHA" to "cat-6",
        "SHREENITA" to "cat-6", "REDCLIFFE" to "cat-6", "HEALTHKART" to "cat-6",

        // Travel
        "MAKEMYTRIP" to "cat-7", "GOIBIBO" to "cat-7", "YATRA" to "cat-7", "CLEARTRIP" to "cat-7",
        "INDIGO" to "cat-7", "SPICEJET" to "cat-7", "AIR INDIA" to "cat-7", "AIRINDIA" to "cat-7",
        "OYO" to "cat-7", "AIRBNB" to "cat-7",

        // Fuel
        "INDIAN OIL" to "cat-8", "IOCL" to "cat-8", "HPCL" to "cat-8", "BPCL" to "cat-8",
        "BHARAT PETROLEUM" to "cat-8", "SHELL" to "cat-8", "ESSAR" to "cat-8",
        "KESARI PETRO" to "cat-8", "CHITHRA SERVICE" to "cat-8", "PETROL" to "cat-8",

        // Bills / utilities / telecom / insurance / EMI finance
        "BESCOM" to "cat-9", "TSSPDCL" to "cat-9", "APSPDCL" to "cat-9", "MSEDCL" to "cat-9",
        "TATA POWER" to "cat-9", "TATAPOWER" to "cat-9", "GAIL" to "cat-9",
        "JIO" to "cat-9", "AIRTEL" to "cat-9", "BSNL" to "cat-9", "VODAFONE" to "cat-9",
        "IDEA" to "cat-9", "LIC" to "cat-9", "POLICYBAZAAR" to "cat-9",
        "BAJAJ FINANCE" to "cat-9", "BAJAJFINSERV" to "cat-9",
        "HDFC LOAN" to "cat-9", "ICICI LOAN" to "cat-9", "BBMP" to "cat-9", "MAHANAGAR" to "cat-9",

        // Investment
        "GROWW" to "cat-10", "ZERODHA" to "cat-10", "UPSTOX" to "cat-10", "PAYTM MONEY" to "cat-10",
        "PAYTMMONEY" to "cat-10", "ICICI DIRECT" to "cat-10", "HDFC SECURITIES" to "cat-10",
        "SBI MF" to "cat-10"
    )

    fun getCategoryForMerchant(merchantName: String, customMappings: Map<String, String> = emptyMap()): String {
        val upperMerchant = merchantName.uppercase()

        for ((pattern, catId) in customMappings) {
            if (upperMerchant.contains(pattern.uppercase())) return catId
        }

        val sorted = DEFAULT_MERCHANT_MAPPINGS.entries.sortedByDescending { it.key.length }
        for ((pattern, catId) in sorted) {
            if (upperMerchant.contains(pattern)) return catId
        }
        return "cat-14"
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
