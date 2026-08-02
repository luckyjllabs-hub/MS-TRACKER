package com.example.mstrackerapp.data.parser

import com.example.mstrackerapp.data.database.MSTrackerDatabase
import com.example.mstrackerapp.parser.classifier.MerchantNormalizer

/**
 * Loads user-learned + DB alias mappings for the classification pipeline.
 */
object ClassificationMappingLoader {

    suspend fun loadUserMappings(db: MSTrackerDatabase): Map<String, String> {
        val learned = db.userLearnedMappingDao().getAllLearnedMappingsList()
            .associate { it.merchant.uppercase() to it.categoryId }
        val mappingRows = db.merchantMappingDao().getAllMappingsList()
            .associate { it.rawSmsPattern.uppercase() to it.categoryId }
        return learned + mappingRows
    }

    suspend fun loadAliasMap(db: MSTrackerDatabase): Map<String, String> {
        val merchantsById = db.merchantDao().getAllMerchantsList().associateBy { it.id }
        val fromDb = db.merchantAliasDao().getAllAliasesList().mapNotNull { alias ->
            val canonical = merchantsById[alias.merchantId]?.name ?: return@mapNotNull null
            alias.aliasPattern.uppercase() to canonical
        }.toMap()
        return MerchantNormalizer.BUILTIN_ALIASES + fromDb
    }
}
