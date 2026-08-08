package com.jllabs.moneylens.domain.usecases

import com.jllabs.moneylens.domain.models.Account
import com.jllabs.moneylens.domain.models.AccountType
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFilteredTransactionsUseCaseTest {

    private val useCase = GetFilteredTransactionsUseCase()

    private val sampleAccounts = listOf(
        Account("acc-1", "HDFC Savings", AccountType.BANK, "HDFC Bank", 100000L, "🏦"),
        Account("acc-2", "ICICI Credit", AccountType.CREDIT_CARD, "ICICI Bank", 50000L, "💳")
    )

    private val sampleTransactions = listOf(
        Transaction("tx-1", TransactionType.EXPENSE, 45000L, "acc-1", null, "cat-1", "Starbucks", "2026-08-01", "14:30", "Latte"),
        Transaction("tx-2", TransactionType.EXPENSE, 123000L, "acc-2", null, "cat-2", "Uber", "2026-08-01", "11:15", "Ride"),
        Transaction("tx-3", TransactionType.EXPENSE, 850000L, "acc-1", null, "cat-3", "Amazon", "2026-07-15", "18:20", "Books")
    )

    @Test
    fun testFilterByMerchantSearch() {
        val result = useCase(sampleTransactions, "Starbucks", "All", sampleAccounts)
        assertEquals(1, result.size)
        assertEquals("Starbucks", result[0].merchant)
    }

    @Test
    fun testFilterByBankSearch() {
        val result = useCase(sampleTransactions, "HDFC", "All", sampleAccounts)
        assertEquals(2, result.size)
    }

    @Test
    fun testFilterByAmountSearch() {
        val result = useCase(sampleTransactions, "1230", "All", sampleAccounts)
        assertEquals(1, result.size)
        assertEquals("Uber", result[0].merchant)
    }

    @Test
    fun testFilterByWeekTime() {
        val result = useCase(sampleTransactions, "", "Week", sampleAccounts)
        assertEquals(2, result.size)
    }
}
