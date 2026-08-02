package com.example.mstrackerapp.presentation.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.domain.models.Account
import com.example.mstrackerapp.domain.models.AccountType
import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.presentation.components.MultiSelectDropdown
import com.example.mstrackerapp.utils.Money

private data class BankLensSummary(
    val name: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netMinor: Long,
    val txCount: Int
)

@Composable
fun AccountsScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedBanks by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Banks discovered from real SMS activity (+ cash)
    val discoveredBanks = remember(uiState.transactions, uiState.accounts) {
        val fromTx = uiState.transactions
            .map { normalizeBankLabel(it.bankName) }
            .filter { it.isNotBlank() && it != "Unknown" }
            .distinct()
            .sorted()
        val fromAccounts = uiState.accounts
            .map { normalizeBankLabel(it.institution.ifBlank { it.name }) }
            .filter { it.isNotBlank() }
        (fromTx + fromAccounts + listOf("Cash / Wallet")).distinct().sorted()
    }

    val lensTxs = remember(uiState.transactions, selectedBanks) {
        if (selectedBanks.isEmpty()) uiState.transactions
        else uiState.transactions.filter { tx ->
            selectedBanks.any { bankMatches(tx.bankName, it) } ||
                (selectedBanks.contains("Cash / Wallet") && isCashLike(tx))
        }
    }

    val incomeTotal = lensTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val expenseTotal = lensTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val netFlow = incomeTotal - expenseTotal

    val bankPortfolios = remember(lensTxs) {
        lensTxs
            .groupBy { normalizeBankLabel(it.bankName).ifBlank { "Other" } }
            .map { (bank, txs) ->
                val inc = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                val exp = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                BankLensSummary(bank, inc, exp, inc - exp, txs.size)
            }
            .sortedByDescending { it.txCount }
    }

    // Linked manual accounts still shown when they match the lens
    val filteredAccounts = remember(uiState.accounts, selectedBanks, uiState.transactions) {
        if (selectedBanks.isEmpty()) uiState.accounts
        else uiState.accounts.filter { acc ->
            selectedBanks.any { bank ->
                accountMatchesBank(acc, bank) ||
                    uiState.transactions.any { tx ->
                        tx.accountId == acc.id && bankMatches(tx.bankName, bank)
                    }
            }
        }
    }

    fun moneyColor(amountMinor: Long, treatPositiveAsGood: Boolean = true): Color {
        return when {
            amountMinor > 0 && treatPositiveAsGood -> Color(0xFF1B5E20)   // green income/positive
            amountMinor < 0 && treatPositiveAsGood -> Color(0xFFB71C1C)   // red negative
            amountMinor > 0 && !treatPositiveAsGood -> Color(0xFFB71C1C) // red = money out / owed
            amountMinor < 0 && !treatPositiveAsGood -> Color(0xFF1B5E20)
            else -> Color(0xFF2D332A)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Accounts", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D332A))
                Text("Bank lens from your SMS activity", fontSize = 12.sp, color = Color(0xFF555A52))
            }
            IconButton(
                onClick = { showAddAccountDialog = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF2E6244),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        MultiSelectDropdown(
            label = "Banks in your data",
            options = discoveredBanks,
            selected = selectedBanks,
            onSelectedChange = { selectedBanks = it },
            allLabel = "All banks"
        )

        Spacer(modifier = Modifier.height(12.dp))
        val filterLabel = when {
            selectedBanks.isEmpty() -> "All banks"
            selectedBanks.size == 1 -> selectedBanks.first()
            else -> "${selectedBanks.size} banks"
        }
        Text("Cash flow · $filterLabel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D332A))
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowMiniCard(
                title = "In",
                amountMinor = incomeTotal,
                color = Color(0xFF1B5E20),
                bg = Color(0xFFE8F5E9),
                icon = Icons.Default.TrendingUp,
                masked = uiState.isPrivacyMasked,
                modifier = Modifier.weight(1f)
            )
            FlowMiniCard(
                title = "Out",
                amountMinor = expenseTotal,
                color = Color(0xFFB71C1C),
                bg = Color(0xFFFFEBEE),
                icon = Icons.Default.TrendingDown,
                masked = uiState.isPrivacyMasked,
                modifier = Modifier.weight(1f)
            )
            FlowMiniCard(
                title = "Net",
                amountMinor = netFlow,
                color = moneyColor(netFlow, treatPositiveAsGood = true),
                bg = if (netFlow >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                icon = Icons.Default.AccountBalanceWallet,
                masked = uiState.isPrivacyMasked,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                Text("By bank", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D332A))
            }

            if (bankPortfolios.isEmpty()) {
                item {
                    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No SMS activity for this bank filter", color = Color(0xFF555A52), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(bankPortfolios, key = { it.name }) { portfolio ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF2E6244), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(portfolio.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2D332A))
                                }
                                Text("${portfolio.txCount} txs", fontSize = 11.sp, color = Color(0xFF555A52))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("In", fontSize = 10.sp, color = Color(0xFF555A52))
                                    Text(
                                        if (uiState.isPrivacyMasked) "••••" else Money.format(portfolio.incomeMinor),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Out", fontSize = 10.sp, color = Color(0xFF555A52))
                                    Text(
                                        if (uiState.isPrivacyMasked) "••••" else Money.format(portfolio.expenseMinor),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFB71C1C)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net", fontSize = 10.sp, color = Color(0xFF555A52))
                                    Text(
                                        if (uiState.isPrivacyMasked) "••••" else Money.format(portfolio.netMinor),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = moneyColor(portfolio.netMinor)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredAccounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Linked accounts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D332A))
                }
                items(filteredAccounts, key = { it.id }) { account ->
                    val income = uiState.transactions
                        .filter { it.accountId == account.id && it.type == TransactionType.INCOME }
                        .sumOf { it.amountMinor }
                    val expense = uiState.transactions
                        .filter { it.accountId == account.id && it.type == TransactionType.EXPENSE }
                        .sumOf { it.amountMinor }
                    val balance = account.startingBalanceMinor + income - expense
                    val isCc = account.type == AccountType.CREDIT_CARD
                    // Credit card: amount owed shown as positive → red; bank balance positive → green
                    val balColor = if (isCc) {
                        if (balance > 0) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                    } else {
                        moneyColor(balance)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isCc) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                            null,
                                            tint = Color(0xFF2E6244)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(account.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2D332A))
                                    Text(account.institution.ifBlank { account.type.name }, fontSize = 11.sp, color = Color(0xFF555A52))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (uiState.isPrivacyMasked) "••••••••" else Money.format(balance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = balColor
                                )
                                Text(
                                    if (isCc) "Amount owed" else "Balance",
                                    fontSize = 10.sp,
                                    color = Color(0xFF555A52)
                                )
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

@Composable
private fun FlowMiniCard(
    title: String,
    amountMinor: Long,
    color: Color,
    bg: Color,
    icon: ImageVector,
    masked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (masked) "••••" else Money.format(amountMinor),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun normalizeBankLabel(raw: String): String {
    val s = raw.trim()
    if (s.isBlank()) return ""
    val lower = s.lowercase()
    return when {
        lower.contains("icici") -> "ICICI Bank"
        lower.contains("hdfc") -> "HDFC Bank"
        lower.contains("sbi") || lower.contains("state bank") -> "SBI"
        lower.contains("canara") -> "Canara Bank"
        lower.contains("axis") -> "Axis Bank"
        lower.contains("kotak") -> "Kotak Bank"
        lower.contains("indus") -> "IndusInd Bank"
        lower.contains("paytm") -> "Paytm"
        lower.contains("phonepe") -> "PhonePe"
        else -> s.take(24)
    }
}

private fun bankMatches(bankName: String, filter: String): Boolean {
    if (filter == "Cash / Wallet") return false
    val n = normalizeBankLabel(bankName)
    return n.equals(filter, ignoreCase = true) ||
        bankName.contains(filter.replace(" Bank", ""), ignoreCase = true)
}

private fun isCashLike(tx: Transaction): Boolean {
    val b = tx.bankName.lowercase()
    return b.contains("cash") || b.contains("wallet") || b.contains("paytm") || b.isBlank()
}

private fun accountMatchesBank(account: Account, filter: String): Boolean {
    if (filter == "Cash / Wallet") {
        return account.type == AccountType.CASH || account.type == AccountType.WALLET || account.type == AccountType.UPI
    }
    val full = "${account.institution} ${account.name}"
    return normalizeBankLabel(full).equals(filter, ignoreCase = true) ||
        full.contains(filter.replace(" Bank", ""), ignoreCase = true)
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
