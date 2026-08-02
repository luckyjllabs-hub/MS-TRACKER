package com.example.mstrackerapp.presentation.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.domain.models.Category
import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.presentation.components.CategoryPieChart
import com.example.mstrackerapp.presentation.components.DailySpendingLineChart
import com.example.mstrackerapp.presentation.components.MonthlyBarChart
import com.example.mstrackerapp.presentation.components.PieChartSlice
import com.example.mstrackerapp.presentation.components.TransactionRowItem
import com.example.mstrackerapp.utils.Money

@Composable
fun OverviewScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    val dashboardFilters = listOf("This Month", "Today", "Yesterday", "This Week", "3 Months", "6 Months", "1 Year", "3 Years")
    val typeTabOptions = listOf("All", "Credit", "Debit")

    var selectedTypeTab by remember { mutableStateOf("All") }
    var selectedTxForPopup by remember { mutableStateOf<Transaction?>(null) }

    val savingsMinor = uiState.totalIncomeMinor - uiState.totalExpenseMinor

    // Budget Used Calculation
    val totalBudgetLimitMinor = uiState.categories.sumOf { it.monthlyLimitMinor }
    val budgetProgress = if (totalBudgetLimitMinor > 0) {
        (uiState.totalExpenseMinor.toFloat() / totalBudgetLimitMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Largest Expense
    val largestExpense = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .maxByOrNull { it.amountMinor }

    // Top Categories & Pie Slices
    val categoryExpenses = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
        .entries
        .sortedByDescending { it.value }

    val pieSlices = categoryExpenses.take(4).map { (catId, totalMinor) ->
        val cat = uiState.categories.find { it.id == catId }
        val catName = cat?.name ?: "Other"
        val catColor = try { Color(android.graphics.Color.parseColor(cat?.color ?: "#8F9C8A")) } catch (e: Exception) { Color(0xFF3B7A57) }
        PieChartSlice(label = catName, value = totalMinor / 100f, color = catColor)
    }

    // Dynamic Chart Data (Aggregating real transactions across last 4 months)
    val (monthLabels, incomeHistory, expenseHistory) = remember(uiState.transactions) {
        val sdfMonthName = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH)
        val sdfMonthNum = java.text.SimpleDateFormat("MM", java.util.Locale.ENGLISH)

        val months = (3 downTo 0).map { offset ->
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.MONTH, -offset)
            Pair(sdfMonthName.format(c.time), sdfMonthNum.format(c.time))
        }

        val labels = months.map { it.first }

        val incs = months.map { (name, num) ->
            uiState.transactions
                .filter { it.type == TransactionType.INCOME && (it.date.contains(num) || it.date.contains(name, ignoreCase = true)) }
                .sumOf { it.amountMinor } / 100f
        }

        val exps = months.map { (name, num) ->
            uiState.transactions
                .filter { it.type == TransactionType.EXPENSE && (it.date.contains(num) || it.date.contains(name, ignoreCase = true)) }
                .sumOf { it.amountMinor } / 100f
        }

        Triple(labels, incs, exps)
    }

    // Dynamic Daily Trend (Aggregating last 7 days of actual expenses)
    val (dailyTrend, dayLabels) = remember(uiState.transactions) {
        val sdfDayName = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)
        val sdfDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
        val sdfAltDateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.ENGLISH)

        val days = (6 downTo 0).map { offset ->
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_YEAR, -offset)
            Triple(
                sdfDayName.format(c.time),
                sdfDateStr.format(c.time),
                sdfAltDateStr.format(c.time)
            )
        }

        val labels = days.map { it.first }

        val amounts = days.map { (_, isoDate, altDate) ->
            uiState.transactions
                .filter { it.type == TransactionType.EXPENSE && (it.date == isoDate || it.date.contains(altDate, ignoreCase = true)) }
                .sumOf { it.amountMinor } / 100f
        }

        Pair(amounts, labels)
    }

    // Filter transactions by Type Tab (All, Credit, Debit) & Search Query
    val filteredTransactions = remember(uiState.transactions, selectedTypeTab, uiState.searchQuery) {
        var list = uiState.transactions

        list = when (selectedTypeTab) {
            "Credit" -> list.filter { it.type == TransactionType.INCOME }
            "Debit" -> list.filter { it.type == TransactionType.EXPENSE }
            else -> list
        }

        if (uiState.searchQuery.isNotBlank()) {
            val q = uiState.searchQuery.lowercase()
            list = list.filter { tx ->
                tx.merchant.lowercase().contains(q) ||
                tx.bankName.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                tx.date.contains(q) ||
                (tx.amountMinor / 100.0).toString().contains(q)
            }
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // Time Filters Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dashboardFilters) { filter ->
                    val isSelected = uiState.selectedFilter.equals(filter, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelect(filter) },
                        label = { Text(filter, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B7A57), selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 1. Net Worth Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B7A57)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Net Worth Summary (${uiState.selectedFilter})", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isPrivacyMasked) "••••••••" else Money.format(uiState.netWorthMinor),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                                text = if (uiState.isPrivacyMasked) "••••" else Money.format(uiState.totalIncomeMinor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFFF8B94), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expense", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Text(
                                text = if (uiState.isPrivacyMasked) "••••" else Money.format(uiState.totalExpenseMinor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFFFD3B6), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Savings", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Text(
                                text = if (uiState.isPrivacyMasked) "••••" else Money.format(savingsMinor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. Category Pie Chart
        if (pieSlices.isNotEmpty()) {
            item {
                CategoryPieChart(slices = pieSlices)
            }
        }

        // 3. Monthly Bar Chart
        item {
            MonthlyBarChart(
                incomeValues = incomeHistory,
                expenseValues = expenseHistory,
                monthLabels = monthLabels
            )
        }

        // 4. Daily Spending Line Chart
        item {
            DailySpendingLineChart(
                spendingPoints = dailyTrend,
                dayLabels = dayLabels
            )
        }

        // 5. Budget Progress
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monthly Budget Used", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))
                        Text("${(budgetProgress * 100).toInt()}% Used", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF3B7A57))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF3B7A57),
                        trackColor = Color(0xFFE4E8E3),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // 6. Largest Expense
        if (largestExpense != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { selectedTxForPopup = largestExpense }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Largest Single Expense (Tap to view/edit)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF856404))
                            Text(largestExpense.merchant, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))
                        }
                        Text(
                            text = Money.format(largestExpense.amountMinor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFD87D56)
                        )
                    }
                }
            }
        }

        // 7. UNIFIED SEARCH & TRANSACTIONS LIST (Placed immediately after Largest Single Expense)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Transactions Ledger (${uiState.selectedFilter})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2D332A)
                )

                // Compact Search Bar (Height reduced to 42.dp for clean look and feel)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD0D5CE)),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF7C8079),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search merchant, category, bank, date, amount...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7C8079)
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = Color.Black
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (uiState.searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF7C8079),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.onSearchQueryChange("") }
                            )
                        }
                    }
                }

                // All | Credit | Debit Type Tabs (Matching Reference App Tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE4E8E3), shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    typeTabOptions.forEach { tab ->
                        val isSelected = selectedTypeTab.equals(tab, ignoreCase = true)
                        Surface(
                            color = if (isSelected) Color(0xFF3B7A57) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTypeTab = tab }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (tab) {
                                        "Credit" -> "Credit (+)"
                                        "Debit" -> "Debit (-)"
                                        else -> "All (${filteredTransactions.size})"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF555A52)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transactions Grouped by Date Headers
        if (filteredTransactions.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
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
                            color = Color(0xFF7C8079),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            val groupedByDate = filteredTransactions.groupBy { it.date }
            groupedByDate.forEach { (dateHeader, dayTransactions) ->
                item(key = "header-$dateHeader") {
                    Surface(
                        color = Color(0xFFF4F3EF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B7A57)
                            )
                            val dailyNet = dayTransactions.sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor }
                            Text(
                                text = "Day Net: ${Money.format(dailyNet)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF555A52)
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
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
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
            onAddNewCategory = { newName, newIcon ->
                viewModel.addCategory(newName, newIcon)
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
    onAddNewCategory: (String, String) -> String,
    onDismiss: () -> Unit,
    onSave: (Transaction, TransactionType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Custom Category Input Mode
    var isInputtingNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("✨") }

    val selectedCategory = categories.find { it.id == selectedCategoryId } ?: categories.firstOrNull()

    val rawTextToShow = when {
        transaction.rawSms.isNotBlank() -> transaction.rawSms
        transaction.note.isNotBlank() -> transaction.note
        else -> "Recorded from ${transaction.bankName.ifEmpty { "Bank" }} transaction."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Transaction & View Raw SMS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2D332A))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Amount & Merchant Header
                Surface(
                    color = Color(0xFFF4F3EF),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(transaction.merchant, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2D332A))
                        Text(
                            text = Money.format(transaction.amountMinor),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text("Date & Time: ${transaction.date} at ${transaction.time}", fontSize = 11.sp, color = Color(0xFF7C8079))
                        if (transaction.bankName.isNotBlank()) {
                            Text("Bank: ${transaction.bankName}", fontSize = 11.sp, color = Color(0xFF3B7A57), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Transaction Type Combo Box
                Text("Transaction Type Combo Pick:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555A52))
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            TransactionType.EXPENSE -> "⬇ DEBIT / EXPENSE (-)"
                            TransactionType.INCOME -> "⬆ CREDIT / INCOME (+)"
                            TransactionType.TRANSFER -> "🔄 TRANSFER"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B7A57),
                            cursorColor = Color(0xFF3B7A57)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⬇ DEBIT / EXPENSE (-)", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedType = TransactionType.EXPENSE
                                typeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⬆ CREDIT / INCOME (+)", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedType = TransactionType.INCOME
                                typeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔄 TRANSFER", color = Color(0xFF2E5B88), fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedType = TransactionType.TRANSFER
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }

                // 2. Category Combo Box with "Create New Category" Option
                Text("Category Type Combo Pick:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555A52))
                
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory?.icon ?: "📦"} ${selectedCategory?.name ?: "Select Category"}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B7A57),
                            cursorColor = Color(0xFF3B7A57)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        // Option to Input / Create New Category
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("➕ Input / Create New Category...", color = Color(0xFF3B7A57), fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                isInputtingNewCategory = true
                                categoryDropdownExpanded = false
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // List all existing categories from DB
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.icon} ${category.name}", fontSize = 14.sp) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    isInputtingNewCategory = false
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Inline New Category Input Form (if user tapped "Input / Create New Category")
                if (isInputtingNewCategory) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Input New Custom Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newCategoryIcon,
                                    onValueChange = { newCategoryIcon = it },
                                    label = { Text("Icon") },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    label = { Text("Category Name") },
                                    placeholder = { Text("e.g. Gaming, Pets") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Button(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        val createdId = onAddNewCategory(newCategoryName, newCategoryIcon)
                                        selectedCategoryId = createdId
                                        isInputtingNewCategory = false
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

                // 3. Full Raw Received Bank SMS Text
                Text("Original Received Bank SMS Text:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555A52))
                Surface(
                    color = Color(0xFF1E241C),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rawTextToShow,
                        color = Color(0xFFE4E8E3),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(transaction, selectedType, selectedCategoryId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57))
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save & Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
