package com.jllabs.moneylens.data.parser

import com.jllabs.moneylens.data.database.MoneyLensDatabase
import com.jllabs.moneylens.parser.classifier.MerchantNormalizer

/**
 * Loads user-learned + DB alias mappings for the classification pipeline.
 */
object ClassificationMappingLoader {

    suspend fun loadUserMappings(db: MoneyLensDatabase): Map<String, String> {
        val learned = db.userLearnedMappingDao().getAllLearnedMappingsList()
            .associate { it.merchant.uppercase() to it.categoryId }
        val mappingRows = db.merchantMappingDao().getAllMappingsList()
            .associate { it.rawSmsPattern.uppercase() to it.categoryId }
        return learned + mappingRows
    }

    suspend fun loadAliasMap(db: MoneyLensDatabase): Map<String, String> {
        val merchantsById = db.merchantDao().getAllMerchantsList().associateBy { it.id }
        val fromDb = db.merchantAliasDao().getAllAliasesList().mapNotNull { alias ->
            val canonical = merchantsById[alias.merchantId]?.name ?: return@mapNotNull null
            alias.aliasPattern.uppercase() to canonical
        }.toMap()
        return MerchantNormalizer.BUILTIN_ALIASES + fromDb
    }
}
