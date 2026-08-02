package com.example.mstrackerapp.presentation.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.data.parser.SmsInboxScanner
import com.example.mstrackerapp.utils.CsvExporter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    uiState: MSTrackerUiState,
    viewModel: MSTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDarkMode by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf("Emerald Green") }
    var enableNotifications by remember { mutableStateOf(true) }
    var isExportingRawSms by remember { mutableStateOf(false) }

    var showRegexDialog by remember { mutableStateOf(false) }
    var showDictDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewSummary by remember { mutableStateOf("Loading…") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "App Settings",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Text(
            text = "Customization, Backup, Privacy & Rule Engines",
            fontSize = 12.sp,
            color = Color(0xFF7C8079)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Appearance & Theme
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Appearance & Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))

                        // Dark Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Dark Mode", fontSize = 14.sp, color = Color(0xFF2D332A))
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { isDarkMode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B7A57))
                            )
                        }

                        // Theme Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF45B7D1), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Theme Palette", fontSize = 14.sp, color = Color(0xFF2D332A))
                            }
                            Text(selectedTheme, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B7A57))
                        }
                    }
                }
            }

            // 2. Backup & Restore
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Backup & Restore Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Backup
                            Button(
                                onClick = {
                                    val file = CsvExporter.exportTransactionsToCsv(context, uiState.transactions, uiState.categories, uiState.accounts)
                                    CsvExporter.shareCsvFile(context, file)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV", fontSize = 12.sp)
                            }

                            // Restore
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Database backup JSON generated locally in app storage.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore JSON", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Export Raw SMS Data
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E241C)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF7DD3A8),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    "Export Raw SMS Data",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Read entire SMS inbox & share as CSV file",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9BA8A3)
                                )
                            }
                        }

                        Text(
                            text = "Exports all SMS messages (Sender, Date, Time, Body) from your device inbox into a CSV file you can open in Excel / Google Sheets.",
                            fontSize = 11.sp,
                            color = Color(0xFF9BA8A3),
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExportingRawSms = true
                                    try {
                                        val file = CsvExporter.exportRawSmsToCsv(context)
                                        CsvExporter.shareCsvFile(
                                            context, file,
                                            subject = "MS Tracker - Raw SMS Export"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isExportingRawSms = false
                                    }
                                }
                            },
                            enabled = !isExportingRawSms,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isExportingRawSms) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reading SMS Inbox...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Read & Export SMS as CSV", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 4. Rule Engines (Regex & Merchant Dictionary)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rule Engines", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))

                        // Regex Rules
                        TextButton(
                            onClick = { showRegexDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Bank SMS Regex Rules Manager", fontSize = 13.sp, color = Color(0xFF2D332A))
                            }
                        }

                        // Merchant Dictionary
                        TextButton(
                            onClick = { showDictDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Merchant Dictionary Manager", fontSize = 13.sp, color = Color(0xFF2D332A))
                            }
                        }

                        TextButton(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Classification Review (Others / Unknown)", fontSize = 13.sp, color = Color(0xFF2D332A))
                            }
                        }
                    }
                }
            }

            // 4. Privacy & Rescan SMS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Privacy & Scanning", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))

                        // Mask Balances Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Mask Account Balances", fontSize = 14.sp, color = Color(0xFF2D332A))
                            }
                            Switch(
                                checked = uiState.isPrivacyMasked,
                                onCheckedChange = { viewModel.togglePrivacyMask() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B7A57))
                            )
                        }

                        // Rescan SMS Button
                        Button(
                            onClick = {
                                SmsInboxScanner.scanExistingInbox(context)
                                Toast.makeText(context, "Full SMS Inbox Rescan Initiated", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rescan SMS Inbox Now")
                        }
                    }
                }
            }

            // 5. Notification Preferences
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Notification Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Bank Transaction Alerts", fontSize = 14.sp, color = Color(0xFF2D332A))
                            }
                            Switch(
                                checked = enableNotifications,
                                onCheckedChange = { enableNotifications = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B7A57))
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRegexDialog) {
        AlertDialog(
            onDismissRequest = { showRegexDialog = false },
            title = { Text("Bank Regex Rules", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Configured Bank SMS Regex Patterns:", fontSize = 12.sp, color = Color(0xFF7C8079))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• HDFC: (?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• ICICI: Txn:\\s*(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• SBI: debited\\s*by\\s*(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegexDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDictDialog) {
        AlertDialog(
            onDismissRequest = { showDictDialog = false },
            title = { Text("Merchant Dictionary", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Standard Merchant Category Mappings:", fontSize = 12.sp, color = Color(0xFF7C8079))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Swiggy / Zomato -> Food 🍔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• Uber / Ola -> Transport 🚗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• Amazon / Flipkart -> Shopping 🛍️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• Netflix / PVR -> Entertainment 🎬", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• Apollo / 1mg -> Health 🏥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDictDialog = false }) { Text("Close") }
            }
        )
    }

    if (showReviewDialog) {
        LaunchedEffect(showReviewDialog) {
            reviewSummary = withContext(kotlinx.coroutines.Dispatchers.IO) {
                val db = com.example.mstrackerapp.data.database.MSTrackerDatabase.getDatabase(context)
                val others = db.transactionDao().getOtherTransactions()
                val total = uiState.transactions.size.coerceAtLeast(1)
                val pct = others.size * 100f / total
                val unknown = others.map { it.merchant }.distinct()
                    .filter { !com.example.mstrackerapp.parser.classifier.MerchantNormalizer.isKnownMerchant(it) }
                val top = db.transactionDao().getTopMerchants(20)
                val counts = uiState.transactions.groupingBy { it.categoryId }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .joinToString("\n") { (id, c) ->
                        "${com.example.mstrackerapp.parser.stage6.SmsCategory.CATEGORY_NAMES[id] ?: id}: $c"
                    }
                buildString {
                    appendLine("Others: ${others.size} (${"%.1f".format(pct)}%)")
                    appendLine("Unknown merchants: ${unknown.size}")
                    appendLine()
                    appendLine("Per category:")
                    appendLine(counts)
                    appendLine()
                    appendLine("Unknown sample:")
                    unknown.take(15).forEach { appendLine("• $it") }
                    appendLine()
                    appendLine("Top merchants:")
                    top.forEach { appendLine("• ${it.merchant} (${it.cnt})") }
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Classification Review", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    item {
                        Text(reviewSummary, fontSize = 12.sp, color = Color(0xFF2D332A))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Close") }
            }
        )
    }
}
