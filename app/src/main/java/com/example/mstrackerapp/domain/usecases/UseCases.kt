package com.example.mstrackerapp.domain.usecases

import com.example.mstrackerapp.domain.models.Account
import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.domain.models.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalculateNetWorthUseCase {
    operator fun invoke(accounts: List<Account>, transactions: List<Transaction>): Long {
        val startingNetWorth = accounts.filter { it.includeInNetWorth }.sumOf { it.startingBalanceMinor }
        val incomeTotal = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val expenseTotal = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
        return startingNetWorth + incomeTotal - expenseTotal
    }
}

class GetFilteredTransactionsUseCase {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    operator fun invoke(
        transactions: List<Transaction>,
        searchQuery: String,
        timeFilter: String,
        accounts: List<Account> = emptyList()
    ): List<Transaction> {
        var filtered = transactions

        val now = Calendar.getInstance()
        val todayStr = dateFormat.format(now.time)
        
        val yesterdayCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(yesterdayCal.time)

        val weekCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        val weekStr = dateFormat.format(weekCal.time)

        val monthCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val monthStr = dateFormat.format(monthCal.time)

        val threeMonthCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -3) }
        val threeMonthStr = dateFormat.format(threeMonthCal.time)

        val sixMonthCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -6) }
        val sixMonthStr = dateFormat.format(sixMonthCal.time)

        val oneYearCal = (now.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val oneYearStr = dateFormat.format(oneYearCal.time)

        val threeYearCal = (now.clone() as Calendar).apply { add(Calendar.YEAR, -3) }
        val threeYearStr = dateFormat.format(threeYearCal.time)

        val firstDayOfThisMonth = (now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val firstDayStr = dateFormat.format(firstDayOfThisMonth.time)

        // 1. Apply Date Filter
        filtered = when (timeFilter.uppercase().trim()) {
            "TODAY" -> filtered.filter { it.date == todayStr }
            "YESTERDAY" -> filtered.filter { it.date == yesterdayStr }
            "LAST 7 DAYS", "THIS WEEK", "WEEK", "CURRENT WEEK" -> filtered.filter { it.date >= weekStr && it.date <= todayStr }
            "THIS MONTH", "MONTH", "CURRENT MONTH" -> {
                val thisMonthList = filtered.filter { it.date >= firstDayStr && it.date <= todayStr }
                if (thisMonthList.isNotEmpty()) {
                    thisMonthList
                } else if (filtered.isNotEmpty()) {
                    // Fallback to most recent month with data so user never gets a zero screen
                    val latestDate = filtered.maxOf { it.date }
                    val latestMonthPrefix = latestDate.take(7) // "yyyy-MM"
                    filtered.filter { it.date.startsWith(latestMonthPrefix) }
                } else {
                    emptyList()
                }
            }
            "3 MONTHS" -> filtered.filter { it.date >= threeMonthStr && it.date <= todayStr }
            "6 MONTHS" -> filtered.filter { it.date >= sixMonthStr && it.date <= todayStr }
            "1 YEAR", "YEAR", "CURRENT YEAR" -> filtered.filter { it.date >= oneYearStr && it.date <= todayStr }
            "3 YEARS" -> filtered.filter { it.date >= threeYearStr && it.date <= todayStr }
            else -> filtered // "ALL" or unknown
        }

        // 2. Search Query Filter (Merchant, Category, Bank, Account, Date, Amount)
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter { tx ->
                val account = accounts.find { it.id == tx.accountId }
                val accountName = account?.name?.lowercase() ?: ""
                val bankName = account?.institution?.lowercase() ?: ""
                val amountRupeesStr = (tx.amountMinor / 100.0).toString()

                tx.merchant.lowercase().contains(q) ||
                        tx.categoryId.lowercase().contains(q) ||
                        accountName.contains(q) ||
                        bankName.contains(q) ||
                        tx.date.lowercase().contains(q) ||
                        tx.note.lowercase().contains(q) ||
                        amountRupeesStr.contains(q) ||
                        tx.amountMinor.toString().contains(q)
            }
        }

        return filtered.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.time }.thenByDescending { it.createdAt })
    }
}

class ProcessBankSmsUseCase {
    operator fun invoke(rawSmsText: String): Map<String, Any> {
        val amountStr = Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
            .find(rawSmsText)?.groupValues?.get(1)?.replace(",", "")
        val amountMinor = ((amountStr?.toDoubleOrNull() ?: 0.0) * 100).toLong()

        val merchant = when {
            rawSmsText.contains("Starbucks", ignoreCase = true) -> "Starbucks"
            rawSmsText.contains("Uber", ignoreCase = true) -> "Uber"
            rawSmsText.contains("Swiggy", ignoreCase = true) -> "Swiggy"
            else -> "Bank Transaction"
        }

        return mapOf(
            "amountMinor" to amountMinor,
            "merchant" to merchant
        )
    }
}
