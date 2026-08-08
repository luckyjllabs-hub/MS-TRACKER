package com.jllabs.moneylens.parser.classifier

import com.jllabs.moneylens.data.database.dao.MerchantAliasDao
import com.jllabs.moneylens.data.database.dao.MerchantDao
import com.jllabs.moneylens.data.database.dao.UserLearnedMappingDao
import com.jllabs.moneylens.data.database.entities.MerchantAliasEntity
import com.jllabs.moneylens.data.database.entities.MerchantEntity
import com.jllabs.moneylens.data.database.entities.UserLearnedMappingEntity
import java.util.Locale
import java.util.UUID

class CategoryLearningManager(
    private val userLearnedMappingDao: UserLearnedMappingDao,
    private val merchantDao: MerchantDao? = null,
    private val merchantAliasDao: MerchantAliasDao? = null
) {

    /**
     * Persist user category correction for a merchant.
     * Also stores alias → canonical merchant when [canonicalName] differs from raw.
     */
    suspend fun onUserChangedCategory(
        merchantName: String,
        newCategoryId: String,
        canonicalName: String? = null,
        rawAlias: String? = null
    ) {
        val cleanMerchant = merchantName.trim().uppercase(Locale.ROOT)
        if (cleanMerchant.isBlank()) return

        val existing = userLearnedMappingDao.getMappingForMerchant(cleanMerchant)
        val updatedEntity = if (existing != null) {
            existing.copy(
                categoryId = newCategoryId,
                timesUsed = existing.timesUsed + 1,
                confidence = "USER_CONFIRMED",
                lastUsedTimestamp = System.currentTimeMillis()
            )
        } else {
            UserLearnedMappingEntity(
                merchant = cleanMerchant,
                categoryId = newCategoryId,
                timesUsed = 1,
                confidence = "USER_CONFIRMED",
                lastUsedTimestamp = System.currentTimeMillis()
            )
        }
        userLearnedMappingDao.insertOrUpdateMapping(updatedEntity)

        // Also learn under canonical name if provided
        val canonical = (canonicalName ?: MerchantNormalizer.normalize(merchantName)).trim()
        if (canonical.isNotBlank() && canonical.uppercase(Locale.ROOT) != cleanMerchant) {
            val canonKey = canonical.uppercase(Locale.ROOT)
            val canonExisting = userLearnedMappingDao.getMappingForMerchant(canonKey)
            userLearnedMappingDao.insertOrUpdateMapping(
                (canonExisting ?: UserLearnedMappingEntity(
                    merchant = canonKey,
                    categoryId = newCategoryId
                )).copy(
                    categoryId = newCategoryId,
                    timesUsed = (canonExisting?.timesUsed ?: 0) + 1,
                    confidence = "USER_CONFIRMED",
                    lastUsedTimestamp = System.currentTimeMillis()
                )
            )
        }

        saveMerchantAndAlias(canonical.ifBlank { merchantName }, rawAlias ?: merchantName, newCategoryId)
    }

    private suspend fun saveMerchantAndAlias(canonical: String, alias: String, categoryId: String) {
        val mDao = merchantDao ?: return
        val aDao = merchantAliasDao ?: return
        val display = MerchantNormalizer.normalize(canonical)
        val existing = mDao.getByName(display)
        val merchantId = existing?.id ?: UUID.randomUUID().toString()
        if (existing == null) {
            mDao.insertMerchant(
                MerchantEntity(
                    id = merchantId,
                    name = display,
                    defaultCategoryId = categoryId
                )
            )
        } else if (existing.defaultCategoryId != categoryId) {
            mDao.insertMerchant(existing.copy(defaultCategoryId = categoryId))
        }

        val aliasKey = MerchantNormalizer.clean(alias).uppercase(Locale.ROOT)
        if (aliasKey.isBlank() || aliasKey.equals(display, ignoreCase = true)) return
        val existingAlias = aDao.getByAlias(aliasKey)
        if (existingAlias == null) {
            aDao.insertAlias(
                MerchantAliasEntity(
                    id = UUID.randomUUID().toString(),
                    merchantId = merchantId,
                    aliasPattern = aliasKey
                )
            )
        }
    }

    suspend fun autoClassifyWithLearnedRules(merchantName: String): CategoryResolutionResult? {
        val cleanMerchant = merchantName.trim().uppercase(Locale.ROOT)
        if (cleanMerchant.isBlank()) return null
        val learned = userLearnedMappingDao.getMappingForMerchant(cleanMerchant) ?: return null
        return CategoryResolutionResult(
            categoryId = learned.categoryId,
            categoryName = "Learned Category",
            confidence = ClassificationConfidenceScore.HIGH,
            reason = "Auto-classified using user learned rule for '$cleanMerchant' (Used ${learned.timesUsed} times)"
        )
    }

    suspend fun mergeAlias(alias: String, canonicalMerchant: String, categoryId: String) {
        onUserChangedCategory(
            merchantName = canonicalMerchant,
            newCategoryId = categoryId,
            canonicalName = canonicalMerchant,
            rawAlias = alias
        )
    }
}
