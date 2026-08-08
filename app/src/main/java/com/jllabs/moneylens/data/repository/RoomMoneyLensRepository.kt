package com.jllabs.moneylens.data.repository

import com.jllabs.moneylens.data.MoneyLensRepository
import com.jllabs.moneylens.data.database.dao.*
import com.jllabs.moneylens.data.database.entities.*
import com.jllabs.moneylens.data.local.preferences.UserPreferencesRepository
import com.jllabs.moneylens.domain.models.*
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

class RoomMoneyLensRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
    private val smsQueueDao: SMSQueueDao,
    private val userPrefs: UserPreferencesRepository,
    private val userLearnedMappingDao: UserLearnedMappingDao? = null,
    private val merchantDao: MerchantDao? = null,
    private val merchantAliasDao: MerchantAliasDao? = null
) : MoneyLensRepository {

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

    override val isDarkMode: StateFlow<Boolean>
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
            com.jllabs.moneylens.parser.classifier.CategoryLearningManager(it, merchantDao, merchantAliasDao)
        }
        val processor = com.jllabs.moneylens.data.parser.InboxQueueProcessor(smsQueueDao, transactionDao, learningManager)
        processor.changeCategoryAndAccept(sms, newCategoryId)
    }

    suspend fun performAcceptSmsItem(smsId: String) {
        val queueItems = smsQueueDao.getSmsQueueList()
        val sms = queueItems.find { it.id == smsId } ?: return
        val learningManager = userLearnedMappingDao?.let {
            com.jllabs.moneylens.parser.classifier.CategoryLearningManager(it, merchantDao, merchantAliasDao)
        }
        val processor = com.jllabs.moneylens.data.parser.InboxQueueProcessor(smsQueueDao, transactionDao, learningManager)
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
                    com.jllabs.moneylens.parser.classifier.CategoryLearningManager(it, merchantDao, merchantAliasDao)
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
        val safeIcon = com.jllabs.moneylens.utils.CategoryIcons.sanitizeForStorage(icon, name)
        scope.launch {
            categoryDao.insertCategory(
                com.jllabs.moneylens.data.database.entities.CategoryEntity(
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

    override fun setDarkMode(enabled: Boolean) {
        scope.launch { userPrefs.setDarkMode(enabled) }
    }

    /**
     * One-shot repair: NEFT/IMPS credits wrongly stored as TRANSFER (recent over-classification)
     * are restored to INCOME/EXPENSE so they show in Overview again.
     */
    suspend fun repairOverclassifiedTransfers() {
        val selfTransfer = Regex(
            """(?i)\b(?:self\s+transfer|transfer\s+to\s+self|own\s+account\s+transfer)\b"""
        )
        val rows = transactionDao.getTransactionsByType(TransactionType.TRANSFER.name)
        for (row in rows) {
            val body = row.rawSms.ifBlank { row.note }
            if (body.isBlank()) continue
            if (selfTransfer.containsMatchIn(body)) continue
            val lower = body.lowercase()
            val fixedType = when {
                lower.contains("credited") || lower.contains("received") ||
                    lower.contains("deposit") -> TransactionType.INCOME.name
                lower.contains("debited") || lower.contains("withdrawn") ||
                    lower.contains("spent") -> TransactionType.EXPENSE.name
                else -> continue
            }
            transactionDao.updateTransaction(
                row.copy(type = fixedType, updatedAt = System.currentTimeMillis())
            )
        }
    }

    override fun importBackup(
        payload: com.jllabs.moneylens.utils.MoneyLensBackupPayload,
        onDone: (Int) -> Unit
    ) {
        scope.launch {
            val count = importBackupPayload(payload)
            // Caller may update UI; keep on same dispatcher as other repo callbacks.
            onDone(count)
        }
    }

    suspend fun importBackupPayload(payload: com.jllabs.moneylens.utils.MoneyLensBackupPayload): Int {
        var imported = 0
        for (cat in payload.categories) {
            if (cat.id.isBlank()) continue
            categoryDao.insertCategory(CategoryEntity.fromDomain(cat))
        }
        for (acc in payload.accounts) {
            if (acc.id.isBlank()) continue
            accountDao.insertAccount(
                AccountEntity(
                    id = acc.id,
                    name = acc.name,
                    type = acc.type.name,
                    institution = acc.institution,
                    startingBalanceMinor = acc.startingBalanceMinor,
                    icon = acc.icon,
                    includeInNetWorth = acc.includeInNetWorth,
                    isArchived = acc.isArchived,
                    order = acc.order
                )
            )
        }
        for (tx in payload.transactions) {
            if (tx.amountMinor <= 0L && tx.type != TransactionType.JUST_INFO) continue
            val existing = transactionDao.getTransaction(tx.id)
            if (existing == null) {
                val dup = if (tx.rawSms.isNotBlank()) {
                    transactionDao.countDuplicateTransaction(tx.rawSms, tx.amountMinor, tx.date) > 0
                } else {
                    transactionDao.countByMerchantAmountDate(tx.merchant, tx.amountMinor, tx.date) > 0
                }
                if (dup) continue
            }
            transactionDao.insertTransaction(
                TransactionEntity(
                    id = tx.id.ifBlank { UUID.randomUUID().toString() },
                    type = tx.type.name,
                    amountMinor = tx.amountMinor,
                    accountId = tx.accountId.ifBlank { "acc-1" },
                    toAccountId = tx.toAccountId,
                    categoryId = tx.categoryId.ifBlank { "cat-14" },
                    merchant = tx.merchant.ifBlank { "Imported" },
                    bankName = tx.bankName,
                    accountLast4 = tx.accountLast4,
                    referenceNumber = tx.referenceNumber,
                    date = tx.date,
                    time = tx.time.ifBlank { "12:00" },
                    note = tx.note,
                    source = tx.source.ifBlank { "IMPORT" },
                    status = tx.status,
                    confidence = tx.confidence,
                    isManual = tx.isManual,
                    isReviewed = tx.isReviewed,
                    rawSms = tx.rawSms,
                    availableBalanceMinor = tx.availableBalance,
                    createdAt = tx.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            imported++
        }
        return imported
    }
}
