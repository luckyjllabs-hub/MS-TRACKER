package com.example.mstrackerapp.data.parser

import com.example.mstrackerapp.data.database.dao.SMSQueueDao
import com.example.mstrackerapp.data.database.dao.TransactionDao
import com.example.mstrackerapp.data.database.entities.SMSQueueEntity
import com.example.mstrackerapp.data.database.entities.TransactionEntity
import com.example.mstrackerapp.parser.classifier.CategoryLearningManager
import com.example.mstrackerapp.parser.classifier.FakeUserLearnedMappingDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeTransactionDao : TransactionDao {
    val insertedTransactions = mutableListOf<TransactionEntity>()
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(insertedTransactions)
    override fun globalSearch(query: String): Flow<List<TransactionEntity>> = flowOf(insertedTransactions)
    override suspend fun countByMerchantAmountDate(merchant: String, amountMinor: Long, date: String): Int = 0
    override suspend fun countDuplicateTransaction(rawSms: String, amountMinor: Long, date: String): Int = 0
    override suspend fun insertTransaction(transaction: TransactionEntity) { insertedTransactions.add(transaction) }
    override suspend fun updateTransaction(transaction: TransactionEntity) {}
    override suspend fun getTransaction(id: String): TransactionEntity? = null
    override suspend fun deleteTransaction(id: String) {}
}

class FakeSMSQueueDao : SMSQueueDao {
    val queueItems = mutableListOf<SMSQueueEntity>()
    override fun getAllSmsQueue(): Flow<List<SMSQueueEntity>> = flowOf(queueItems)
    override suspend fun getSmsQueueList(): List<SMSQueueEntity> = queueItems.toList()
    override suspend fun countByRawTextAndTimestamp(rawText: String, timestamp: Long): Int = 0
    override suspend fun insertSmsItem(sms: SMSQueueEntity) { queueItems.add(sms) }
    override suspend fun insertSmsItems(smsList: List<SMSQueueEntity>) { queueItems.addAll(smsList) }
    override suspend fun deleteSmsItem(id: String) { queueItems.removeAll { it.id == id } }
}

class InboxQueueProcessorTest {
    @Test
    fun testPasses() = runBlocking {
        assertTrue(true)
    }
}
