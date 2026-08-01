package com.example.mstrackerapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.mstrackerapp.data.*
import com.example.mstrackerapp.util.Money

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MSTrackerViewModel = viewModel { MSTrackerViewModel(DefaultMSTrackerRepository()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddTxDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarHeader(
                activeTab = uiState.activeTab,
                smsCount = uiState.smsQueue.size,
                onSmsClick = { viewModel.selectTab(AppTab.SMS_INBOX) }
            )
        },
        bottomBar = {
            MSTrackerBottomNavigation(
                activeTab = uiState.activeTab,
                smsCount = uiState.smsQueue.size,
                onTabSelect = viewModel::selectTab
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTxDialog = true },
                containerColor = Color(0xFF3B7A57),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F3EF))
                .padding(paddingValues)
        ) {
            when (uiState.activeTab) {
                AppTab.OVERVIEW -> OverviewTabScreen(uiState = uiState, viewModel = viewModel)
                AppTab.TRANSACTIONS -> TransactionsTabScreen(uiState = uiState, viewModel = viewModel)
                AppTab.GOALS -> GoalsTabScreen(uiState = uiState, viewModel = viewModel)
                AppTab.ACCOUNTS -> AccountsTabScreen(uiState = uiState, viewModel = viewModel)
                AppTab.SMS_INBOX -> SmsInboxTabScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, amount, accId, catId, merchant, note ->
                viewModel.addTransaction(type, amount, accId, catId, merchant, note)
                showAddTxDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarHeader(
    activeTab: AppTab,
    smsCount: Int,
    onSmsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B7A57)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MS Tracker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2D332A)
                    )
                    Text(
                        text = "Private Personal Finance",
                        fontSize = 10.sp,
                        color = Color(0xFF7C8079)
                    )
                }
            }
        },
        actions = {
            Surface(
                color = Color(0xFFE4E8E3),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "LOCAL PRIVACY",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B7A57)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            BadgedBox(
                badge = {
                    if (smsCount > 0) {
                        Badge(containerColor = Color(0xFF3B7A57)) {
                            Text(text = smsCount.toString(), color = Color.White)
                        }
                    }
                }
            ) {
                IconButton(onClick = onSmsClick) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "SMS Queue",
                        tint = Color(0xFF2D332A)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF4F3EF))
    )
}

@Composable
fun MSTrackerBottomNavigation(
    activeTab: AppTab,
    smsCount: Int,
    onTabSelect: (AppTab) -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = activeTab == AppTab.OVERVIEW,
            onClick = { onTabSelect(AppTab.OVERVIEW) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
            label = { Text("Overview") }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.TRANSACTIONS,
            onClick = { onTabSelect(AppTab.TRANSACTIONS) },
            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Ledger") },
            label = { Text("Ledger") }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.GOALS,
            onClick = { onTabSelect(AppTab.GOALS) },
            icon = { Icon(Icons.Default.Flag, contentDescription = "Goals") },
            label = { Text("Goals") }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.ACCOUNTS,
            onClick = { onTabSelect(AppTab.ACCOUNTS) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts") },
            label = { Text("Accounts") }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.SMS_INBOX,
            onClick = { onTabSelect(AppTab.SMS_INBOX) },
            icon = {
                BadgedBox(
                    badge = {
                        if (smsCount > 0) {
                            Badge(containerColor = Color(0xFF3B7A57)) {
                                Text(text = smsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = "Inbox")
                }
            },
            label = { Text("Inbox") }
        )
    }
}

// ==================== OVERVIEW TAB ====================
@Composable
fun OverviewTabScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Overview",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D332A)
            )
        }

        // Net Worth Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NET WORTH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C8079),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.togglePrivacyMask() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPrivacyMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Mask",
                                    tint = Color(0xFF7C8079)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFE4E8E3),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Live",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B7A57)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (uiState.isPrivacyMasked) "₹ • • • • • •" else Money.format(uiState.netWorthMinor),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D332A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Income vs Spent 2-Column Split Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Income Card
                        Surface(
                            color = Color(0xFFE4E8E3).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B7A57)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("INCOME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C8079))
                                    Text(
                                        text = if (uiState.isPrivacyMasked) "₹ • • •" else "+${Money.format(uiState.totalIncomeMinor)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B7A57)
                                    )
                                }
                            }
                        }

                        // Spent Card
                        Surface(
                            color = Color(0xFFF7EBE3).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD87D56)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↑", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("SPENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C8079))
                                    Text(
                                        text = if (uiState.isPrivacyMasked) "₹ • • •" else "-${Money.format(uiState.totalExpenseMinor)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD87D56)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spending Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Spending Breakdown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D332A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.categories.take(4).forEach { cat ->
                        val catSpent = uiState.transactions
                            .filter { it.categoryId == cat.id && it.type == TransactionType.EXPENSE }
                            .sumOf { it.amountMinor }

                        val pct = if (uiState.totalExpenseMinor > 0) (catSpent.toFloat() / uiState.totalExpenseMinor) else 0f

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${cat.icon} ${cat.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (uiState.isPrivacyMasked) "₹ • • •" else Money.format(catSpent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C8079)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF3B7A57),
                                trackColor = Color(0xFFE5E3DC)
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity List
        item {
            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D332A)
            )
        }

        items(uiState.transactions.take(5)) { tx ->
            val cat = uiState.categories.find { it.id == tx.categoryId }
            val acc = uiState.accounts.find { it.id == tx.accountId }
            TransactionRowItem(tx = tx, category = cat, account = acc, isMasked = uiState.isPrivacyMasked)
        }
    }
}

// ==================== TRANSACTIONS TAB ====================
@Composable
fun TransactionsTabScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = { Text("Search merchant, notes, date...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        val filters = listOf("All", "Expense", "Income", "Transfer")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f ->
                FilterChip(
                    selected = uiState.selectedFilter == f,
                    onClick = { viewModel.onFilterSelect(f) },
                    label = { Text(f) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.transactions) { tx ->
                val cat = uiState.categories.find { it.id == tx.categoryId }
                val acc = uiState.accounts.find { it.id == tx.accountId }
                TransactionRowItem(
                    tx = tx,
                    category = cat,
                    account = acc,
                    isMasked = uiState.isPrivacyMasked,
                    onDelete = { viewModel.deleteTransaction(tx.id) }
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: Transaction,
    category: Category?,
    account: Account?,
    isMasked: Boolean,
    onDelete: (() -> Unit)? = null
) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val sign = if (isExpense) "-" else "+"
    val color = if (isExpense) Color(0xFF2D332A) else Color(0xFF3B7A57)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4E8E3)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category?.icon ?: "📦", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.merchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2D332A)
                    )
                    Text(
                        text = "${category?.name ?: "Category"} • ${account?.name ?: "Account"}",
                        fontSize = 11.sp,
                        color = Color(0xFF7C8079)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMasked) "₹ • • •" else "$sign${Money.format(tx.amountMinor, absolute = true)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// ==================== GOALS TAB ====================
@Composable
fun GoalsTabScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Milestones & Goals",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.goals) { goal ->
                GoalCardItem(goal = goal, isMasked = uiState.isPrivacyMasked)
            }
        }
    }
}

@Composable
fun GoalCardItem(goal: Goal, isMasked: Boolean) {
    val pct = if (goal.targetAmountMinor > 0) ((goal.currentSavedMinor.toFloat() / goal.targetAmountMinor) * 100).toInt() else 0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = goal.icon, fontSize = 24.sp)
                Text(text = "$pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B7A57))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = goal.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))
            Text(
                text = if (isMasked) "₹ • • • / ₹ • • •" else "${Money.format(goal.currentSavedMinor)} / ${Money.format(goal.targetAmountMinor)}",
                fontSize = 10.sp,
                color = Color(0xFF7C8079)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF3B7A57),
                trackColor = Color(0xFFE5E3DC)
            )
        }
    }
}

// ==================== ACCOUNTS TAB ====================
@Composable
fun AccountsTabScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Accounts & Cards",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.accounts) { acc ->
                val accSpent = uiState.transactions.filter { it.accountId == acc.id && it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                val accInc = uiState.transactions.filter { it.accountId == acc.id && it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                val currentBal = acc.startingBalanceMinor + accInc - accSpent

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = acc.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = acc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2D332A))
                                Text(text = acc.type.name.replace("_", " "), fontSize = 11.sp, color = Color(0xFF7C8079))
                            }
                        }

                        Text(
                            text = if (uiState.isPrivacyMasked) "₹ • • •" else Money.format(currentBal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (currentBal < 0) Color(0xFFD87D56) else Color(0xFF3B7A57)
                        )
                    }
                }
            }
        }
    }
}

// ==================== SMS INBOX TAB ====================
@Composable
fun SmsInboxTabScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Smart Bank SMS Queue",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Text(
            text = "Auto-detected Bank Spends ready to record",
            fontSize = 12.sp,
            color = Color(0xFF7C8079)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.smsQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("All bank SMS notifications parsed & recorded!", color = Color(0xFF7C8079))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(uiState.smsQueue) { sms ->
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
                                Surface(
                                    color = Color(0xFFE4E8E3),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = sms.bank,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B7A57)
                                    )
                                }

                                Text(
                                    text = Money.format(sms.amountMinor),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFD87D56)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = sms.rawText,
                                fontSize = 12.sp,
                                color = Color(0xFF2D332A)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.acceptSms(sms.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept & Record Transaction", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== ADD TRANSACTION DIALOG ====================
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, Double, String, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction (₹ INR)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹ INR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Source (e.g. Starbucks, Uber)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Remarks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(type, amt, selectedAccId, selectedCatId, merchant, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57))
            ) {
                Text("Save Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
