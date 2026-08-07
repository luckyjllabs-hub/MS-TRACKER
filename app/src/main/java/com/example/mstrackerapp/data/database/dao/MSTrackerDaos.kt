package com.example.mstrackerapp.data.database.dao

import androidx.room.*
import com.example.mstrackerapp.data.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE merchant LIKE '%' || :query || '%'
           OR note LIKE '%' || :query || '%'
           OR date LIKE '%' || :query || '%'
           OR categoryId LIKE '%' || :query || '%'
           OR accountId LIKE '%' || :query || '%'
           OR bankName LIKE '%' || :query || '%'
           OR referenceNumber LIKE '%' || :query || '%'
           OR CAST(amountMinor AS TEXT) LIKE '%' || :query || '%'
        ORDER BY date DESC, time DESC, createdAt DESC
        """
    )
    fun globalSearch(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransaction(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE merchant = :merchant AND amountMinor = :amountMinor AND date = :date")
    suspend fun countByMerchantAmountDate(merchant: String, amountMinor: Long, date: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE (rawSms = :rawSms AND rawSms != '') OR (amountMinor = :amountMinor AND date = :date)")
    suspend fun countDuplicateTransaction(rawSms: String, amountMinor: Long, date: String): Int

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC, time DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = 'cat-14' ORDER BY date DESC, time DESC")
    suspend fun getOtherTransactions(): List<TransactionEntity>

    @Query(
        """
        SELECT merchant, COUNT(*) as cnt FROM transactions
        GROUP BY merchant ORDER BY cnt DESC LIMIT :limit
        """
    )
    suspend fun getTopMerchants(limit: Int): List<MerchantCountRow>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
}

data class MerchantCountRow(
    val merchant: String,
    val cnt: Int
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET monthlyLimitMinor = :limitMinor WHERE id = :id")
    suspend fun updateCategoryLimit(id: String, limitMinor: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)
}

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants")
    fun getAllMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants")
    suspend fun getAllMerchantsList(): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity)
}

@Dao
interface MerchantMappingDao {
    @Query("SELECT * FROM merchant_mappings")
    fun getAllMappings(): Flow<List<MerchantMappingEntity>>

    @Query("SELECT * FROM merchant_mappings")
    suspend fun getAllMappingsList(): List<MerchantMappingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantMappingEntity)
}

@Dao
interface MerchantAliasDao {
    @Query("SELECT * FROM merchant_aliases")
    fun getAllAliases(): Flow<List<MerchantAliasEntity>>

    @Query("SELECT * FROM merchant_aliases")
    suspend fun getAllAliasesList(): List<MerchantAliasEntity>

    @Query("SELECT * FROM merchant_aliases WHERE aliasPattern = :alias LIMIT 1")
    suspend fun getByAlias(alias: String): MerchantAliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: MerchantAliasEntity)

    @Query("DELETE FROM merchant_aliases WHERE id = :id")
    suspend fun deleteAlias(id: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY `order` ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)
}

@Dao
interface SMSQueueDao {
    @Query("SELECT * FROM sms_queue ORDER BY timestamp DESC")
    fun getAllSmsQueue(): Flow<List<SMSQueueEntity>>

    @Query("SELECT * FROM sms_queue")
    suspend fun getSmsQueueList(): List<SMSQueueEntity>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE rawText = :rawText AND timestamp = :timestamp")
    suspend fun countByRawTextAndTimestamp(rawText: String, timestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsItem(sms: SMSQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsItems(smsList: List<SMSQueueEntity>)

    @Query("DELETE FROM sms_queue WHERE id = :id")
    suspend fun deleteSmsItem(id: String)
}

@Dao
interface SMSInboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSms(sms: SMSInboxEntity)

    @Query("UPDATE sms_inbox SET isProcessed = 1, processingStatus = :status WHERE rawHash = :rawHash")
    suspend fun markProcessed(rawHash: String, status: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingsEntity)
}
