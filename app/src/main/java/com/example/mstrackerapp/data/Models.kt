package com.example.mstrackerapp.data

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

enum class AccountType {
    BANK, CASH, CREDIT_CARD, WALLET, SAVINGS
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val institution: String,
    val startingBalanceMinor: Long,
    val icon: String,
    val includeInNetWorth: Boolean = true,
    val isArchived: Boolean = false,
    val order: Int = 0
)

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val monthlyLimitMinor: Long = 0L,
    val order: Int = 0
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String,
    val merchant: String,
    val date: String,
    val time: String,
    val note: String = "",
    val source: String = "Manual",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class Goal(
    val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentSavedMinor: Long,
    val deadline: String,
    val icon: String,
    val linkedAccountId: String,
    val isCompleted: Boolean = false
)

data class SmsQueueItem(
    val id: String,
    val rawText: String,
    val bank: String,
    val amountMinor: Long,
    val merchant: String,
    val suggestedCategoryId: String,
    val suggestedAccountId: String,
    val confidence: String = "High Confidence",
    val timestamp: Long = System.currentTimeMillis()
)
