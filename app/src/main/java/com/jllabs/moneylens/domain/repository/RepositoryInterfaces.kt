package com.jllabs.moneylens.domain.repository

import com.jllabs.moneylens.domain.models.*
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
}

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    suspend fun addAccount(account: Account)
}

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category)
}

interface GoalRepository {
    fun getGoals(): Flow<List<Goal>>
    suspend fun addGoal(goal: Goal)
}

interface SMSQueueRepository {
    fun getSMSQueue(): Flow<List<SmsQueueItem>>
    suspend fun addSmsItem(smsItem: SmsQueueItem)
    suspend fun deleteSmsItem(id: String)
}

interface BudgetRepository {
    fun getBudgets(monthYear: String): Flow<List<BudgetEntityData>>
    suspend fun setBudget(categoryId: String, limitMinor: Long, monthYear: String)
}

data class BudgetEntityData(
    val id: String,
    val categoryId: String,
    val limitMinor: Long,
    val monthYear: String
)
