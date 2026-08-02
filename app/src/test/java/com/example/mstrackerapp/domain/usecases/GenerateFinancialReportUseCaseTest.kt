package com.example.mstrackerapp.domain.usecases

import com.example.mstrackerapp.domain.models.Account
import com.example.mstrackerapp.domain.models.AccountType
import com.example.mstrackerapp.domain.models.Category
import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateFinancialReportUseCaseTest {

    private val useCase = GenerateFinancialReportUseCase()

    private val sampleCategories = listOf(
        Category("cat-1", "Food", "🍔", "#FF6B6B"),
        Category("cat-2", "Transport", "🚗", "#4ECDC4")
    )

    private val sampleAccounts = listOf(
        Account("acc-1", "HDFC Bank", AccountType.BANK, "HDFC", 100000L, "🏦")
    )

    private val sampleTransactions = listOf(
        Transaction("tx-1", TransactionType.EXPENSE, 45000L, "acc-1", null, "cat-1", "Starbucks", "2026-08-01", "14:30"),
        Transaction("tx-2", TransactionType.EXPENSE, 123000L, "acc-1", null, "cat-2", "Uber", "2026-08-01", "11:15"),
        Transaction("tx-3", TransactionType.INCOME, 30000000L, "acc-1", null, "cat-1", "Payroll", "2026-08-01", "09:00")
    )

    @Test
    fun testReportCalculations() {
        val report = useCase(sampleTransactions, sampleCategories, sampleAccounts, "Monthly")

        assertEquals("Monthly", report.periodName)
        assertEquals(30000000L, report.totalIncomeMinor)
        assertEquals(168000L, report.totalExpenseMinor)
        assertEquals(29832000L, report.netSavingsMinor)

        assertEquals(2, report.categoryBreakdown.size)
        assertEquals(2, report.merchantBreakdown.size)
        assertEquals(1, report.accountBreakdown.size)
    }
}
