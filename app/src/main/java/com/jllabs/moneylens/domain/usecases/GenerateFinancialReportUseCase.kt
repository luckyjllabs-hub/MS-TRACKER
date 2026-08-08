package com.jllabs.moneylens.domain.usecases

import com.jllabs.moneylens.domain.models.Account
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType

data class BreakdownItem(
    val id: String,
    val name: String,
    val totalAmountMinor: Long,
    val transactionCount: Int,
    val percentage: Int
)

data class FinancialReportData(
    val periodName: String,
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val netSavingsMinor: Long,
    val categoryBreakdown: List<BreakdownItem>,
    val merchantBreakdown: List<BreakdownItem>,
    val accountBreakdown: List<BreakdownItem>
)

class GenerateFinancialReportUseCase {

    operator fun invoke(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        period: String
    ): FinancialReportData {
        val totalExpense = com.jllabs.moneylens.domain.accounts.InternalTransferClassifier
            .incomeExpenseTotals(transactions).second
        val totalIncome = com.jllabs.moneylens.domain.accounts.InternalTransferClassifier
            .incomeExpenseTotals(transactions).first
        val netSavings = totalIncome - totalExpense

        // 1. Category Breakdown
        val categoryBreakdown = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val catName = categories.find { it.id == catId }?.name ?: "Other"
                val sum = list.sumOf { it.amountMinor }
                val pct = if (totalExpense > 0) ((sum.toFloat() / totalExpense.toFloat()) * 100).toInt() else 0
                BreakdownItem(catId, catName, sum, list.size, pct)
            }
            .sortedByDescending { it.totalAmountMinor }

        // 2. Merchant Breakdown
        val merchantBreakdown = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.merchant }
            .map { (merchant, list) ->
                val sum = list.sumOf { it.amountMinor }
                val pct = if (totalExpense > 0) ((sum.toFloat() / totalExpense.toFloat()) * 100).toInt() else 0
                BreakdownItem(merchant, merchant, sum, list.size, pct)
            }
            .sortedByDescending { it.totalAmountMinor }

        // 3. Account Breakdown
        val accountBreakdown = transactions
            .groupBy { it.accountId }
            .map { (accId, list) ->
                val accName = accounts.find { it.id == accId }?.name ?: "Account"
                val sum = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                val pct = if (totalExpense > 0) ((sum.toFloat() / totalExpense.toFloat()) * 100).toInt() else 0
                BreakdownItem(accId, accName, sum, list.size, pct)
            }
            .sortedByDescending { it.totalAmountMinor }

        return FinancialReportData(
            periodName = period,
            totalIncomeMinor = totalIncome,
            totalExpenseMinor = totalExpense,
            netSavingsMinor = netSavings,
            categoryBreakdown = categoryBreakdown,
            merchantBreakdown = merchantBreakdown,
            accountBreakdown = accountBreakdown
        )
    }
}
