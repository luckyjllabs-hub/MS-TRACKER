package com.jllabs.moneylens.data

import android.content.Context
import com.jllabs.moneylens.data.database.MoneyLensDatabase
import com.jllabs.moneylens.data.local.preferences.UserPreferencesRepository
import com.jllabs.moneylens.data.repository.RoomMoneyLensRepository
import com.jllabs.moneylens.domain.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface MoneyLensRepository {
    val accounts: StateFlow<List<Account>>
    val categories: StateFlow<List<Category>>
    val transactions: StateFlow<List<Transaction>>
    val goals: StateFlow<List<Goal>>
    val smsQueue: StateFlow<List<SmsQueueItem>>
    val isPrivacyMasked: StateFlow<Boolean>
    val isDarkMode: StateFlow<Boolean>

    fun addTransaction(type: TransactionType, amountRupees: Double, accountId: String, categoryId: String, merchant: String, note: String)
    fun updateTransaction(id: String, merchant: String, categoryId: String, amountRupees: Double, accountId: String, note: String, date: String, type: TransactionType = TransactionType.EXPENSE)
    fun addAccount(name: String, type: AccountType, startingBalanceRupees: Double, icon: String)
    fun addGoal(name: String, targetAmountRupees: Double, icon: String, deadline: String)
    fun acceptSmsItem(smsId: String)
    fun changeCategoryAndAccept(smsId: String, newCategoryId: String)
    fun ignoreSms(smsId: String)
    fun deleteSmsItem(smsId: String)
    fun deleteTransaction(transactionId: String)
    fun updateCategoryLimit(categoryId: String, limitRupees: Double)
    fun addCategory(name: String, icon: String = ""): String
    fun deleteCategory(categoryId: String)
    fun togglePrivacyMask()
    fun setDarkMode(enabled: Boolean)
    fun importBackup(payload: com.jllabs.moneylens.utils.MoneyLensBackupPayload, onDone: (Int) -> Unit = {})
}

class DefaultMoneyLensRepository(context: Context? = null) : MoneyLensRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var roomRepo: RoomMoneyLensRepository? = null
    private var userPrefs: UserPreferencesRepository? = null

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    private val _smsQueue = MutableStateFlow<List<SmsQueueItem>>(emptyList())
    private val _isPrivacyMasked = MutableStateFlow(false)
    private val _isDarkMode = MutableStateFlow(false)

    override val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    override val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    override val goals: StateFlow<List<Goal>> = _goals.asStateFlow()
    override val smsQueue: StateFlow<List<SmsQueueItem>> = _smsQueue.asStateFlow()
    override val isPrivacyMasked: StateFlow<Boolean> = _isPrivacyMasked.asStateFlow()
    override val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        if (context != null) {
            val db = MoneyLensDatabase.getDatabase(context)
            val prefs = UserPreferencesRepository(context)
            val repo = RoomMoneyLensRepository(
                db.transactionDao(),
                db.categoryDao(),
                db.accountDao(),
                db.goalDao(),
                db.smsQueueDao(),
                prefs,
                db.userLearnedMappingDao(),
                db.merchantDao(),
                db.merchantAliasDao()
            )
            this.roomRepo = repo
            this.userPrefs = prefs

            scope.launch {
                try {
                    repo.repairOverclassifiedTransfers()
                } catch (_: Exception) {
                }
            }
            scope.launch {
                repo.accountsFlow.collect { _accounts.value = it }
            }
            scope.launch {
                repo.categoriesFlow.collect { _categories.value = it }
            }
            scope.launch {
                repo.transactionsFlow.collect { _transactions.value = it }
            }
            scope.launch {
                repo.goalsFlow.collect { _goals.value = it }
            }
            scope.launch {
                repo.smsQueueFlow.collect { _smsQueue.value = it }
            }
            scope.launch {
                prefs.isPrivacyMasked.collect { _isPrivacyMasked.value = it }
            }
            scope.launch {
                prefs.isDarkMode.collect { _isDarkMode.value = it }
            }
        } else {
            _accounts.value = mockAccounts
            _categories.value = mockCategories
            _transactions.value = mockTransactions
            _goals.value = mockGoals
            _smsQueue.value = mockSmsQueue
        }
    }

    override fun addTransaction(
        type: TransactionType,
        amountRupees: Double,
        accountId: String,
        categoryId: String,
        merchant: String,
        note: String
    ) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.insertTransaction(type, amountRupees, accountId, categoryId, merchant, note)
            }
        } else {
            val amountMinor = (amountRupees * 100).toLong()
            val newTx = Transaction(
                id = java.util.UUID.randomUUID().toString(),
                type = type,
                amountMinor = amountMinor,
                accountId = accountId,
                toAccountId = null,
                categoryId = categoryId,
                merchant = merchant.ifEmpty { "Transaction" },
                date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                note = note,
                source = "Manual"
            )
            _transactions.value = listOf(newTx) + _transactions.value
        }
    }

    override fun updateTransaction(
        id: String,
        merchant: String,
        categoryId: String,
        amountRupees: Double,
        accountId: String,
        note: String,
        date: String,
        type: TransactionType
    ) {
        if (roomRepo != null) {
            roomRepo?.updateTransaction(id, merchant, categoryId, amountRupees, accountId, note, date, type)
        } else {
            val amountMinor = (amountRupees * 100).toLong()
            _transactions.value = _transactions.value.map { tx ->
                if (tx.id == id) {
                    tx.copy(
                        type = type,
                        merchant = merchant,
                        categoryId = categoryId,
                        amountMinor = amountMinor,
                        accountId = accountId,
                        note = note,
                        date = date
                    )
                } else tx
            }
        }
    }

    override fun addAccount(name: String, type: AccountType, startingBalanceRupees: Double, icon: String) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.insertAccount(name, type, startingBalanceRupees, icon)
            }
        } else {
            val newAcc = Account(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                type = type,
                institution = name,
                startingBalanceMinor = (startingBalanceRupees * 100).toLong(),
                icon = icon.ifEmpty { "🏦" },
                includeInNetWorth = true,
                order = _accounts.value.size + 1
            )
            _accounts.value = _accounts.value + newAcc
        }
    }

    override fun addGoal(name: String, targetAmountRupees: Double, icon: String, deadline: String) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.insertGoal(name, targetAmountRupees, icon, deadline)
            }
        } else {
            val newGoal = Goal(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                targetAmountMinor = (targetAmountRupees * 100).toLong(),
                currentSavedMinor = 0L,
                deadline = deadline.ifEmpty { "2026-12-31" },
                icon = icon.ifEmpty { "🎯" },
                linkedAccountId = "acc-1"
            )
            _goals.value = _goals.value + newGoal
        }
    }

    override fun acceptSmsItem(smsId: String) {
        val sms = _smsQueue.value.find { it.id == smsId }
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.acceptSmsItem(smsId)
            }
        } else if (sms != null) {
            addTransaction(
                type = TransactionType.EXPENSE,
                amountRupees = sms.amountMinor / 100.0,
                accountId = sms.suggestedAccountId,
                categoryId = sms.suggestedCategoryId,
                merchant = sms.merchant,
                note = "Recorded from ${sms.bank} SMS"
            )
            _smsQueue.value = _smsQueue.value.filterNot { it.id == smsId }
        }
    }

    override fun changeCategoryAndAccept(smsId: String, newCategoryId: String) {
        val sms = _smsQueue.value.find { it.id == smsId }
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.changeCategoryAndAccept(smsId, newCategoryId)
            }
        } else if (sms != null) {
            addTransaction(
                type = TransactionType.EXPENSE,
                amountRupees = sms.amountMinor / 100.0,
                accountId = sms.suggestedAccountId,
                categoryId = newCategoryId,
                merchant = sms.merchant,
                note = "Accepted with category update from ${sms.bank} SMS"
            )
            _smsQueue.value = _smsQueue.value.filterNot { it.id == smsId }
        }
    }

    override fun ignoreSms(smsId: String) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.deleteSms(smsId)
            }
        } else {
            _smsQueue.value = _smsQueue.value.filterNot { it.id == smsId }
        }
    }

    override fun deleteSmsItem(smsId: String) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.deleteSms(smsId)
            }
        } else {
            _smsQueue.value = _smsQueue.value.filterNot { it.id == smsId }
        }
    }

    override fun deleteTransaction(transactionId: String) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.deleteTx(transactionId)
            }
        } else {
            _transactions.value = _transactions.value.filterNot { it.id == transactionId }
        }
    }

    override fun updateCategoryLimit(categoryId: String, limitRupees: Double) {
        val limitMinor = (limitRupees * 100).toLong()
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.updateCategoryLimit(categoryId, limitRupees)
            }
        } else {
            _categories.value = _categories.value.map { cat ->
                if (cat.id == categoryId) cat.copy(monthlyLimitMinor = limitMinor) else cat
            }
        }
    }

    override fun addCategory(name: String, icon: String): String {
        val newId = "cat-" + java.util.UUID.randomUUID().toString().take(8)
        val safeIcon = com.jllabs.moneylens.utils.CategoryIcons.sanitizeForStorage(icon, name)
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.addCategory(name, safeIcon)
            }
        } else {
            val newCat = Category(id = newId, name = name, icon = safeIcon, color = "#3B7A57", monthlyLimitMinor = 0L, order = _categories.value.size + 1)
            _categories.value = _categories.value + newCat
        }
        return newId
    }

    override fun deleteCategory(categoryId: String) {
        if (roomRepo != null) {
            scope.launch {
                roomRepo?.deleteCategory(categoryId)
            }
        } else {
            _categories.value = _categories.value.filterNot { it.id == categoryId }
        }
    }

    override fun togglePrivacyMask() {
        if (userPrefs != null) {
            scope.launch {
                userPrefs?.togglePrivacyMask()
            }
        } else {
            _isPrivacyMasked.value = !_isPrivacyMasked.value
        }
    }

    override fun setDarkMode(enabled: Boolean) {
        if (userPrefs != null) {
            scope.launch {
                userPrefs?.setDarkMode(enabled)
            }
        } else {
            _isDarkMode.value = enabled
        }
    }

    override fun importBackup(
        payload: com.jllabs.moneylens.utils.MoneyLensBackupPayload,
        onDone: (Int) -> Unit
    ) {
        if (roomRepo != null) {
            scope.launch {
                val count = roomRepo?.importBackupPayload(payload) ?: 0
                kotlinx.coroutines.withContext(Dispatchers.Main) { onDone(count) }
            }
        } else {
            val existingIds = _transactions.value.map { it.id }.toSet()
            val existingFingerprints = _transactions.value.map {
                "${it.date}|${it.amountMinor}|${it.merchant}|${it.rawSms.take(80)}"
            }.toSet()
            val toAdd = payload.transactions.filter { tx ->
                tx.id !in existingIds &&
                    "${tx.date}|${tx.amountMinor}|${tx.merchant}|${tx.rawSms.take(80)}" !in existingFingerprints
            }
            _transactions.value = toAdd + _transactions.value
            if (payload.categories.isNotEmpty()) {
                val known = _categories.value.map { it.id }.toSet()
                _categories.value = _categories.value + payload.categories.filter { it.id !in known }
            }
            if (payload.accounts.isNotEmpty()) {
                val known = _accounts.value.map { it.id }.toSet()
                _accounts.value = _accounts.value + payload.accounts.filter { it.id !in known }
            }
            onDone(toAdd.size)
        }
    }
}

private val mockAccounts = listOf(
    Account("acc-1", "HDFC Bank", AccountType.BANK, "HDFC", 245000L, "🏦", includeInNetWorth = true, order = 1),
    Account("acc-2", "Cash Wallet", AccountType.CASH, "Cash", 12000L, "💵", includeInNetWorth = true, order = 2),
    Account("acc-3", "Credit Card", AccountType.CREDIT_CARD, "ICICI", -35000L, "💳", includeInNetWorth = true, order = 3)
)

private val mockCategories = listOf(
    Category("cat-1", "Food & Drink", "🍔", "#3B7A57", 500000L, 1),
    Category("cat-2", "Transport", "🚗", "#2E5B88", 300000L, 2),
    Category("cat-3", "Shopping", "🛍️", "#D87D56", 400000L, 3),
    Category("cat-4", "Entertainment", "🎬", "#8E44AD", 200000L, 4),
    Category("cat-5", "College", "🎓", "#27AE60", 250000L, 5),
    Category("cat-6", "Subscriptions", "📺", "#2980B9", 100000L, 6),
    Category("cat-7", "Health", "🏥", "#C0392B", 150000L, 7),
    Category("cat-8", "Other", "📦", "#BDC3C7", 0L, 8)
)

private val mockTransactions = listOf(
    Transaction("tx-1", TransactionType.EXPENSE, 45000L, "acc-1", null, "cat-1", "Starbucks", "2026-08-01", "14:30", "Iced Latte", "SMS Detected"),
    Transaction("tx-2", TransactionType.EXPENSE, 123000L, "acc-3", null, "cat-2", "Uber", "2026-08-01", "11:15", "Ride downtown", "Manual"),
    Transaction("tx-3", TransactionType.EXPENSE, 850000L, "acc-1", null, "cat-3", "Amazon", "2026-08-01", "18:20", "Beach gear", "Manual"),
    Transaction("tx-4", TransactionType.INCOME, 30000000L, "acc-1", null, "cat-8", "Payroll Inc", "2026-08-01", "09:00", "Monthly Salary", "Manual")
)

private val mockGoals = listOf(
    Goal("goal-1", "Goa Trip", 10000000L, 4500000L, "2026-10-15", "🏖️", "acc-1"),
    Goal("goal-2", "New Phone", 8000000L, 3200000L, "2026-11-30", "📱", "acc-1")
)

private val mockSmsQueue = listOf(
    SmsQueueItem("sms-1", "Alert: Spend of INR 450.00 on Food at Starbucks card 1234", "HDFC Bank", 45000L, "Starbucks", "cat-1", "acc-1"),
    SmsQueueItem("sms-2", "Txn: INR 280.00 debited for Uber ride on ICICI Card 5678", "ICICI Bank", 28000L, "Uber", "cat-2", "acc-3"),
    SmsQueueItem("sms-3", "Alert: INR 620.00 spent at Swiggy on HDFC Card 1234", "HDFC Bank", 62000L, "Swiggy", "cat-1", "acc-1")
)
