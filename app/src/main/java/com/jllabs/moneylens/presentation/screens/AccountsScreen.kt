package com.jllabs.moneylens.presentation.screens

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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.jllabs.moneylens.domain.accounts.SmsAccountAggregator
import com.jllabs.moneylens.domain.accounts.SmsAccountRow
import com.jllabs.moneylens.domain.models.AccountType
import com.jllabs.moneylens.theme.rememberAppUiColors

@Composable
fun AccountsScreen(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<SmsAccountRow?>(null) }
    var bankSectionExpanded by remember { mutableStateOf(true) }
    var loanSectionExpanded by remember { mutableStateOf(true) }
    var cardSectionExpanded by remember { mutableStateOf(true) }
    val ui = rememberAppUiColors(uiState.isDarkMode)

    val smsAccounts = uiState.smsAccounts

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
            isPrivacyMasked = false,
            isDarkMode = uiState.isDarkMode,
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
            Text("Accounts", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ui.ink)
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = ui.ink)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = ui.card
                ) {
                    DropdownMenuItem(
                        text = { Text("Add account", color = ui.ink) },
                        onClick = {
                            menuExpanded = false
                            showAddAccountDialog = true
                        }
                    )
                }
            }
        }

        Text(
            "Balance powered from SMS, actual may vary",
            fontSize = 12.sp,
            color = ui.muted
        )
        if (smsAccounts.any { it.balanceMinor != null }) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recently updated", fontSize = 12.sp, color = Color(0xFF81C784))
            }
        }

        val bankAccounts = remember(smsAccounts) {
            smsAccounts.filter {
                !it.isCreditCard && !it.isLoanAccount &&
                    !it.key.contains("|CC|") && !it.key.contains("|LN|")
            }.sortedBy { it.displayName.lowercase() }
        }
        val loanAccounts = remember(smsAccounts) {
            smsAccounts.filter { it.isLoanAccount || it.key.contains("|LN|") }
                .sortedBy { it.displayName.lowercase() }
        }
        val creditCards = remember(smsAccounts) {
            smsAccounts.filter { it.isCreditCard || it.key.contains("|CC|") }
                .sortedBy { it.displayName.lowercase() }
        }

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            if (bankAccounts.isEmpty() && loanAccounts.isEmpty() && creditCards.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 24.dp)) {
                        Text(
                            "No accounts found yet",
                            fontWeight = FontWeight.SemiBold,
                            color = ui.ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Re-scan SMS after updating. Supports A/c XX1234, Card XX0018, and similar.",
                            fontSize = 13.sp,
                            color = ui.muted
                        )
                    }
                }
            } else {
                item {
                    SectionHeader(
                        title = "Bank accounts (${bankAccounts.size})",
                        icon = Icons.Default.AccountBalance,
                        iconTint = ui.ink,
                        ink = ui.ink,
                        muted = ui.muted,
                        divider = ui.divider,
                        expanded = bankSectionExpanded,
                        onToggle = { bankSectionExpanded = !bankSectionExpanded }
                    )
                }
                if (bankSectionExpanded) {
                    if (bankAccounts.isEmpty()) {
                        item {
                            Text(
                                "No bank accounts found yet",
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontSize = 13.sp,
                                color = ui.muted
                            )
                        }
                    } else {
                        items(bankAccounts, key = { it.key }) { account ->
                            FinanceAccountRow(
                                account = account,
                                ink = ui.ink,
                                muted = ui.muted,
                                onClick = { selectedAccount = account }
                            )
                            HorizontalDivider(color = ui.divider, thickness = 1.dp)
                        }
                    }
                }

                if (loanAccounts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(
                            title = "Loan accounts (${loanAccounts.size})",
                            icon = Icons.Default.AccountBalance,
                            iconTint = Color(0xFFCE93D8),
                            ink = ui.ink,
                            muted = ui.muted,
                            divider = ui.divider,
                            expanded = loanSectionExpanded,
                            onToggle = { loanSectionExpanded = !loanSectionExpanded }
                        )
                    }
                    if (loanSectionExpanded) {
                        items(loanAccounts, key = { it.key }) { account ->
                            FinanceAccountRow(
                                account = account,
                                ink = ui.ink,
                                muted = ui.muted,
                                onClick = { selectedAccount = account }
                            )
                            HorizontalDivider(color = ui.divider, thickness = 1.dp)
                        }
                    }
                }

                if (creditCards.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(
                            title = "Credit cards (${creditCards.size})",
                            icon = Icons.Default.CreditCard,
                            iconTint = Color(0xFF90CAF9),
                            ink = ui.ink,
                            muted = ui.muted,
                            divider = ui.divider,
                            expanded = cardSectionExpanded,
                            onToggle = { cardSectionExpanded = !cardSectionExpanded }
                        )
                    }
                    if (cardSectionExpanded) {
                        items(creditCards, key = { it.key }) { account ->
                            FinanceAccountRow(
                                account = account,
                                ink = ui.ink,
                                muted = ui.muted,
                                onClick = { selectedAccount = account }
                            )
                            HorizontalDivider(color = ui.divider, thickness = 1.dp)
                        }
                    }
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
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "We do not upload your SMS data anywhere. Your personal information stays in your device and is not shared anywhere.",
                        fontSize = 12.sp,
                        color = ui.muted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            isDarkMode = uiState.isDarkMode,
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, balance, icon ->
                viewModel.addAccount(name, type, balance, icon)
                showAddAccountDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    ink: Color,
    muted: Color,
    divider: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = muted
        )
    }
    HorizontalDivider(color = divider, thickness = 1.dp)
}

@Composable
private fun FinanceAccountRow(
    account: SmsAccountRow,
    ink: Color,
    muted: Color,
    onClick: () -> Unit
) {
    val dateLabel = SmsAccountAggregator.formatBalanceDate(
        account.balanceDate ?: account.lastActivityDate
    )
    val balance = account.balanceMinor
    val balColor = when {
        balance == null -> muted
        balance < 0 -> Color(0xFFEF9A9A)
        account.isCreditCard -> Color(0xFF90CAF9)
        account.isLoanAccount -> Color(0xFFCE93D8)
        else -> ink
    }
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
                color = ink
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                when {
                    dateLabel.isNotBlank() && account.isCreditCard -> "Avl limit on $dateLabel"
                    dateLabel.isNotBlank() && balance != null -> "Avbl bal on $dateLabel"
                    dateLabel.isNotBlank() -> "Updated $dateLabel"
                    account.isCreditCard -> "Available limit"
                    account.isLoanAccount -> "Loan account"
                    account.isFasTagAccount -> "FASTag wallet"
                    balance == null -> "From SMS"
                    else -> "Balance"
                },
                fontSize = 12.sp,
                color = muted
            )
        }
        Text(
            when {
                balance == null -> "—"
                else -> formatRupee(balance)
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = balColor,
            modifier = Modifier.padding(end = 4.dp)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open account",
            tint = muted
        )
    }
}

@Composable
fun AddAccountDialog(
    isDarkMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balanceRupees: Double, icon: String) -> Unit
) {
    val ui = rememberAppUiColors(isDarkMode)
    var name by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("ICICI Bank") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var balanceText by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🏦") }

    val majorBanks = listOf("ICICI Bank", "HDFC Bank", "SBI", "Canara Bank", "Axis Bank", "Kotak Bank", "Paytm", "Cash / Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ui.card,
        titleContentColor = ui.ink,
        textContentColor = ui.ink,
        modifier = Modifier.widthIn(max = 520.dp).imePadding(),
        title = { Text("Add New Bank Account", fontWeight = FontWeight.Bold, color = ui.ink) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Bank / Institution", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ui.ink)
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
                            label = { Text(bank, fontSize = 10.sp, color = if (selectedBank == bank) Color.White else ui.ink) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E6244),
                                selectedLabelColor = Color.White,
                                containerColor = ui.chip,
                                labelColor = ui.ink
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(isDarkMode),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ui.ink)
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
                            label = { Text(type.name, fontSize = 10.sp, color = if (selectedType == type) Color.White else ui.ink) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E6244),
                                selectedLabelColor = Color.White,
                                containerColor = ui.chip,
                                labelColor = ui.ink
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Starting Balance / Owed (₹)") },
                    colors = com.jllabs.moneylens.presentation.components.appTextFieldColors(isDarkMode),
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
                Text("Cancel", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            }
        }
    )
}
