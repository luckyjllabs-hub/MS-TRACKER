package com.example.mstrackerapp.presentation.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import com.example.mstrackerapp.data.parser.SmsInboxScanner
import com.example.mstrackerapp.domain.models.Category
import com.example.mstrackerapp.domain.models.SmsQueueItem
import com.example.mstrackerapp.parser.stage1.MessageTypeClassifier
import com.example.mstrackerapp.parser.stage4.DebitCreditDetector
import com.example.mstrackerapp.utils.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SmsInboxScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    val context = LocalContext.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedSmsToEdit by remember { mutableStateOf<SmsQueueItem?>(null) }
    var selectedSmsForDetail by remember { mutableStateOf<SmsQueueItem?>(null) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var selectedQueueFilter by remember { mutableStateOf("All") }

    val queueDateFilters = listOf("All", "Today", "This Week", "This Month", "This Quarter")

    val filteredQueue = remember(uiState.smsQueue, selectedQueueFilter) {
        val now = Calendar.getInstance()
        uiState.smsQueue.filter { sms ->
            if (sms.amountMinor == 0L) return@filter false
            when (selectedQueueFilter) {
                "Today" -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = sms.timestamp }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
                "This Week" -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = sms.timestamp }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                }
                "This Month" -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = sms.timestamp }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                "This Quarter" -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = sms.timestamp }
                    val currentQ = now.get(Calendar.MONTH) / 3
                    val smsQ = cal.get(Calendar.MONTH) / 3
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && smsQ == currentQ
                }
                else -> true
            }
        }
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
        if (hasSmsPermission) {
            SmsInboxScanner.scanExistingInbox(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Smart Bank SMS Inbox Queue",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Text(
            text = "Full multi-stage classification: Type, Credit/Debit, SubType & Confidence",
            fontSize = 12.sp,
            color = Color(0xFF7C8079)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Date filter chips
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(queueDateFilters) { filter ->
                val isSelected = selectedQueueFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedQueueFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B7A57),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasSmsPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF3B7A57),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SMS Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF2D332A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant SMS permission so MS Tracker can automatically detect bank spend alerts locally on your device.",
                        fontSize = 12.sp,
                        color = Color(0xFF7C8079)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant SMS Permission")
                    }
                }
            }
        } else if (filteredQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (uiState.smsQueue.isEmpty())
                        "✅ All bank SMS parsed & recorded!"
                    else
                        "No messages found for '$selectedQueueFilter'",
                    color = Color(0xFF7C8079)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredQueue, key = { it.id }) { sms ->
                    val detectedCategory = uiState.categories.find { it.id == sms.suggestedCategoryId }
                        ?: Category("cat-14", "Other", "📦", "#BDC3C7")

                    SmsQueueCardItem(
                        sms = sms,
                        category = detectedCategory,
                        onCardClick = { selectedSmsForDetail = sms },
                        onAccept = { viewModel.acceptSms(sms.id) },
                        onEdit = { selectedSmsToEdit = sms },
                        onIgnore = { viewModel.ignoreSms(sms.id) },
                        onDelete = { viewModel.deleteSmsItem(sms.id) }
                    )
                }
            }
        }
    }

    // Full SMS Detail Pop-Up Dialog
    if (selectedSmsForDetail != null) {
        val sms = selectedSmsForDetail!!
        val detectedCategory = uiState.categories.find { it.id == sms.suggestedCategoryId }
            ?: Category("cat-14", "Other", "📦", "#BDC3C7")
        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sms.timestamp))
        val debitCredit = DebitCreditDetector.detect(sms.rawText)
        val isIncome = debitCredit.transactionType == com.example.mstrackerapp.domain.models.TransactionType.INCOME

        val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD87D56)
        val typeLabel = if (isIncome) "CREDIT / INCOME (+)" else "DEBIT / EXPENSE (-)"
        val typeBadgeColor = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFBE9E7)
        val typeTextColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

        AlertDialog(
            onDismissRequest = { selectedSmsForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actual Bank SMS Received",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF2D332A)
                    )
                    Surface(
                        color = Color(0xFFE4E8E3),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = sms.bank,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B7A57)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Type Badge & Amount Banner
                    Surface(
                        color = Color(0xFFF4F3EF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = typeBadgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$typeLabel • [${debitCredit.subType.name}]",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = typeTextColor
                                    )
                                }
                                Text(
                                    text = "${if (isIncome) "+" else "-"}${Money.format(sms.amountMinor)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = amountColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sms.merchant, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2D332A))
                                Text("${detectedCategory.icon} ${detectedCategory.name}", fontSize = 11.sp, color = Color(0xFF7C8079))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Full Received Bank SMS Text:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555A52))
                    Spacer(modifier = Modifier.height(6.dp))

                    // Full Raw SMS Body Box
                    Surface(
                        color = Color(0xFF1E241C),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = sms.rawText,
                                color = Color(0xFFE4E8E3),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timestamp & Confidence Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Received: $formattedDate", fontSize = 10.sp, color = Color(0xFF7C8079))
                        Text("Confidence: ${sms.confidence}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B7A57))
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            selectedSmsToEdit = sms
                            selectedSmsForDetail = null
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Category")
                    }
                    Button(
                        onClick = {
                            viewModel.acceptSms(sms.id)
                            selectedSmsForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept & Record")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SMS Body", sms.rawText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied SMS text to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Text")
                    }
                    TextButton(onClick = { selectedSmsForDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Close")
                    }
                }
            }
        )
    }

    // Category Edit Dialog
    if (selectedSmsToEdit != null) {
        val sms = selectedSmsToEdit!!
        AlertDialog(
            onDismissRequest = { selectedSmsToEdit = null },
            title = { Text("Edit Category for ${sms.merchant}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Select correct category to update mapping:", fontSize = 12.sp, color = Color(0xFF7C8079))
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.categories.forEach { category ->
                        TextButton(
                            onClick = {
                                viewModel.changeCategoryAndAccept(sms.id, category.id)
                                selectedSmsToEdit = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${category.icon} ${category.name}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSmsToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Rationale") },
            text = { Text("MS Tracker uses SMS access only locally on your phone to convert debit/credit bank alerts into expense entries.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57))
                ) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SmsQueueCardItem(
    sms: SmsQueueItem,
    category: Category,
    onCardClick: () -> Unit,
    onAccept: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    onDelete: () -> Unit
) {
    val messageType = MessageTypeClassifier.classify(sms.bank, sms.rawText)
    val debitCredit = DebitCreditDetector.detect(sms.rawText)
    val isIncome = debitCredit.transactionType == com.example.mstrackerapp.domain.models.TransactionType.INCOME

    val cardBorderColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD87D56)
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amountPrefix = if (isIncome) "+" else "-"
    val typeBadgeLabel = if (isIncome) "⬆ CREDIT" else "⬇ DEBIT"
    val typeBadgeBg = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val typeBadgeText = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorderColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Bank, Type Badges, SubType, Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFE4E8E3), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = sms.bank,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B7A57)
                        )
                    }
                    Surface(color = typeBadgeBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = typeBadgeLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeBadgeText
                        )
                    }
                    Surface(color = Color(0xFFEBF3FA), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = debitCredit.subType.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E5B88)
                        )
                    }
                }
                Surface(color = Color(0xFFFFF3CD), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = sms.confidence,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF856404)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Merchant & Amount with sign
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sms.merchant,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2D332A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(color = Color(0xFFF4F3EF), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${category.icon} ${category.name}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF555A52)
                        )
                    }
                }
                Text(
                    text = "$amountPrefix${Money.format(sms.amountMinor)}",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Raw Text Snippet
            Text(
                text = sms.rawText,
                fontSize = 11.sp, color = Color(0xFF7C8079), maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Accept, Edit, Ignore, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFF3B7A57)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Edit", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = "Ignore", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Ignore", fontSize = 11.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD87D56))
                }
            }
        }
    }
}
