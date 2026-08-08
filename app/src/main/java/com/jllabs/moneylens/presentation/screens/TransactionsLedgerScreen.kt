package com.jllabs.moneylens.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.presentation.components.PeriodDropdown
import com.jllabs.moneylens.presentation.components.TransactionRowItem
import com.jllabs.moneylens.presentation.components.TypeSegmentedControl
import com.jllabs.moneylens.utils.Money

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionsLedgerScreen(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    val dateFilterOptions = listOf("All", "Today", "Yesterday", "This Week", "This Month", "3 Months", "6 Months", "1 Year", "3 Years")
    val typeTabOptions = listOf("All", "Credit", "Debit")

    var selectedTypeTab by remember { mutableStateOf("All") }
    var selectedTxForPopup by remember { mutableStateOf<Transaction?>(null) }

    // Apply Type Tab Filter (All, Credit, Debit, Just Info)
    val typeFilteredTransactions = remember(uiState.transactions, selectedTypeTab) {
        when (selectedTypeTab) {
            "Credit" -> uiState.transactions.filter { it.type == TransactionType.INCOME }
            "Debit" -> uiState.transactions.filter { it.type == TransactionType.EXPENSE && it.smsTransactionSubType != "INFO_ALERT" }
            else -> uiState.transactions
        }
    }

    // Group filtered transactions by date
    val groupedTransactions = typeFilteredTransactions.groupBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Transactions Ledger",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Text(
            text = "Tap any card to view raw SMS, adjust debit/credit direction & category",
            fontSize = 12.sp,
            color = Color(0xFF7C8079)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search merchant, category, bank, date, amount...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF7C8079)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF7C8079))
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(),
            singleLine = true
        )

                Spacer(modifier = Modifier.height(10.dp))

        PeriodDropdown(
            label = "Period",
            options = dateFilterOptions,
            selected = uiState.selectedFilter,
            onSelected = { viewModel.onFilterSelect(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TypeSegmentedControl(
            options = typeTabOptions,
            selected = selectedTypeTab,
            onSelected = { selectedTypeTab = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Transactions Grouped by Date Header
        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.searchQuery.isEmpty()) "No $selectedTypeTab transactions found for '${uiState.selectedFilter}'" else "No matching transactions",
                    color = Color(0xFF7C8079)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                groupedTransactions.forEach { (dateHeader, dayTransactions) ->
                    stickyHeader {
                        val dailyTotalMinor = dayTransactions.sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF4F3EF))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B7A57)
                            )
                            Surface(
                                color = Color(0xFFE4E8E3),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Day Net: ${Money.format(dailyTotalMinor)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D332A)
                                )
                            }
                        }
                    }

                    items(dayTransactions, key = { it.id }) { tx ->
                        val account = uiState.accounts.find { it.id == tx.accountId }
                        val category = uiState.categories.find { it.id == tx.categoryId }

                        Box(modifier = Modifier.clickable { selectedTxForPopup = tx }) {
                            TransactionRowItem(
                                transaction = tx,
                                accountName = account?.name ?: "Account",
                                categoryName = category?.name ?: "Category",
                                categoryIcon = category?.icon ?: "📦",
                                isPrivacyMasked = uiState.isPrivacyMasked,
                                isDarkMode = uiState.isDarkMode,
                                onDelete = { viewModel.deleteTransaction(tx.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Popup for Original SMS & Editable Details (Debit/Credit + Category)
    if (selectedTxForPopup != null) {
        EditableTransactionDetailDialog(
            transaction = selectedTxForPopup!!,
            categories = uiState.categories,
            isDarkMode = uiState.isDarkMode,
            onAddNewCategory = { newName, newIcon ->
                viewModel.addCategory(newName, newIcon)
            },
            onDeleteCategory = { catId ->
                viewModel.deleteCategory(catId)
            },
            onDeleteTransaction = {
                viewModel.deleteTransaction(selectedTxForPopup!!.id)
                selectedTxForPopup = null
            },
            onDismiss = { selectedTxForPopup = null },
            onSave = { updatedTx, newType, newCatId ->
                viewModel.updateTransaction(
                    id = updatedTx.id,
                    merchant = updatedTx.merchant,
                    categoryId = newCatId,
                    amountRupees = updatedTx.amountMinor / 100.0,
                    accountId = updatedTx.accountId,
                    note = updatedTx.note,
                    date = updatedTx.date,
                    type = newType
                )
                selectedTxForPopup = null
            }
        )
    }
}
