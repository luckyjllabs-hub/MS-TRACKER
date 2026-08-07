package com.example.mstrackerapp.data.repository

import com.example.mstrackerapp.data.MSTrackerRepository
import com.example.mstrackerapp.data.database.dao.*
import com.example.mstrackerapp.data.database.entities.*
import com.example.mstrackerapp.data.local.preferences.UserPreferencesRepository
import com.example.mstrackerapp.domain.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RoomMSTrackerRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
    private val smsQueueDao: SMSQueueDao,
    private val userPrefs: UserPreferencesRepository,
    private val userLearnedMappingDao: UserLearnedMappingDao? = null,
    private val merchantDao: MerchantDao? = null,
    private val merchantAliasDao: MerchantAliasDao? = null
) : MSTrackerRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val accountsFlow: Flow<List<Account>> = accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    val categoriesFlow: Flow<List<Category>> = categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    val transactionsFlow: Flow<List<Transaction>> = transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    val goalsFlow: Flow<List<Goal>> = goalDao.getAllGoals().map { list -> list.map { it.toDomain() } }
    val smsQueueFlow: Flow<List<SmsQueueItem>> = smsQueueDao.getAllSmsQueue().map { list -> list.map { it.toDomain() } }

    override val accounts: StateFlow<List<Account>>
        get() = throw UnsupportedOperationException("Use Room reactive Flow properties")

    override val categories: StateFlow<List<Category>>
        get() = throw UnsupportedOperationException("Use Room reactive Flow properties")

    override val transactions: StateFlow<List<Transaction>>
        get() = throw UnsupportedOperationException("Use Room reactive Flow properties")

    override val goals: StateFlow<List<Goal>>
        get() = throw UnsupportedOperationException("Use Room reactive Flow properties")

    override val smsQueue: StateFlow<List<SmsQueueItem>>
        get() = throw UnsupportedOperationException("Use Room reactive Flow properties")

    override val isPrivacyMasked: StateFlow<Boolean>
        get() = throw UnsupportedOperationException("Use UserPreferencesRepository Flow")

    suspend fun insertTransaction(
        type: TransactionType,
        amountRupees: Double,
        accountId: String,
        categoryId: String,
        merchant: String,
        note: String
    ) {
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = type.name,
            amountMinor = (amountRupees * 100).toLong(),
            accountId = accountId,
            toAccountId = null,
            categoryId = categoryId,
            merchantId = null,
            merchant = merchant.ifEmpty { "Transaction" },
            date = dateFormat.format(Date()),
            time = timeFormat.format(Date()),
            note = note,
            source = "Manual"
        )
        transactionDao.insertTransaction(entity)
    }

    suspend fun insertAccount(name: String, type: AccountType, startingBalanceRupees: Double, icon: String) {
        val entity = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type.name,
            institution = name,
            startingBalanceMinor = (startingBalanceRupees * 100).toLong(),
            icon = icon.ifEmpty { "🏦" },
            includeInNetWorth = true,
            order = 10
        )
        accountDao.insertAccount(entity)
    }

    suspend fun insertGoal(name: String, targetAmountRupees: Double, icon: String, deadline: String) {
        val entity = GoalEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            targetAmountMinor = (targetAmountRupees * 100).toLong(),
            currentSavedMinor = 0L,
            deadline = deadline.ifEmpty { "2026-12-31" },
            icon = icon.ifEmpty { "🎯" },
            linkedAccountId = "acc-1"
        )
        goalDao.insertGoal(entity)
    }

    suspend fun deleteTx(id: String) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun deleteSms(id: String) {
        smsQueueDao.deleteSmsItem(id)
    }

    suspend fun performChangeCategoryAndAccept(smsId: String, newCategoryId: String) {
        val queueItems = smsQueueDao.getSmsQueueList()
        val sms = queueItems.find { it.id == smsId } ?: return
        val learningManager = userLearnedMappingDao?.let {
            com.example.mstrackerapp.parser.classifier.CategoryLearningManager(it, merchantDao, merchantAliasDao)
        }
        val processor = com.example.mstrackerapp.data.parser.InboxQueueProcessor(smsQueueDao, transactionDao, learningManager)
        processor.changeCategoryAndAccept(sms, newCategoryId)
    }

    suspend fun performAcceptSmsItem(smsId: String) {
        val queueItems = smsQueueDao.getSmsQueueList()
        val sms = queueItems.find { it.id == smsId } ?: return
        val learningManager = userLearnedMappingDao?.let {
            com.example.mstrackerapp.parser.classifier.CategoryLearningManager(it, merchantDao, merchantAliasDao)
        }
        val processor = com.example.mstrackerapp.data.parser.InboxQueueProcessor(smsQueueDao, transactionDao, learningManager)
        processor.acceptItem(sms)
    }

    override fun addTransaction(type: TransactionType, amountRupees: Double, accountId: String, categoryId: String, merchant: String, note: String) {}
    override fun updateTransaction(id: String, merchant: String, categoryId: String, amountRupees: Double, accountId: String, note: String, date: String, type: TransactionType) {
        scope.launch {
            val existing = transactionDao.getTransaction(id) ?: return@launch
            val updatedMerchant = merchant.ifBlank { existing.merchant }
            transactionDao.updateTransaction(existing.copy(
                type = type.name, merchant = updatedMerchant, categoryId = categoryId,
                amountMinor = (amountRupees * 100).toLong(), accountId = accountId,
                note = note, date = date, isManual = true, updatedAt = System.currentTimeMillis()
            ))
            // Learn from ledger edits so future SMS auto-classify
            if (existing.categoryId != categoryId || existing.merchant != updatedMerchant) {
                userLearnedMappingDao?.let {
                    com.example.mstrackerapp.parser.classifier.CategoryLearningManager(it, merchantDao, merchantAliasDao)
                        .onUserChangedCategory(updatedMerchant, categoryId)
                }
            }
        }
    }
    override fun addAccount(name: String, type: AccountType, startingBalanceRupees: Double, icon: String) {}
    override fun addGoal(name: String, targetAmountRupees: Double, icon: String, deadline: String) {}
    override fun acceptSmsItem(smsId: String) {
        scope.launch {
            performAcceptSmsItem(smsId)
        }
    }
    override fun changeCategoryAndAccept(smsId: String, newCategoryId: String) {
        scope.launch {
            performChangeCategoryAndAccept(smsId, newCategoryId)
        }
    }
    override fun ignoreSms(smsId: String) {
        scope.launch {
            deleteSms(smsId)
        }
    }
    override fun deleteSmsItem(smsId: String) {
        scope.launch {
            deleteSms(smsId)
        }
    }
    override fun deleteTransaction(transactionId: String) {}
    override fun updateCategoryLimit(categoryId: String, limitRupees: Double) {
        scope.launch {
            categoryDao.updateCategoryLimit(categoryId, (limitRupees * 100).toLong())
        }
    }

    override fun addCategory(name: String, icon: String): String {
        val catId = "cat-" + java.util.UUID.randomUUID().toString().take(8)
        val safeIcon = com.example.mstrackerapp.utils.CategoryIcons.sanitizeForStorage(icon, name)
        scope.launch {
            categoryDao.insertCategory(
                com.example.mstrackerapp.data.database.entities.CategoryEntity(
                    id = catId,
                    name = name.trim().ifEmpty { "New Category" },
                    icon = safeIcon,
                    color = "#3B7A57",
                    monthlyLimitMinor = 0L,
                    order = 99
                )
            )
        }
        return catId
    }

    suspend fun deleteCategoryById(id: String) {
        categoryDao.deleteCategory(id)
    }

    override fun deleteCategory(categoryId: String) {
        scope.launch {
            categoryDao.deleteCategory(categoryId)
        }
    }

    override fun togglePrivacyMask() {}
}
