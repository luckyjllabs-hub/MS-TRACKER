package com.example.mstrackerapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface MSTrackerRepository {
    val accounts: StateFlow<List<Account>>
    val categories: StateFlow<List<Category>>
    val transactions: StateFlow<List<Transaction>>
    val goals: StateFlow<List<Goal>>
    val smsQueue: StateFlow<List<SmsQueueItem>>
    val isPrivacyMasked: StateFlow<Boolean>

    fun addTransaction(type: TransactionType, amountRupees: Double, accountId: String, categoryId: String, merchant: String, note: String)
    fun addAccount(name: String, type: AccountType, startingBalanceRupees: Double, icon: String)
    fun addGoal(name: String, targetAmountRupees: Double, icon: String, deadline: String)
    fun acceptSmsItem(smsId: String)
    fun deleteTransaction(transactionId: String)
    fun togglePrivacyMask()
}

class DefaultMSTrackerRepository : MSTrackerRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val todayStr: String get() = dateFormat.format(Date())
    private val currentTimeStr: String get() = timeFormat.format(Date())

    private val _accounts = MutableStateFlow(
        listOf(
            Account("acc-1", "HDFC Bank", AccountType.BANK, "HDFC", 245000L, "🏦", includeInNetWorth = true, order = 1),
            Account("acc-2", "Cash Wallet", AccountType.CASH, "Cash", 12000L, "💵", includeInNetWorth = true, order = 2),
            Account("acc-3", "Credit Card", AccountType.CREDIT_CARD, "ICICI", -35000L, "💳", includeInNetWorth = true, order = 3)
        )
    )

    private val _categories = MutableStateFlow(
        listOf(
            Category("cat-1", "Food & Drink", "🍔", 500000L, 1),
            Category("cat-2", "Transport", "🚗", 300000L, 2),
            Category("cat-3", "Shopping", "🛍️", 400000L, 3),
            Category("cat-4", "Entertainment", "🎬", 200000L, 4),
            Category("cat-5", "College", "🎓", 250000L, 5),
            Category("cat-6", "Subscriptions", "📺", 100000L, 6),
            Category("cat-7", "Health", "🏥", 150000L, 7),
            Category("cat-8", "Other", "📦", 0L, 8)
        )
    )

    private val _transactions = MutableStateFlow(
        listOf(
            Transaction("tx-1", TransactionType.EXPENSE, 45000L, "acc-1", null, "cat-1", "Starbucks", todayStr, "14:30", "Iced Latte", "SMS Detected"),
            Transaction("tx-2", TransactionType.EXPENSE, 123000L, "acc-3", null, "cat-2", "Uber", todayStr, "11:15", "Ride downtown", "Manual"),
            Transaction("tx-3", TransactionType.EXPENSE, 850000L, "acc-1", null, "cat-3", "Amazon", todayStr, "18:20", "Beach gear", "Manual"),
            Transaction("tx-4", TransactionType.INCOME, 30000000L, "acc-1", null, "cat-8", "Payroll Inc", todayStr, "09:00", "Monthly Salary", "Manual")
        )
    )

    private val _goals = MutableStateFlow(
        listOf(
            Goal("goal-1", "Goa Trip", 10000000L, 4500000L, "2026-10-15", "🏖️", "acc-1"),
            Goal("goal-2", "New Phone", 8000000L, 3200000L, "2026-11-30", "📱", "acc-1")
        )
    )

    private val _smsQueue = MutableStateFlow(
        listOf(
            SmsQueueItem("sms-1", "Alert: Spend of INR 450.00 on Food at Starbucks card 1234", "HDFC Bank", 45000L, "Starbucks", "cat-1", "acc-1"),
            SmsQueueItem("sms-2", "Txn: INR 280.00 debited for Uber ride on ICICI Card 5678", "ICICI Bank", 28000L, "Uber", "cat-2", "acc-3"),
            SmsQueueItem("sms-3", "Alert: INR 620.00 spent at Swiggy on HDFC Card 1234", "HDFC Bank", 62000L, "Swiggy", "cat-1", "acc-1")
        )
    )

    private val _isPrivacyMasked = MutableStateFlow(false)

    override val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    override val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    override val goals: StateFlow<List<Goal>> = _goals.asStateFlow()
    override val smsQueue: StateFlow<List<SmsQueueItem>> = _smsQueue.asStateFlow()
    override val isPrivacyMasked: StateFlow<Boolean> = _isPrivacyMasked.asStateFlow()

    override fun addTransaction(
        type: TransactionType,
        amountRupees: Double,
        accountId: String,
        categoryId: String,
        merchant: String,
        note: String
    ) {
        val amountMinor = (amountRupees * 100).toLong()
        val newTx = Transaction(
            id = UUID.randomUUID().toString(),
            type = type,
            amountMinor = amountMinor,
            accountId = accountId,
            toAccountId = null,
            categoryId = categoryId,
            merchant = merchant.ifEmpty { "Transaction" },
            date = todayStr,
            time = currentTimeStr,
            note = note,
            source = "Manual"
        )
        _transactions.update { listOf(newTx) + it }
    }

    override fun addAccount(name: String, type: AccountType, startingBalanceRupees: Double, icon: String) {
        val newAcc = Account(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            institution = name,
            startingBalanceMinor = (startingBalanceRupees * 100).toLong(),
            icon = icon.ifEmpty { "🏦" },
            includeInNetWorth = true,
            order = _accounts.value.size + 1
        )
        _accounts.update { _accounts.value + newAcc }
    }

    override fun addGoal(name: String, targetAmountRupees: Double, icon: String, deadline: String) {
        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            name = name,
            targetAmountMinor = (targetAmountRupees * 100).toLong(),
            currentSavedMinor = 0L,
            deadline = deadline.ifEmpty { "2026-12-31" },
            icon = icon.ifEmpty { "🎯" },
            linkedAccountId = _accounts.value.firstOrNull()?.id ?: "acc-1"
        )
        _goals.update { _goals.value + newGoal }
    }

    override fun acceptSmsItem(smsId: String) {
        val sms = _smsQueue.value.find { it.id == smsId } ?: return
        addTransaction(
            type = TransactionType.EXPENSE,
            amountRupees = sms.amountMinor / 100.0,
            accountId = sms.suggestedAccountId,
            categoryId = sms.suggestedCategoryId,
            merchant = sms.merchant,
            note = "Recorded from ${sms.bank} SMS"
        )
        _smsQueue.update { list -> list.filterNot { it.id == smsId } }
    }

    override fun deleteTransaction(transactionId: String) {
        _transactions.update { list -> list.filterNot { it.id == transactionId } }
    }

    override fun togglePrivacyMask() {
        _isPrivacyMasked.update { !it }
    }
}
