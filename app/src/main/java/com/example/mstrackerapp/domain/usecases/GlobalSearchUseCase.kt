package com.example.mstrackerapp.domain.usecases

import com.example.mstrackerapp.domain.models.Account
import com.example.mstrackerapp.domain.models.Category
import com.example.mstrackerapp.domain.models.Transaction

data class GlobalSearchResult(
    val transaction: Transaction,
    val matchedField: String,
    val accountName: String,
    val categoryName: String
)

class GlobalSearchUseCase {

    operator fun invoke(
        query: String,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): List<GlobalSearchResult> {
        if (query.isBlank()) return emptyList()

        val q = query.trim().lowercase()
        val results = mutableListOf<GlobalSearchResult>()

        transactions.forEach { tx ->
            val category = categories.find { it.id == tx.categoryId }
            val account = accounts.find { it.id == tx.accountId }

            val categoryName = category?.name ?: "Other"
            val accountName = account?.name ?: "Account"
            val bankName = account?.institution ?: ""
            val amountRupeesStr = "%.2f".format(tx.amountMinor / 100.0)

            val matchedField = when {
                tx.merchant.lowercase().contains(q) -> "Merchant: ${tx.merchant}"
                categoryName.lowercase().contains(q) -> "Category: $categoryName"
                accountName.lowercase().contains(q) -> "Account: $accountName"
                bankName.lowercase().contains(q) -> "Bank: $bankName"
                tx.date.lowercase().contains(q) -> "Date: ${tx.date}"
                tx.note.lowercase().contains(q) -> "Note: ${tx.note}"
                amountRupeesStr.contains(q) || tx.amountMinor.toString().contains(q) -> "Amount: ₹$amountRupeesStr"
                tx.id.lowercase().contains(q) -> "Ref ID: ${tx.id}"
                else -> null
            }

            if (matchedField != null) {
                results.add(
                    GlobalSearchResult(
                        transaction = tx,
                        matchedField = matchedField,
                        accountName = accountName,
                        categoryName = categoryName
                    )
                )
            }
        }

        return results
    }
}
