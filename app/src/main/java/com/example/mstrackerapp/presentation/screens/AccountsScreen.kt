package com.example.mstrackerapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.domain.models.Account
import com.example.mstrackerapp.domain.models.AccountType
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.utils.Money

@Composable
fun AccountsScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedBankFilter by remember { mutableStateOf("All Banks") }

    val bankFilters = listOf("All Banks", "ICICI Bank", "HDFC Bank", "SBI", "Canara Bank", "Axis Bank", "Kotak Bank", "Cash / Wallet")

    // Account Balance Calculations per account
    val accountBalances = uiState.accounts.associate { account ->
        val income = uiState.transactions
            .filter { it.accountId == account.id && it.type == TransactionType.INCOME }
            .sumOf { it.amountMinor }
        val expense = uiState.transactions
            .filter { it.accountId == account.id && it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor }

        val calculatedCurrentBalance = account.startingBalanceMinor + income - expense
        account.id to calculatedCurrentBalance
    }

    val bankTotal = uiState.accounts.filter { it.type == AccountType.BANK || it.type == AccountType.SAVINGS }
        .sumOf { accountBalances[it.id] ?: 0L }
    val cashWalletTotal = uiState.accounts.filter { it.type == AccountType.CASH || it.type == AccountType.WALLET || it.type == AccountType.UPI }
        .sumOf { accountBalances[it.id] ?: 0L }
    val creditTotal = uiState.accounts.filter { it.type == AccountType.CREDIT_CARD }
        .sumOf { accountBalances[it.id] ?: 0L }

    // Filter accounts by selected Bank Filter Chip with keyword matching
    val filteredAccounts = remember(uiState.accounts, selectedBankFilter) {
        if (selectedBankFilter == "All Banks") {
            uiState.accounts
        } else {
            uiState.accounts.filter { acc -> matchesBankFilter(acc, selectedBankFilter) }
        }
    }

    // Group accounts by Bank Name
    val groupedAccounts = remember(filteredAccounts) {
        filteredAccounts.groupBy { acc -> getBankClassificationName(acc) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Account Management",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D332A)
                )
                Text(
                    text = "Classified by Bank (ICICI, HDFC, SBI, Canara, Axis, Kotak)",
                    fontSize = 12.sp,
                    color = Color(0xFF7C8079)
                )
            }

            IconButton(
                onClick = { showAddAccountDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF3B7A57), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bank Classification Chips Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bankFilters) { filter ->
                val isSelected = selectedBankFilter.equals(filter, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedBankFilter = filter },
                    label = { Text(filter, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B7A57), selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Account Summary Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Bank Savings (Debit)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bank (Debit)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isPrivacyMasked) "••••" else Money.format(bankTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Cash & Wallet
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF00838F), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cash & Wallet", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00838F))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isPrivacyMasked) "••••" else Money.format(cashWalletTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00838F)
                    )
                }
            }

            // Credit Cards (Liability / Credit)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Credit (Owed)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isPrivacyMasked) "••••" else Money.format(creditTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accounts List Grouped by Bank Headers
        if (groupedAccounts.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No accounts found for $selectedBankFilter", color = Color(0xFF7C8079), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedAccounts.forEach { (bankGroupTitle, bankAccountList) ->
                    item(key = "bank-header-$bankGroupTitle") {
                        Surface(
                            color = Color(0xFFE4E8E3),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🏛️ $bankGroupTitle",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B7A57)
                                )
                                Text(
                                    text = "${bankAccountList.size} ${if (bankAccountList.size == 1) "Account" else "Accounts"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF555A52)
                                )
                            }
                        }
                    }

                    items(bankAccountList, key = { it.id }) { account ->
                        val currentBalanceMinor = accountBalances[account.id] ?: account.startingBalanceMinor
                        val linkedTxCount = uiState.transactions.count { it.accountId == account.id }
                        val (badgeLabel, badgeBg, badgeText) = getBankBadgeInfo(account)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = badgeLabel,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeText
                                        )
                                    }

                                    Text(
                                        text = if (account.includeInNetWorth) "In Net Worth" else "Excluded",
                                        fontSize = 10.sp,
                                        color = Color(0xFF7C8079)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFFF4F3EF),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(account.icon, fontSize = 22.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = account.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF2D332A)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$linkedTxCount Transactions",
                                                fontSize = 11.sp,
                                                color = Color(0xFF7C8079)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (uiState.isPrivacyMasked) "••••••••" else Money.format(currentBalanceMinor),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (account.type == AccountType.CREDIT_CARD) Color(0xFFC62828) else Color(0xFF3B7A57)
                                        )
                                        Text(
                                            text = if (account.type == AccountType.CREDIT_CARD) "Current Owed" else "Available Balance",
                                            fontSize = 10.sp,
                                            color = Color(0xFF7C8079)
                                        )
                                    }
                                }
                            }
                        }
                    }
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

private fun matchesBankFilter(account: Account, filter: String): Boolean {
    if (filter == "All Banks") return true
    val fullStr = "${account.institution} ${account.name}".lowercase()
    return when (filter) {
        "ICICI Bank" -> fullStr.contains("icici")
        "HDFC Bank" -> fullStr.contains("hdfc")
        "SBI" -> fullStr.contains("sbi") || fullStr.contains("state bank")
        "Canara Bank" -> fullStr.contains("canara")
        "Axis Bank" -> fullStr.contains("axis")
        "Kotak Bank" -> fullStr.contains("kotak")
        "Cash / Wallet" -> account.type == AccountType.CASH || account.type == AccountType.WALLET || fullStr.contains("cash") || fullStr.contains("paytm") || fullStr.contains("wallet")
        else -> fullStr.contains(filter.lowercase().replace(" bank", "").trim())
    }
}

private fun getBankClassificationName(account: Account): String {
    val searchStr = "${account.institution} ${account.name}".uppercase()
    return when {
        searchStr.contains("ICICI") -> "ICICI Bank"
        searchStr.contains("HDFC") -> "HDFC Bank"
        searchStr.contains("SBI") || searchStr.contains("STATE BANK") -> "State Bank of India (SBI)"
        searchStr.contains("CANARA") -> "Canara Bank"
        searchStr.contains("AXIS") -> "Axis Bank"
        searchStr.contains("KOTAK") -> "Kotak Mahindra Bank"
        searchStr.contains("BARODA") || searchStr.contains("BOB") -> "Bank of Baroda"
        searchStr.contains("PNB") || searchStr.contains("PUNJAB") -> "Punjab National Bank"
        account.type == AccountType.CASH || account.type == AccountType.WALLET || account.type == AccountType.UPI -> "Cash & Digital Wallets"
        else -> "Other Bank / Accounts"
    }
}

private fun getBankBadgeInfo(account: Account): Triple<String, Color, Color> {
    val searchStr = "${account.institution} ${account.name}".uppercase()
    return when {
        searchStr.contains("ICICI") -> Triple("🏛️ ICICI BANK", Color(0xFFFFE0B2), Color(0xFFE65100))
        searchStr.contains("HDFC") -> Triple("🏛️ HDFC BANK", Color(0xFFE3F2FD), Color(0xFF0D47A1))
        searchStr.contains("SBI") || searchStr.contains("STATE BANK") -> Triple("🏛️ SBI", Color(0xFFE1F5FE), Color(0xFF0277BD))
        searchStr.contains("CANARA") -> Triple("🏛️ CANARA BANK", Color(0xFFFFF3E0), Color(0xFFEF6C00))
        searchStr.contains("AXIS") -> Triple("🏛️ AXIS BANK", Color(0xFFFCE4EC), Color(0xFFC2185B))
        searchStr.contains("KOTAK") -> Triple("🏛️ KOTAK BANK", Color(0xFFFFEBEE), Color(0xFFB71C1C))
        searchStr.contains("BARODA") || searchStr.contains("BOB") -> Triple("🏛️ BANK OF BARODA", Color(0xFFFFF8E1), Color(0xFFF57F17))
        searchStr.contains("PNB") || searchStr.contains("PUNJAB") -> Triple("🏛️ PNB", Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        account.type == AccountType.CREDIT_CARD -> Triple("💳 CREDIT CARD", Color(0xFFFFEBEE), Color(0xFFC62828))
        account.type == AccountType.CASH -> Triple("💵 CASH", Color(0xFFFFF8E1), Color(0xFFF57F17))
        account.type == AccountType.WALLET || account.type == AccountType.UPI -> Triple("👛 DIGITAL WALLET / UPI", Color(0xFFE0F7FA), Color(0xFF00838F))
        else -> Triple("🏦 BANK (SAVINGS)", Color(0xFFE8F5E9), Color(0xFF2E7D32))
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
        modifier = Modifier
            .widthIn(max = 520.dp)
            .imePadding(),
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
                            label = { Text(bank, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B7A57),
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
                            label = { Text(type.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B7A57),
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
                    onConfirm(name, selectedType, bal, icon)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57), contentColor = Color.White),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Add Account", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF3B7A57)),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Cancel", color = Color(0xFF3B7A57), fontWeight = FontWeight.Bold)
            }
        }
    )
}
