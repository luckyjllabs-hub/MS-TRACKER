package com.example.mstrackerapp.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.domain.accounts.SmsAccountAggregator
import com.example.mstrackerapp.domain.accounts.SmsAccountRow
import com.example.mstrackerapp.domain.models.AccountType

@Composable
fun AccountsScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<SmsAccountRow?>(null) }

    val smsAccounts = remember(uiState.allTransactions) {
        SmsAccountAggregator.derive(uiState.allTransactions)
    }

    val activeAccount = selectedAccount?.let { sel ->
        smsAccounts.firstOrNull { it.key == sel.key } ?: sel
    }

    BackHandler(enabled = activeAccount != null) {
        selectedAccount = null
    }

    if (activeAccount != null) {
        AccountLedgerScreen(
            account = activeAccount,
            transactions = uiState.allTransactions,
            isPrivacyMasked = uiState.isPrivacyMasked,
            onBack = { selectedAccount = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Finance", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D332A))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF2D332A))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add account") },
                        onClick = {
                            menuExpanded = false
                            showAddAccountDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show balance", fontSize = 14.sp, color = Color(0xFF2D332A))
            Switch(
                checked = !uiState.isPrivacyMasked,
                onCheckedChange = { viewModel.togglePrivacyMask() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2E7DFF),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFB0B0B0)
                )
            )
        }

        Text(
            "Balance powered from SMS, actual may vary",
            fontSize = 12.sp,
            color = Color(0xFF555A52)
        )
        if (smsAccounts.any { it.balanceMinor != null }) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recently updated", fontSize = 12.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color(0xFF2D332A),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Accounts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D332A))
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = Color(0xFFE0E4DC), thickness = 1.dp)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            if (smsAccounts.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 24.dp)) {
                        Text(
                            "No bank accounts found yet",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D332A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Accounts appear when SMS includes A/c XX1234. Re-scan SMS after updating.",
                            fontSize = 13.sp,
                            color = Color(0xFF555A52)
                        )
                    }
                }
            } else {
                items(smsAccounts, key = { it.key }) { account ->
                    FinanceAccountRow(
                        account = account,
                        showBalance = !uiState.isPrivacyMasked,
                        onClick = { selectedAccount = account }
                    )
                    HorizontalDivider(color = Color(0xFFE8EBE6), thickness = 1.dp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF2E7DFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "We do not upload your SMS data anywhere. Your personal information stays in your device and is not shared anywhere.",
                        fontSize = 12.sp,
                        color = Color(0xFF555A52),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, balance, icon ->
                viewModel.addAccount(name, type, balance, icon)
                showAddAccountDialog = false
            }
        )
    }
}

@Composable
private fun FinanceAccountRow(
    account: SmsAccountRow,
    showBalance: Boolean,
    onClick: () -> Unit
) {
    val dateLabel = SmsAccountAggregator.formatBalanceDate(
        account.balanceDate ?: account.lastActivityDate
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF2D332A)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                when {
                    account.balanceMinor != null && dateLabel.isNotBlank() ->
                        "Avbl bal on $dateLabel"
                    account.balanceMinor != null -> "Available balance"
                    dateLabel.isNotBlank() -> "Last activity $dateLabel"
                    else -> "No balance in SMS yet"
                },
                fontSize = 12.sp,
                color = Color(0xFF888C84)
            )
        }
        Text(
            when {
                account.balanceMinor == null -> "—"
                showBalance -> formatRupee(account.balanceMinor)
                else -> "••••••••"
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color(0xFF2D332A),
            modifier = Modifier.padding(end = 4.dp)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open account",
            tint = Color(0xFF9AA09A)
        )
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balanceRupees: Double, icon: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("ICICI Bank") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var balanceText by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🏦") }

    val majorBanks = listOf("ICICI Bank", "HDFC Bank", "SBI", "Canara Bank", "Axis Bank", "Kotak Bank", "Paytm", "Cash / Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF2D332A),
        textContentColor = Color(0xFF2D332A),
        modifier = Modifier.widthIn(max = 520.dp).imePadding(),
        title = { Text("Add New Bank Account", fontWeight = FontWeight.Bold, color = Color(0xFF2D332A)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Bank / Institution", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D332A))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(majorBanks) { bank ->
                        FilterChip(
                            selected = selectedBank == bank,
                            onClick = {
                                selectedBank = bank
                                if (name.isBlank() || majorBanks.any { name.startsWith(it) }) {
                                    name = "$bank Savings"
                                }
                            },
                            label = { Text(bank, fontSize = 10.sp, color = if (selectedBank == bank) Color.White else Color(0xFF2D332A)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E6244),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE4E8E3),
                                labelColor = Color(0xFF2D332A)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    colors = com.example.mstrackerapp.presentation.components.appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D332A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccountType.values().take(4).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                icon = when (type) {
                                    AccountType.BANK -> "🏦"
                                    AccountType.CASH -> "💵"
                                    AccountType.WALLET -> "👛"
                                    AccountType.CREDIT_CARD -> "💳"
                                    AccountType.UPI -> "📱"
                                    AccountType.SAVINGS -> "💰"
                                }
                            },
                            label = { Text(type.name, fontSize = 10.sp, color = if (selectedType == type) Color.White else Color(0xFF2D332A)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E6244),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE4E8E3),
                                labelColor = Color(0xFF2D332A)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Starting Balance / Owed (₹)") },
                    colors = com.example.mstrackerapp.presentation.components.appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    onConfirm(name.ifBlank { "$selectedBank Savings" }, selectedType, bal, icon)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6244), contentColor = Color.White),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Add Account", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF2E6244), fontWeight = FontWeight.Bold)
            }
        }
    )
}
