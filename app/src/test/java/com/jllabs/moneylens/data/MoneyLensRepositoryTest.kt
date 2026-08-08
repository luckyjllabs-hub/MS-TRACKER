package com.jllabs.moneylens.data

import com.jllabs.moneylens.domain.models.AccountType
import com.jllabs.moneylens.domain.models.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyLensRepositoryTest {

    @Test
    fun testDefaultRepositoryInMemoryState() = runBlocking {
        val repo = DefaultMoneyLensRepository(null)

        val accounts = repo.accounts.first()
        val categories = repo.categories.first()
        val transactions = repo.transactions.first()
        val goals = repo.goals.first()
        val smsQueue = repo.smsQueue.first()

        assertEquals(3, accounts.size)
        assertEquals(8, categories.size)
        assertEquals(4, transactions.size)
        assertEquals(2, goals.size)
        assertEquals(3, smsQueue.size)
    }

    @Test
    fun testAddTransaction() = runBlocking {
        val repo = DefaultMoneyLensRepository(null)

        repo.addTransaction(
            type = TransactionType.EXPENSE,
            amountRupees = 500.0,
            accountId = "acc-1",
            categoryId = "cat-1",
            merchant = "Test Merchant",
            note = "Unit test note"
        )

        val transactions = repo.transactions.first()
        assertEquals(5, transactions.size)
        assertEquals("Test Merchant", transactions[0].merchant)
        assertEquals(50000L, transactions[0].amountMinor)
    }

    @Test
    fun testAcceptSmsItem() = runBlocking {
        val repo = DefaultMoneyLensRepository(null)
        val initialSmsCount = repo.smsQueue.first().size
        val initialTxCount = repo.transactions.first().size

        repo.acceptSmsItem("sms-1")

        val newSmsList = repo.smsQueue.first()
        val newTxList = repo.transactions.first()

        assertEquals(initialSmsCount - 1, newSmsList.size)
        assertEquals(initialTxCount + 1, newTxList.size)
    }

    @Test
    fun testDeleteTransaction() = runBlocking {
        val repo = DefaultMoneyLensRepository(null)
        val initialTxCount = repo.transactions.first().size

        repo.deleteTransaction("tx-1")

        val newTxList = repo.transactions.first()
        assertEquals(initialTxCount - 1, newTxList.size)
    }

    @Test
    fun testTogglePrivacyMask() = runBlocking {
        val repo = DefaultMoneyLensRepository(null)
        val initialMask = repo.isPrivacyMasked.first()

        repo.togglePrivacyMask()
        val newMask = repo.isPrivacyMasked.first()

        assertEquals(!initialMask, newMask)
    }
}
