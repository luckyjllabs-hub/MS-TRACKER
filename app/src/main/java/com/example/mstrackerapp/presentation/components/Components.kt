package com.example.mstrackerapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.mstrackerapp.domain.models.*
import com.example.mstrackerapp.presentation.navigation.AppTab
import com.example.mstrackerapp.utils.Money

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
            label = { Text("Overview", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.BUDGET,
            onClick = { onTabSelect(AppTab.BUDGET) },
            icon = { Icon(Icons.Default.PieChart, contentDescription = "Budget") },
            label = { Text("Budget", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.GOALS,
            onClick = { onTabSelect(AppTab.GOALS) },
            icon = { Icon(Icons.Default.Flag, contentDescription = "Goals") },
            label = { Text("Goals", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.ACCOUNTS,
            onClick = { onTabSelect(AppTab.ACCOUNTS) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts") },
            label = { Text("Accounts", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == AppTab.SETTINGS,
            onClick = { onTabSelect(AppTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp) }
        )
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    accountName: String,
    categoryName: String,
    categoryIcon: String,
    isPrivacyMasked: Boolean,
    onDelete: (() -> Unit)? = null
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val sign = if (isExpense) "-" else "+"
    // Bright RED for Debits/Expenses, Bright GREEN for Credits/Income
    val color = if (isExpense) Color(0xFFC62828) else Color(0xFF2E7D32)

    val displayTitle = if (transaction.merchant.isNotBlank() && !transaction.merchant.equals("CRED", ignoreCase = true)) {
        transaction.merchant
    } else if (transaction.bankName.isNotBlank() && !transaction.bankName.equals("CRED", ignoreCase = true)) {
        transaction.bankName
    } else "Bank Transaction"

    val accountOrBankDisplay = if (transaction.bankName.isNotBlank() && !transaction.bankName.equals("CRED", ignoreCase = true)) {
        transaction.bankName
    } else accountName

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
                        .background(if (isExpense) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = categoryIcon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2D332A)
                    )
                    Text(
                        text = "$categoryName • $accountOrBankDisplay",
                        fontSize = 11.sp,
                        color = Color(0xFF7C8079)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPrivacyMasked) "₹ • • •" else "$sign${Money.format(transaction.amountMinor, absolute = true)}",
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
