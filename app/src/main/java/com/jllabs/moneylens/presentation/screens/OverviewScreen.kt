package com.jllabs.moneylens.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.presentation.components.CategoryPieChart
import com.jllabs.moneylens.presentation.components.PieChartSlice
import com.jllabs.moneylens.presentation.components.TransactionRowItem
import com.jllabs.moneylens.presentation.components.TypeSegmentedControl
import com.jllabs.moneylens.presentation.components.transactionMatchesSearch
import com.jllabs.moneylens.theme.rememberAppUiColors
import com.jllabs.moneylens.utils.CategoryIcons
import com.jllabs.moneylens.utils.CsvExporter
import com.jllabs.moneylens.utils.Money

@Composable
fun OverviewScreen(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    val context = LocalContext.current
    val ui = rememberAppUiColors(uiState.isDarkMode)
    val isDark = uiState.isDarkMode
    val typeTabOptions = listOf("All", "Credit", "Debit")

    var selectedTypeTab by remember { mutableStateOf("All") }
    var selectedTxForPopup by remember { mutableStateOf<Transaction?>(null) }
    // Dates absent from the map default to collapsed (same as Accounts sections).
    val dateExpanded = remember { mutableStateMapOf<String, Boolean>() }

    val savingsMinor = uiState.totalIncomeMinor - uiState.totalExpenseMinor

    // Largest Expense
    val largestExpense = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .maxByOrNull { it.amountMinor }

    // Category distribution from the SAME period-filtered expenses (all categories;
    // top 5 + Others so small cats like Fuel still show correctly).
    val categoryExpenses = remember(uiState.transactions) {
        uiState.transactions
            .filter { it.type == TransactionType.EXPENSE && it.smsTransactionSubType != "INFO_ALERT" }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
            .entries
            .sortedByDescending { it.value }
    }

    val pieSlices = remember(categoryExpenses, uiState.categories) {
        if (categoryExpenses.isEmpty()) emptyList()
        else {
            val top = categoryExpenses.take(4)
            val rest = categoryExpenses.drop(5).sumOf { it.value }
            val slices = top.map { (catId, totalMinor) ->
                val cat = uiState.categories.find { it.id == catId }
                val catName = cat?.name ?: "Other"
                val catColor = try {
                    Color(android.graphics.Color.parseColor(cat?.color ?: "#8F9C8A"))
                } catch (_: Exception) {
                    Color(0xFF3B7A57)
                }
                PieChartSlice(label = catName, value = totalMinor / 100f, color = catColor)
            }.toMutableList()
            if (rest > 0) {
                slices += PieChartSlice(label = "Others", value = rest / 100f, color = Color(0xFF9E9E9E))
            }
            slices
        }
    }

    // Filter transactions by Type Tab (All, Credit, Debit) & Search Query
    // Overview is money movement only — never EMI/due/balance JUST_INFO reminders.
    // TRANSFER (true self-transfer) still shows so moves aren't "missing".
    val filteredTransactions = remember(uiState.transactions, selectedTypeTab, uiState.searchQuery) {
        var list = uiState.transactions.filter { tx ->
            when (tx.type) {
                TransactionType.INCOME -> true
                TransactionType.EXPENSE -> tx.smsTransactionSubType != "INFO_ALERT"
                TransactionType.TRANSFER -> true
                TransactionType.JUST_INFO -> false
            } && !tx.source.equals("SMS_REMINDER", ignoreCase = true) &&
                !tx.merchant.equals("EMI due", ignoreCase = true) &&
                !tx.merchant.equals("Payment due", ignoreCase = true) &&
                !tx.merchant.equals("Balance update", ignoreCase = true)
        }

        list = when (selectedTypeTab) {
            "Credit" -> list.filter {
                it.type == TransactionType.INCOME ||
                    (it.type == TransactionType.TRANSFER &&
                        (it.rawSms.contains("credited", ignoreCase = true) ||
                            it.rawSms.contains("received", ignoreCase = true) ||
                            it.smsTransactionSubType.equals("TRANSFER_IN", ignoreCase = true)))
            }
            "Debit" -> list.filter {
                it.type == TransactionType.EXPENSE ||
                    (it.type == TransactionType.TRANSFER &&
                        (it.rawSms.contains("debited", ignoreCase = true) ||
                            it.smsTransactionSubType.equals("TRANSFER_OUT", ignoreCase = true)))
            }
            else -> list
        }

        if (uiState.searchQuery.isNotBlank()) {
            list = list.filter { tx ->
                transactionMatchesSearch(tx, uiState.searchQuery, uiState.categories)
            }
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
    ) {
        // Period + search live in the global filter bar (MainScreen)

        // 1. Net Worth Card
        item {
            val netWorthColor = if (uiState.netWorthMinor < 0) Color(0xFFFFCDD2) else Color.White
            val savingsColor = when {
                savingsMinor < 0 -> Color(0xFFFFCDD2)
                else -> Color.White
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B7A57)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Net Worth Summary (${uiState.selectedFilter})", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        IconButton(
                            onClick = {
                                CsvExporter.exportAndShareCsv(
                                    context = context,
                                    filterName = uiState.selectedFilter,
                                    netWorthMinor = uiState.netWorthMinor,
                                    incomeMinor = uiState.totalIncomeMinor,
                                    expenseMinor = uiState.totalExpenseMinor,
                                    savingsMinor = savingsMinor,
                                    transactions = filteredTransactions
                                )
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isPrivacyMasked) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022" else Money.format(uiState.netWorthMinor),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = netWorthColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFA8E6CF), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Income", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Text(
                                text = if (uiState.isPrivacyMasked) "\u2022\u2022\u2022\u2022" else Money.format(uiState.totalIncomeMinor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC8E6C9)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFFF8B94), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expense", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Text(
                                text = if (uiState.isPrivacyMasked) "\u2022\u2022\u2022\u2022" else Money.format(uiState.totalExpenseMinor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFCDD2)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFFFD3B6), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Savings", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Text(
                                text = if (uiState.isPrivacyMasked) "\u2022\u2022\u2022\u2022" else Money.format(savingsMinor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = savingsColor
                            )
                        }
                    }
                }
            }
        }

        // 2. Compact category spend breakdown — reflects selected period
        if (pieSlices.isNotEmpty()) {
            item {
                CategoryPieChart(
                    slices = pieSlices,
                    title = "Category spend \u00B7 ${uiState.selectedFilter}",
                    isPrivacyMasked = uiState.isPrivacyMasked,
                    isDarkMode = isDark
                )
            }
        }

        // Largest Expense
        if (largestExpense != null) {
            item {
                val expenseCardBg = if (isDark) Color(0xFF2A2418) else Color(0xFFFFF3CD)
                val expenseLabel = if (isDark) Color(0xFFFFCC80) else Color(0xFF856404)
                val expenseTitle = ui.ink
                val expenseAmount = if (isDark) Color(0xFFFFAB91) else Color(0xFFD87D56)
                Card(
                    colors = CardDefaults.cardColors(containerColor = expenseCardBg),
                    shape = RoundedCornerShape(18.dp),
                    border = if (isDark) BorderStroke(1.dp, Color(0xFF3D3428)) else null,
                    modifier = Modifier.fillMaxWidth().clickable { selectedTxForPopup = largestExpense }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = expenseLabel, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Largest Single Expense (Tap to view/edit)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = expenseLabel)
                            Text(
                                text = largestExpense.merchant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = expenseTitle,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Money.format(largestExpense.amountMinor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = expenseAmount,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 7. UNIFIED SEARCH & TRANSACTIONS LIST (Placed immediately after Largest Single Expense)
        item {
            val filterLabel = if (uiState.selectedFilter.equals("All", ignoreCase = true)) "All Time" else uiState.selectedFilter
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Transactions Ledger ($filterLabel)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ui.ink
                )

                TypeSegmentedControl(
                    options = typeTabOptions,
                    selected = selectedTypeTab,
                    onSelected = { selectedTypeTab = it }
                )

                Text(
                    text = "${filteredTransactions.size} transactions \u00B7 $selectedTypeTab",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ui.muted
                )
            }
        }

        // Transactions Grouped by Date Headers
        if (filteredTransactions.isEmpty()) {
            item {
                Surface(
                    color = ui.card,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isEmpty()) "No $selectedTypeTab transactions for ${uiState.selectedFilter}" else "No matching transactions",
                            color = ui.muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            val groupedByDate = filteredTransactions.groupBy { it.date }
            groupedByDate.forEach { (dateHeader, dayTransactions) ->
                val isExpanded = dateExpanded[dateHeader] ?: false
                item(key = "header-$dateHeader") {
                    Surface(
                        color = ui.chip,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                dateExpanded[dateHeader] = !isExpanded
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF81C784) else Color(0xFF3B7A57),
                                modifier = Modifier.weight(1f)
                            )
                            val dailyNet = dayTransactions.sumOf {
                                if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor
                            }
                            Text(
                                text = "#${dayTransactions.size} · Day Net: ${Money.format(dailyNet)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ui.muted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = ui.muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isExpanded) {
                    items(dayTransactions, key = { it.id }) { tx ->
                        val account = uiState.accounts.find { it.id == tx.accountId }
                        val category = uiState.categories.find { it.id == tx.categoryId }

                        Box(modifier = Modifier.clickable { selectedTxForPopup = tx }) {
                            TransactionRowItem(
                                transaction = tx,
                                accountName = account?.name ?: "Account",
                                categoryName = category?.name ?: "Category",
                                categoryIcon = CategoryIcons.display(category?.icon, category?.name ?: "Category"),
                                isPrivacyMasked = uiState.isPrivacyMasked,
                                isDarkMode = isDark,
                                onDelete = { viewModel.deleteTransaction(tx.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Editable Transaction Popup Modal Dialog with Combo Boxes & Custom Category Input
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTransactionDetailDialog(
    transaction: Transaction,
    categories: List<Category>,
    isDarkMode: Boolean = false,
    onAddNewCategory: (String, String) -> String,
    onDeleteCategory: (String) -> Unit = {},
    onDeleteTransaction: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Transaction, TransactionType, String) -> Unit
) {
    val context = LocalContext.current
    val ui = rememberAppUiColors(isDarkMode)
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var confirmDeleteTx by remember { mutableStateOf(false) }

    var isInputtingNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("") }

    val selectedCategory = categories.find { it.id == selectedCategoryId } ?: categories.firstOrNull()

    val rawTextToShow = when {
        transaction.rawSms.isNotBlank() -> transaction.rawSms
        transaction.note.isNotBlank() -> transaction.note
        else -> "Recorded from ${transaction.bankName.ifEmpty { "Bank" }} transaction."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ui.card,
        titleContentColor = ui.ink,
        textContentColor = ui.ink,
        title = {
            Text("Edit Transaction & View Raw SMS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ui.ink)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = ui.chip,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(transaction.merchant, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ui.ink)
                        Text(
                            text = Money.format(transaction.amountMinor),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedType) {
                                TransactionType.INCOME -> Color(0xFF81C784)
                                TransactionType.JUST_INFO -> ui.muted
                                else -> Color(0xFFEF9A9A)
                            }
                        )
                        Text("Date & Time: ${transaction.date} at ${transaction.time}", fontSize = 11.sp, color = ui.muted)
                        if (transaction.bankName.isNotBlank()) {
                            Text("Bank: ${transaction.bankName}", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Transaction Type Combo Pick:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ui.muted)
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            TransactionType.EXPENSE -> "\u2B07 DEBIT / EXPENSE (-)"
                            TransactionType.INCOME -> "\u2B06 CREDIT / INCOME (+)"
                            TransactionType.TRANSFER -> "\uD83D\uDD04 TRANSFER"
                            TransactionType.JUST_INFO -> "\u2139\uFE0F JUST INFO / ALERT (Excluded)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(isDarkMode)
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        containerColor = ui.card
                    ) {
                        DropdownMenuItem(
                            text = { Text("\u2B07 DEBIT / EXPENSE (-)", color = Color(0xFFEF9A9A), fontWeight = FontWeight.Bold) },
                            onClick = { selectedType = TransactionType.EXPENSE; typeDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("\u2B06 CREDIT / INCOME (+)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold) },
                            onClick = { selectedType = TransactionType.INCOME; typeDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("\uD83D\uDD04 TRANSFER", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold) },
                            onClick = { selectedType = TransactionType.TRANSFER; typeDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("\u2139\uFE0F JUST INFO / ALERT (Excluded)", color = ui.muted, fontWeight = FontWeight.Bold) },
                            onClick = { selectedType = TransactionType.JUST_INFO; typeDropdownExpanded = false }
                        )
                    }
                }

                Text("Category Type Combo Pick:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ui.muted)
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${CategoryIcons.display(selectedCategory?.icon, selectedCategory?.name ?: "Category")} ${selectedCategory?.name ?: "Select Category"}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(isDarkMode)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        containerColor = ui.card
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Input / Create New Category...", color = Color(0xFF3B7A57), fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                isInputtingNewCategory = true
                                newCategoryIcon = CategoryIcons.letterFor(newCategoryName.ifBlank { "N" })
                                categoryDropdownExpanded = false
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${CategoryIcons.display(category.icon, category.name)} ${category.name}",
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (selectedCategoryId == category.id) {
                                                    selectedCategoryId = categories.firstOrNull { it.id != category.id }?.id
                                                        ?: selectedCategoryId
                                                }
                                                onDeleteCategory(category.id)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete category",
                                                tint = Color(0xFFC62828),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = category.id
                                    isInputtingNewCategory = false
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isInputtingNewCategory) {
                    Surface(
                        color = ui.chip,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Input New Custom Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            Text("Leave icon blank to use the first letter of the name.", fontSize = 10.sp, color = ui.muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newCategoryIcon,
                                    onValueChange = { newCategoryIcon = it.take(2) },
                                    label = { Text("Icon") },
                                    placeholder = { Text(CategoryIcons.letterFor(newCategoryName.ifBlank { "N" })) },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true,
                                    colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(isDarkMode)
                                )
                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = {
                                        newCategoryName = it
                                        if (newCategoryIcon.isBlank() || CategoryIcons.isBroken(newCategoryIcon)) {
                                            newCategoryIcon = CategoryIcons.letterFor(it.ifBlank { "N" })
                                        }
                                    },
                                    label = { Text("Category Name") },
                                    placeholder = { Text("e.g. Gaming, Pets") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(isDarkMode)
                                )
                            }
                            Button(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        val createdId = onAddNewCategory(newCategoryName, newCategoryIcon)
                                        selectedCategoryId = createdId
                                        isInputtingNewCategory = false
                                        newCategoryName = ""
                                        newCategoryIcon = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Create & Assign Category", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Original Received Bank SMS Text:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ui.muted)
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SMS Body", rawTextToShow))
                            Toast.makeText(context, "Copied SMS text", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF81C784))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp, color = Color(0xFF81C784))
                    }
                }
                Surface(
                    color = ui.chip,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rawTextToShow,
                        color = ui.ink,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { confirmDeleteTx = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF9A9A))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                Button(
                    onClick = { onSave(transaction, selectedType, selectedCategoryId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save & Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF81C784)) }
        }
    )

    if (confirmDeleteTx) {
        AlertDialog(
            onDismissRequest = { confirmDeleteTx = false },
            containerColor = ui.card,
            title = { Text("Delete transaction?", color = ui.ink) },
            text = { Text("This removes \"${transaction.merchant}\" (${Money.format(transaction.amountMinor)}) from your ledger.", color = ui.ink) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteTx = false
                        onDeleteTransaction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTx = false }) { Text("Cancel", color = ui.muted) }
            }
        )
    }
}
