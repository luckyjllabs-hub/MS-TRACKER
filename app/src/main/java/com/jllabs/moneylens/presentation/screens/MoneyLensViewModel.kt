package com.jllabs.moneylens.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jllabs.moneylens.data.MoneyLensRepository
import com.jllabs.moneylens.domain.accounts.SmsAccountAggregator
import com.jllabs.moneylens.domain.accounts.SmsAccountRow
import com.jllabs.moneylens.domain.models.*
import com.jllabs.moneylens.domain.usecases.CalculateNetWorthUseCase
import com.jllabs.moneylens.domain.usecases.GetFilteredTransactionsUseCase
import com.jllabs.moneylens.presentation.navigation.AppTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

data class MoneyLensUiState(
    val activeTab: AppTab = AppTab.OVERVIEW,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    /** Unfiltered ledger — Finance accounts must use this, not date-filtered [transactions]. */
    val allTransactions: List<Transaction> = emptyList(),
    /** Precomputed SMS accounts (background thread) — use this instead of derive() in Compose. */
    val smsAccounts: List<SmsAccountRow> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val smsQueue: List<SmsQueueItem> = emptyList(),
    val netWorthMinor: Long = 0L,
    val totalIncomeMinor: Long = 0L,
    val totalExpenseMinor: Long = 0L,
    val isPrivacyMasked: Boolean = false,
    val isDarkMode: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: String = "This Month",
    val customStartDate: String = "",
    val customEndDate: String = ""
)

class MoneyLensViewModel(private val repository: MoneyLensRepository) : ViewModel() {

    private val calculateNetWorthUseCase = CalculateNetWorthUseCase()
    private val getFilteredTransactionsUseCase = GetFilteredTransactionsUseCase()

    private val _activeTab = MutableStateFlow(AppTab.OVERVIEW)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("This Month")
    private val _customStartDate = MutableStateFlow("")
    private val _customEndDate = MutableStateFlow("")

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
        repository.isDarkMode,
        _searchQuery,
        _selectedFilter
    ) { activeTab, isPrivacyMasked, isDarkMode, query, filter ->
        UserControls(activeTab, isPrivacyMasked, isDarkMode, query, filter)
    }.combine(
        combine(_customStartDate, _customEndDate) { start, end -> start to end }
    ) { controls, range ->
        controls.copy(customStart = range.first, customEnd = range.second)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val smsAccountsFlow: StateFlow<List<SmsAccountRow>> = repository.transactions
        .mapLatest { txs ->
            withContext(Dispatchers.Default) { SmsAccountAggregator.derive(txs) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<MoneyLensUiState> = combine(
        dataFlow,
        userControlsFlow,
        smsAccountsFlow
    ) { data, controls, smsAccounts ->
        val filteredTx = getFilteredTransactionsUseCase(
            data.transactions,
            controls.query,
            controls.filter,
            data.accounts,
            controls.customStart.takeIf { it.isNotBlank() },
            controls.customEnd.takeIf { it.isNotBlank() }
        )

        val (totalIncome, totalExpense) =
            com.jllabs.moneylens.domain.accounts.InternalTransferClassifier.incomeExpenseTotals(filteredTx)
        val netWorth = calculateNetWorthUseCase(data.accounts, filteredTx)

        MoneyLensUiState(
            activeTab = controls.activeTab,
            accounts = data.accounts,
            categories = data.categories,
            transactions = filteredTx,
            allTransactions = data.transactions,
            smsAccounts = smsAccounts,
            goals = data.goals,
            smsQueue = data.smsQueue,
            netWorthMinor = netWorth,
            totalIncomeMinor = totalIncome,
            totalExpenseMinor = totalExpense,
            isPrivacyMasked = controls.isPrivacyMasked,
            isDarkMode = controls.isDarkMode,
            searchQuery = controls.query,
            selectedFilter = controls.filter,
            customStartDate = controls.customStart,
            customEndDate = controls.customEnd
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MoneyLensUiState())

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelect(filter: String) {
        _selectedFilter.value = filter
    }

    fun onCustomDateRangeChange(startIso: String, endIso: String) {
        _customStartDate.value = startIso
        _customEndDate.value = endIso
        if (startIso.isNotBlank() || endIso.isNotBlank()) {
            _selectedFilter.value = "Custom"
        }
    }

    fun togglePrivacyMask() {
        repository.togglePrivacyMask()
    }

    fun setDarkMode(enabled: Boolean) {
        repository.setDarkMode(enabled)
    }

    fun addTransaction(type: TransactionType, amountRupees: Double, accountId: String, categoryId: String, merchant: String, note: String) {
        repository.addTransaction(type, amountRupees, accountId, categoryId, merchant, note)
    }

    fun updateTransaction(id: String, merchant: String, categoryId: String, amountRupees: Double, accountId: String, note: String, date: String, type: TransactionType = TransactionType.EXPENSE) {
        repository.updateTransaction(id, merchant, categoryId, amountRupees, accountId, note, date, type)
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

    fun changeCategoryAndAccept(smsId: String, newCategoryId: String) {
        repository.changeCategoryAndAccept(smsId, newCategoryId)
    }

    fun ignoreSms(smsId: String) {
        repository.ignoreSms(smsId)
    }

    fun deleteSmsItem(smsId: String) {
        repository.deleteSmsItem(smsId)
    }

    fun deleteTransaction(transactionId: String) {
        repository.deleteTransaction(transactionId)
    }

    fun updateCategoryLimit(categoryId: String, limitRupees: Double) {
        repository.updateCategoryLimit(categoryId, limitRupees)
    }

    fun addCategory(name: String, icon: String = ""): String {
        val safeName = name.trim()
        val safeIcon = com.jllabs.moneylens.utils.CategoryIcons.sanitizeForStorage(icon, safeName)
        return repository.addCategory(safeName, safeIcon)
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
    }

    fun importBackup(payload: com.jllabs.moneylens.utils.MoneyLensBackupPayload, onDone: (Int) -> Unit = {}) {
        repository.importBackup(payload, onDone)
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
        val isDarkMode: Boolean,
        val query: String,
        val filter: String,
        val customStart: String = "",
        val customEnd: String = ""
    )
}

