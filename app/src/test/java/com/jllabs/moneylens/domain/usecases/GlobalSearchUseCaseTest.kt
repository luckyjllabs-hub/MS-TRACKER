package com.jllabs.moneylens.domain.usecases

import com.jllabs.moneylens.domain.models.Account
import com.jllabs.moneylens.domain.models.AccountType
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchUseCaseTest {

    private val useCase = GlobalSearchUseCase()

    private val sampleCategories = listOf(
        Category("cat-1", "Food", "🍔", "#FF6B6B"),
        Category("cat-2", "Transport", "🚗", "#4ECDC4")
    )

    private val sampleAccounts = listOf(
        Account("acc-1", "HDFC Savings", AccountType.BANK, "HDFC Bank", 100000L, "🏦")
    )

    private val sampleTransactions = listOf(
        Transaction("tx-1", TransactionType.EXPENSE, 45000L, "acc-1", null, "cat-1", "Starbucks", "2026-08-01", "14:30", "Iced Latte"),
        Transaction("tx-2", TransactionType.EXPENSE, 123000L, "acc-1", null, "cat-2", "Uber", "2026-08-01", "11:15", "Ride to office")
    )

    @Test
    fun testGlobalSearchMerchant() {
        val results = useCase("Starbucks", sampleTransactions, sampleCategories, sampleAccounts)
        assertEquals(1, results.size)
        assertEquals("Starbucks", results[0].transaction.merchant)
        assertTrue(results[0].matchedField.contains("Merchant"))
    }

    @Test
    fun testGlobalSearchAmount() {
        val results = useCase("450", sampleTransactions, sampleCategories, sampleAccounts)
        assertEquals(1, results.size)
        assertEquals("Starbucks", results[0].transaction.merchant)
    }

    @Test
    fun testGlobalSearchCategory() {
        val results = useCase("Transport", sampleTransactions, sampleCategories, sampleAccounts)
        assertEquals(1, results.size)
        assertEquals("Uber", results[0].transaction.merchant)
    }

    @Test
    fun testGlobalSearchBank() {
        val results = useCase("HDFC", sampleTransactions, sampleCategories, sampleAccounts)
        assertEquals(2, results.size)
    }

    @Test
    fun testGlobalSearchDate() {
        val results = useCase("2026-08-01", sampleTransactions, sampleCategories, sampleAccounts)
        assertEquals(2, results.size)
    }

    @Test
    fun testGlobalSearchNote() {
        val results = useCase("Latte", sampleTransactions, sampleCategories, sampleAccounts)
        assertEquals(1, results.size)
        assertEquals("Starbucks", results[0].transaction.merchant)
    }
}
