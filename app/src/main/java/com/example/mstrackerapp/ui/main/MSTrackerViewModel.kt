package com.example.mstrackerapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mstrackerapp.data.*
import kotlinx.coroutines.flow.*

enum class AppTab {
    OVERVIEW, TRANSACTIONS, GOALS, ACCOUNTS, SMS_INBOX
}

data class MSTrackerUiState(
    val activeTab: AppTab = AppTab.OVERVIEW,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val smsQueue: List<SmsQueueItem> = emptyList(),
    val netWorthMinor: Long = 0L,
    val totalIncomeMinor: Long = 0L,
    val totalExpenseMinor: Long = 0L,
    val isPrivacyMasked: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: String = "All"
)

class MSTrackerViewModel(private val repository: MSTrackerRepository) : ViewModel() {

    private val _activeTab = MutableStateFlow(AppTab.OVERVIEW)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("All")

    private val dataFlow = combine(
        repository.accounts,
        repository.categories,
        repository.transactions,
        repository.goals,
        repository.smsQueue
    ) { accounts, categories, transactions, goals, smsQueue ->
        DataSnapshot(accounts, categories, transactions, goals, smsQueue)
    }

    private val userControlsFlow = combine(
        _activeTab,
        repository.isPrivacyMasked,
        _searchQuery,
        _selectedFilter
    ) { activeTab, isPrivacyMasked, query, filter ->
        UserControls(activeTab, isPrivacyMasked, query, filter)
    }

    val uiState: StateFlow<MSTrackerUiState> = combine(
        dataFlow,
        userControlsFlow
    ) { data, controls ->
        val totalIncome = data.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val totalExpense = data.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }

        val startingBalances = data.accounts.filter { it.includeInNetWorth }.sumOf { it.startingBalanceMinor }
        val netWorth = startingBalances + totalIncome - totalExpense

        val filteredTx = data.transactions.filter { tx ->
            val matchesQuery = tx.merchant.contains(controls.query, ignoreCase = true) ||
                    tx.note.contains(controls.query, ignoreCase = true) ||
                    tx.date.contains(controls.query, ignoreCase = true)

            val matchesFilter = when (controls.filter) {
                "Expense" -> tx.type == TransactionType.EXPENSE
                "Income" -> tx.type == TransactionType.INCOME
                "Transfer" -> tx.type == TransactionType.TRANSFER
                else -> true
            }

            matchesQuery && matchesFilter
        }

        MSTrackerUiState(
            activeTab = controls.activeTab,
            accounts = data.accounts,
            categories = data.categories,
            transactions = filteredTx,
            goals = data.goals,
            smsQueue = data.smsQueue,
            netWorthMinor = netWorth,
            totalIncomeMinor = totalIncome,
            totalExpenseMinor = totalExpense,
            isPrivacyMasked = controls.isPrivacyMasked,
            searchQuery = controls.query,
            selectedFilter = controls.filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MSTrackerUiState())

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelect(filter: String) {
        _selectedFilter.value = filter
    }

    fun togglePrivacyMask() {
        repository.togglePrivacyMask()
    }

    fun addTransaction(type: TransactionType, amountRupees: Double, accountId: String, categoryId: String, merchant: String, note: String) {
        repository.addTransaction(type, amountRupees, accountId, categoryId, merchant, note)
    }

    fun addAccount(name: String, type: AccountType, balanceRupees: Double, icon: String) {
        repository.addAccount(name, type, balanceRupees, icon)
    }

    fun addGoal(name: String, targetRupees: Double, icon: String, deadline: String) {
        repository.addGoal(name, targetRupees, icon, deadline)
    }

    fun acceptSms(smsId: String) {
        repository.acceptSmsItem(smsId)
    }

    fun deleteTransaction(transactionId: String) {
        repository.deleteTransaction(transactionId)
    }

    private data class DataSnapshot(
        val accounts: List<Account>,
        val categories: List<Category>,
        val transactions: List<Transaction>,
        val goals: List<Goal>,
        val smsQueue: List<SmsQueueItem>
    )

    private data class UserControls(
        val activeTab: AppTab,
        val isPrivacyMasked: Boolean,
        val query: String,
        val filter: String
    )
}
