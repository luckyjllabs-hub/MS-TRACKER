package com.example.mstrackerapp.presentation.screens

import com.example.mstrackerapp.data.DefaultMSTrackerRepository
import com.example.mstrackerapp.domain.models.AccountType
import com.example.mstrackerapp.domain.models.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

import com.example.mstrackerapp.presentation.navigation.AppTab

@OptIn(ExperimentalCoroutinesApi::class)
class MSTrackerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DefaultMSTrackerRepository
    private lateinit var viewModel: MSTrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = DefaultMSTrackerRepository(null)
        viewModel = MSTrackerViewModel(repository)
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
