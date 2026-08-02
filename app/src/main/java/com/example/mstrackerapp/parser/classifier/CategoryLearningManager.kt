package com.example.mstrackerapp.parser.classifier

import com.example.mstrackerapp.data.database.dao.UserLearnedMappingDao
import com.example.mstrackerapp.data.database.entities.UserLearnedMappingEntity
import java.util.Locale

class CategoryLearningManager(private val userLearnedMappingDao: UserLearnedMappingDao) {

    suspend fun onUserChangedCategory(merchantName: String, newCategoryId: String) {
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
}
