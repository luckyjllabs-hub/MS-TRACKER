package com.jllabs.moneylens.parser.classifier

import com.jllabs.moneylens.data.database.dao.UserLearnedMappingDao
import com.jllabs.moneylens.data.database.entities.UserLearnedMappingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FakeUserLearnedMappingDao : UserLearnedMappingDao {
    private val memoryStore = mutableMapOf<String, UserLearnedMappingEntity>()

    override fun getAllLearnedMappings(): Flow<List<UserLearnedMappingEntity>> {
        return flowOf(memoryStore.values.toList())
    }

    override suspend fun getAllLearnedMappingsList(): List<UserLearnedMappingEntity> {
        return memoryStore.values.toList()
    }

    override suspend fun getMappingForMerchant(merchant: String): UserLearnedMappingEntity? {
        return memoryStore[merchant.uppercase()]
    }

    override suspend fun insertOrUpdateMapping(mapping: UserLearnedMappingEntity) {
        memoryStore[mapping.merchant.uppercase()] = mapping
    }
}

class CategoryLearningManagerTest {

    @Test
    fun testUserChangeCategoryAndAutoClassify() = runBlocking {
        val fakeDao = FakeUserLearnedMappingDao()
        val manager = CategoryLearningManager(fakeDao)

        // 1. User changes category for Starbucks from Food (cat-1) to Education (cat-5)
        manager.onUserChangedCategory("Starbucks", "cat-5")

        val stored = fakeDao.getMappingForMerchant("STARBUCKS")
        assertNotNull(stored)
        assertEquals("STARBUCKS", stored!!.merchant)
        assertEquals("cat-5", stored.categoryId)
        assertEquals(1, stored.timesUsed)
        assertEquals("USER_CONFIRMED", stored.confidence)

        // 2. Second reclassification increments timesUsed
        manager.onUserChangedCategory("Starbucks", "cat-5")
        val updated = fakeDao.getMappingForMerchant("STARBUCKS")
        assertEquals(2, updated!!.timesUsed)

        // 3. Next time auto-classifies Starbucks to cat-5
        val autoResult = manager.autoClassifyWithLearnedRules("Starbucks")
        assertNotNull(autoResult)
        assertEquals("cat-5", autoResult!!.categoryId)
        assertEquals(ClassificationConfidenceScore.HIGH, autoResult.confidence)
    }
}
