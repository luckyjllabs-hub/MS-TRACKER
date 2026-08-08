package com.jllabs.moneylens.presentation.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jllabs.moneylens.data.parser.SmsInboxScanner
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.SmsQueueItem
import com.jllabs.moneylens.parser.stage4.DebitCreditDetector
import com.jllabs.moneylens.presentation.components.PeriodDropdown
import com.jllabs.moneylens.theme.rememberAppUiColors
import com.jllabs.moneylens.utils.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SmsInboxScreen(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    val context = LocalContext.current
    val ui = rememberAppUiColors(uiState.isDarkMode)

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedSmsToEdit by remember { mutableStateOf<SmsQueueItem?>(null) }
    var selectedSmsForDetail by remember { mutableStateOf<SmsQueueItem?>(null) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    // Inbox keeps its own period (default All) — do not reuse Overview's "This Month"
    var selectedQueueFilter by remember { mutableStateOf("All") }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val queueDateFilters = listOf(
        "All", "Today", "Yesterday", "This Week", "This Month", "This Quarter", "3 Months", "6 Months", "1 Year"
    )

    val filteredQueue = remember(uiState.smsQueue, selectedQueueFilter) {
        filterInboxQueue(uiState.smsQueue, selectedQueueFilter)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false
        if (receiveGranted || readGranted) {
            hasSmsPermission = true
            SmsInboxScanner.scanExistingInbox(context)
        } else {
            hasSmsPermission = false
            showRationaleDialog = true
        }
    }

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) SmsInboxScanner.scanExistingInbox(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Inbox queue", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ui.ink)
        Text(
            "Review low-confidence bank SMS before recording",
            fontSize = 12.sp,
            color = ui.muted
        )
        Spacer(modifier = Modifier.height(12.dp))

        PeriodDropdown(
            label = "Period",
            options = queueDateFilters,
            selected = selectedQueueFilter,
            onSelected = { selectedQueueFilter = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!hasSmsPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ui.card),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ui.accent, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SMS Permission Required", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ui.ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Grant SMS permission so MoneyLens can detect bank alerts on your device.",
                        fontSize = 12.sp,
                        color = ui.muted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Grant SMS Permission") }
                }
            }
        } else if (filteredQueue.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (uiState.smsQueue.isEmpty()) {
                        "All bank SMS parsed & recorded"
                    } else {
                        "No messages in $selectedQueueFilter (${uiState.smsQueue.size} total in queue)"
                    },
                    color = ui.muted
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredQueue, key = { it.id }) { sms ->
                    SmsQueueCardItem(
                        sms = sms,
                        isDark = uiState.isDarkMode,
                        onCardClick = { selectedSmsForDetail = sms },
                        onAccept = { viewModel.acceptSms(sms.id) },
                        onIgnore = { viewModel.ignoreSms(sms.id) },
                        onDelete = { viewModel.deleteSmsItem(sms.id) }
                    )
                }
            }
        }
    }

    if (selectedSmsForDetail != null) {
        val sms = selectedSmsForDetail!!
        InboxSmsDetailDialog(
            sms = sms,
            category = uiState.categories.find { it.id == sms.suggestedCategoryId }
                ?: Category("cat-14", "Other", "📦", "#BDC3C7"),
            isDark = uiState.isDarkMode,
            onDismiss = { selectedSmsForDetail = null },
            onAccept = {
                viewModel.acceptSms(sms.id)
                selectedSmsForDetail = null
            },
            onEditCategory = {
                selectedSmsToEdit = sms
                selectedSmsForDetail = null
            },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("SMS Body", sms.rawText))
                Toast.makeText(context, "SMS copied", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (selectedSmsToEdit != null) {
        val sms = selectedSmsToEdit!!
        AlertDialog(
            onDismissRequest = { selectedSmsToEdit = null },
            containerColor = ui.card,
            title = { Text("Edit category", color = ui.ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(sms.merchant, fontSize = 12.sp, color = ui.muted)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.categories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.changeCategoryAndAccept(sms.id, category.id)
                                    selectedSmsToEdit = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "${category.icon} ${category.name}",
                                    color = ui.ink,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = Color(0xFFEF9A9A), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddCategory = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add new category", color = ui.accent)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSmsToEdit = null }) {
                    Text("Cancel", color = ui.muted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            containerColor = ui.card,
            title = { Text("New category", fontWeight = FontWeight.Bold, color = ui.ink) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    singleLine = true,
                    placeholder = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ui.ink,
                        unfocusedTextColor = ui.ink
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newCategoryName.trim()
                        if (name.isNotEmpty()) {
                            val id = viewModel.addCategory(name)
                            selectedSmsToEdit?.let { sms ->
                                viewModel.changeCategoryAndAccept(sms.id, id)
                                selectedSmsToEdit = null
                            }
                            newCategoryName = ""
                            showAddCategory = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ui.accent)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false }) { Text("Cancel", color = ui.muted) }
            }
        )
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            containerColor = ui.card,
            title = { Text("Permission needed", color = ui.ink, fontWeight = FontWeight.Bold) },
            text = { Text("MoneyLens reads SMS only on your phone to convert bank alerts into transactions.", color = ui.ink) },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ui.accent)
                ) { Text("Try again") }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) { Text("Cancel", color = ui.muted) }
            }
        )
    }
}

/**
 * Filter inbox queue by SMS receive time ([SmsQueueItem.timestamp], epoch millis).
 * Uses inclusive day/period bounds so items match what users expect from the dropdown.
 */
internal fun filterInboxQueue(
    queue: List<SmsQueueItem>,
    filter: String,
    nowMillis: Long = System.currentTimeMillis()
): List<SmsQueueItem> {
    if (filter.equals("All", ignoreCase = true) || filter.isBlank()) return queue

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
    fun endOfDay(millis: Long): Long = startOfDay(millis) + 24L * 60 * 60 * 1000 - 1

    val todayStart = startOfDay(nowMillis)
    val todayEnd = endOfDay(nowMillis)
    val yesterdayStart = todayStart - 24L * 60 * 60 * 1000
    val yesterdayEnd = todayStart - 1
    val weekStart = todayStart - 6L * 24 * 60 * 60 * 1000 // last 7 calendar days incl. today
    val monthStart = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val quarterStart = startOfQuarterMillis(nowMillis)
    val threeMonthStart = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.MONTH, -3)
    }.timeInMillis.let { startOfDay(it) }
    val sixMonthStart = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.MONTH, -6)
    }.timeInMillis.let { startOfDay(it) }
    val oneYearStart = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.YEAR, -1)
    }.timeInMillis.let { startOfDay(it) }

    val (rangeStart, rangeEnd) = when (filter.trim().uppercase(Locale.US)) {
        "TODAY" -> todayStart to todayEnd
        "YESTERDAY" -> yesterdayStart to yesterdayEnd
        "THIS WEEK", "WEEK", "CURRENT WEEK", "LAST 7 DAYS" -> weekStart to todayEnd
        "THIS MONTH", "MONTH", "CURRENT MONTH" -> monthStart to todayEnd
        "THIS QUARTER", "QUARTER" -> quarterStart to todayEnd
        "3 MONTHS" -> threeMonthStart to todayEnd
        "6 MONTHS" -> sixMonthStart to todayEnd
        "1 YEAR", "YEAR" -> oneYearStart to todayEnd
        else -> return queue
    }

    return queue.filter { sms ->
        val ts = normalizeSmsTimestamp(sms.timestamp)
        ts in rangeStart..rangeEnd
    }
}

/** Android SMS dates are millis; tolerate accidental seconds or blank/zero. */
internal fun normalizeSmsTimestamp(raw: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    if (raw <= 0L) return nowMillis
    // Seconds since epoch (~2001–2286) look like 1e9…1e10; millis are ~1e12+
    if (raw < 1_000_000_000_000L) return raw * 1000L
    return raw
}

private fun startOfQuarterMillis(now: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val month = cal.get(Calendar.MONTH)
    cal.set(Calendar.MONTH, (month / 3) * 3)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
private fun InboxSmsDetailDialog(
    sms: SmsQueueItem,
    category: Category,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onEditCategory: () -> Unit,
    onCopy: () -> Unit
) {
    val ui = rememberAppUiColors(isDark)
    val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(normalizeSmsTimestamp(sms.timestamp)))
    val debitCredit = DebitCreditDetector.detect(sms.rawText)
    val isIncome = debitCredit.transactionType == com.jllabs.moneylens.domain.models.TransactionType.INCOME
    val amountColor = if (isIncome) Color(0xFF81C784) else Color(0xFFEF9A9A)
    val typeLabel = if (isIncome) "Credit" else "Debit"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ui.card,
        title = {
            Text(sms.bank.ifBlank { "Bank SMS" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ui.ink)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${if (isIncome) "+" else "-"}${Money.format(sms.amountMinor)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = amountColor,
                    maxLines = 1
                )
                Text("$typeLabel · ${category.icon} ${category.name}", fontSize = 13.sp, color = ui.muted)
                Text(sms.merchant.ifBlank { "Transaction" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ui.ink)
                HorizontalDivider(color = ui.divider)
                Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ui.muted)
                Surface(
                    color = ui.chip,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ui.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        sms.rawText,
                        modifier = Modifier.padding(14.dp),
                        color = ui.ink,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                Text("$formattedDate · Confidence ${sms.confidence}", fontSize = 12.sp, color = ui.muted)
            }
        },
        confirmButton = {
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = ui.accent)) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accept")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEditCategory) {
                    Text("Edit category", color = ui.accent, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onCopy) {
                    Text("Copy", color = ui.accent, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = ui.muted)
                }
            }
        }
    )
}

@Composable
fun SmsQueueCardItem(
    sms: SmsQueueItem,
    isDark: Boolean,
    onCardClick: () -> Unit,
    onAccept: () -> Unit,
    onIgnore: () -> Unit,
    onDelete: () -> Unit
) {
    val ui = rememberAppUiColors(isDark)
    val formattedDate = remember(sms.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(normalizeSmsTimestamp(sms.timestamp)))
    }
    val debitCredit = DebitCreditDetector.detect(sms.rawText)
    val isIncome = debitCredit.transactionType == com.jllabs.moneylens.domain.models.TransactionType.INCOME
    val typeLabel = if (isIncome) "Credit" else "Debit"
    val typeColor = if (isIncome) Color(0xFF81C784) else Color(0xFFEF9A9A)

    Card(
        colors = CardDefaults.cardColors(containerColor = ui.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ui.divider),
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    sms.bank.ifBlank { "Bank" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ui.ink,
                    modifier = Modifier.weight(1f)
                )
                Text(sms.confidence, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ui.muted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(typeLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = typeColor)
            Text(formattedDate, fontSize = 11.sp, color = ui.muted)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = ui.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Accept", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    border = BorderStroke(1.dp, ui.divider)
                ) {
                    Text("Ignore", fontSize = 12.sp, color = ui.muted)
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    border = BorderStroke(1.dp, ui.divider)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF9A9A), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp, color = Color(0xFFEF9A9A))
                }
            }
        }
    }
}
