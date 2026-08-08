package com.jllabs.moneylens.presentation.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.data.parser.SmsInboxScanner
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.presentation.navigation.AppTab
import com.jllabs.moneylens.theme.rememberAppUiColors
import com.jllabs.moneylens.utils.BackupImporter
import com.jllabs.moneylens.utils.CsvExporter
import com.jllabs.moneylens.utils.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MoneyLensUiState,
    viewModel: MoneyLensViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ui = rememberAppUiColors(uiState.isDarkMode)

    var isExportingRawSms by remember { mutableStateOf(false) }
    var isExportingBackup by remember { mutableStateOf(false) }
    var isImportingBackup by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var budgetDialogCategoryId by remember { mutableStateOf<String?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    fun runImport(uri: android.net.Uri) {
        coroutineScope.launch {
            isImportingBackup = true
            try {
                val payload = withContext(Dispatchers.IO) {
                    BackupImporter.readUri(context, uri)
                }
                if (payload.transactions.isEmpty() &&
                    payload.categories.isEmpty() &&
                    payload.accounts.isEmpty()
                ) {
                    Toast.makeText(context, "No backup data found in file", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.importBackup(payload) { count ->
                        Toast.makeText(
                            context,
                            "Restored $count transactions" +
                                (if (payload.accounts.isNotEmpty()) " · ${payload.accounts.size} accounts" else "") +
                                (if (payload.categories.isNotEmpty()) " · ${payload.categories.size} categories" else ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isImportingBackup = false
                pendingImportUri = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ui.ink)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ui.card),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SMS Inbox", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ui.ink)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onSearchQueryChange("")
                                    viewModel.selectTab(AppTab.SMS_INBOX)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgedBox(
                                badge = {
                                    if (uiState.smsQueue.isNotEmpty()) {
                                        Badge(containerColor = ui.accent) {
                                            Text(
                                                text = uiState.smsQueue.size.toString(),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = ui.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Open SMS inbox", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ui.ink)
                                Text(
                                    if (uiState.smsQueue.isEmpty()) "Review queued bank SMS"
                                    else "${uiState.smsQueue.size} messages waiting",
                                    fontSize = 12.sp,
                                    color = ui.muted
                                )
                            }
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ui.accent)
                        }
                        HorizontalDivider(color = ui.divider)
                        Button(
                            onClick = {
                                SmsInboxScanner.scanExistingInbox(context)
                                Toast.makeText(context, "SMS rescan started", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rescan SMS inbox")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ui.card),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Budgets", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ui.ink)
                        Text("Tap a budget to view or edit its limit", fontSize = 12.sp, color = ui.muted)
                        val activeBudgets = uiState.categories.filter { it.monthlyLimitMinor > 0 }
                        if (activeBudgets.isEmpty()) {
                            Text("No category budgets set", fontSize = 12.sp, color = ui.muted)
                        } else {
                            activeBudgets.forEach { cat ->
                                val spent = uiState.allTransactions
                                    .filter { it.categoryId == cat.id && it.type == TransactionType.EXPENSE }
                                    .sumOf { it.amountMinor }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { budgetDialogCategoryId = cat.id }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${cat.icon} ${cat.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ui.ink)
                                        Text(
                                            "Limit ${Money.format(cat.monthlyLimitMinor)} · Spent ${Money.format(spent)}",
                                            fontSize = 11.sp,
                                            color = ui.muted
                                        )
                                    }
                                    IconButton(onClick = { viewModel.updateCategoryLimit(cat.id, 0.0) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove budget", tint = Color(0xFFEF9A9A))
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { showAddBudgetDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add budget")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ui.card),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ui.ink)
                        Text(
                            "Export a full backup to move devices, or import a MoneyLens JSON / transactions CSV.",
                            fontSize = 12.sp,
                            color = ui.muted
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExportingBackup = true
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            BackupImporter.writeBackupFile(
                                                context,
                                                uiState.allTransactions,
                                                uiState.categories,
                                                uiState.accounts
                                            )
                                        }
                                        BackupImporter.shareBackupFile(context, file)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isExportingBackup = false
                                    }
                                }
                            },
                            enabled = !isExportingBackup && !isImportingBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isExportingBackup) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preparing backup…")
                            } else {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export backup (JSON)")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/*",
                                        "text/csv",
                                        "application/csv",
                                        "*/*"
                                    )
                                )
                            },
                            enabled = !isExportingBackup && !isImportingBackup,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ui.accent)
                        ) {
                            if (isImportingBackup) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ui.accent, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importing…")
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import / restore backup")
                            }
                        }
                        HorizontalDivider(color = ui.divider)
                        Text("Other exports", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ui.muted)
                        Button(
                            onClick = {
                                val file = CsvExporter.exportTransactionsToCsv(
                                    context, uiState.allTransactions, uiState.categories, uiState.accounts
                                )
                                CsvExporter.shareCsvFile(context, file, subject = "MoneyLens - Transactions")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export transactions (CSV)")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExportingRawSms = true
                                    try {
                                        val file = CsvExporter.exportRawSmsToCsv(context)
                                        CsvExporter.shareCsvFile(context, file, subject = "MoneyLens - SMS Raw Data")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isExportingRawSms = false
                                    }
                                }
                            },
                            enabled = !isExportingRawSms,
                            colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isExportingRawSms) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reading SMS…")
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export SMS Raw Data")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportConfirm && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            containerColor = ui.card,
            title = { Text("Restore backup?", fontWeight = FontWeight.Bold, color = ui.ink) },
            text = {
                Text(
                    "Imported accounts, categories, and transactions will be merged with your current data. Duplicates are skipped.",
                    color = ui.muted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        showImportConfirm = false
                        if (uri != null) runImport(uri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ui.accent)
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        pendingImportUri = null
                    }
                ) { Text("Cancel", color = ui.muted) }
            }
        )
    }

    val editCategoryId = budgetDialogCategoryId
    if (editCategoryId != null || showAddBudgetDialog) {
        BudgetEditDialog(
            categories = uiState.categories,
            initialCategoryId = editCategoryId,
            isDark = uiState.isDarkMode,
            onDismiss = {
                budgetDialogCategoryId = null
                showAddBudgetDialog = false
            },
            onSave = { categoryId, amountRupees ->
                viewModel.updateCategoryLimit(categoryId, amountRupees)
                budgetDialogCategoryId = null
                showAddBudgetDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditDialog(
    categories: List<Category>,
    initialCategoryId: String?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (categoryId: String, amountRupees: Double) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategoryId by remember {
        mutableStateOf(initialCategoryId ?: categories.firstOrNull()?.id.orEmpty())
    }
    val selected = categories.find { it.id == selectedCategoryId }
    var amountText by remember(selectedCategoryId) {
        mutableStateOf(
            selected?.takeIf { it.monthlyLimitMinor > 0 }?.let { (it.monthlyLimitMinor / 100).toString() }.orEmpty()
        )
    }
    val ui = rememberAppUiColors(isDark)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ui.card,
        title = {
            Text(
                if (initialCategoryId != null) "Edit budget" else "Add budget",
                fontWeight = FontWeight.Bold,
                color = ui.ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Category", fontSize = 12.sp, color = ui.muted)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.let { "${it.icon} ${it.name}" }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ui.ink,
                            unfocusedTextColor = ui.ink,
                            focusedContainerColor = ui.card,
                            unfocusedContainerColor = ui.card
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.name}", color = ui.ink) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    amountText = if (cat.monthlyLimitMinor > 0) (cat.monthlyLimitMinor / 100).toString() else ""
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text("Monthly limit (₹)", fontSize = 12.sp, color = ui.muted)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ui.ink,
                        unfocusedTextColor = ui.ink,
                        focusedContainerColor = ui.card,
                        unfocusedContainerColor = ui.card
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (selectedCategoryId.isNotBlank()) onSave(selectedCategoryId, amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ui.accent)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ui.muted) }
        }
    )
}
