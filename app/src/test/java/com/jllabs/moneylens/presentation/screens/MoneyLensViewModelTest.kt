package com.jllabs.moneylens.presentation.screens

import com.jllabs.moneylens.data.DefaultMoneyLensRepository
import com.jllabs.moneylens.domain.models.AccountType
import com.jllabs.moneylens.domain.models.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

import com.jllabs.moneylens.presentation.navigation.AppTab

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyLensViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DefaultMoneyLensRepository
    private lateinit var viewModel: MoneyLensViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = DefaultMoneyLensRepository(null)
        viewModel = MoneyLensViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest {
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(AppTab.OVERVIEW, state.activeTab)
        assertEquals(4, state.transactions.size)
        assertEquals(3, state.accounts.size)
        assertEquals(8, state.categories.size)
        assertNotNull(state.netWorthMinor)
    }

    @Test
    fun testTabSelection() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.selectTab(AppTab.ACCOUNTS)
        testScheduler.advanceUntilIdle()
        assertEquals(AppTab.ACCOUNTS, viewModel.uiState.value.activeTab)
    }

    @Test
    fun testSearchQueryChange() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.onSearchQueryChange("Starbucks")
        testScheduler.advanceUntilIdle()
        assertEquals("Starbucks", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun testFilterSelection() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.onFilterSelect("Month")
        testScheduler.advanceUntilIdle()
        assertEquals("Month", viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun testAddTransactionViewModel() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.addTransaction(
            type = TransactionType.EXPENSE,
            amountRupees = 250.0,
            accountId = "acc-1",
            categoryId = "cat-1",
            merchant = "McDonalds",
            note = "Burger"
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.transactions.size)
        assertEquals("McDonalds", state.transactions[0].merchant)
    }

    @Test
    fun testDeleteTransactionViewModel() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.deleteTransaction("tx-1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.transactions.size)
    }
}
