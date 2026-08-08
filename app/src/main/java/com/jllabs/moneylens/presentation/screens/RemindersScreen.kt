package com.jllabs.moneylens.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.domain.reminders.ReminderDismissStore
import com.jllabs.moneylens.domain.reminders.ReminderExtractor
import com.jllabs.moneylens.domain.reminders.ReminderKind
import com.jllabs.moneylens.domain.reminders.SmsReminder
import com.jllabs.moneylens.theme.rememberAppUiColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    val context = LocalContext.current
    val ui = rememberAppUiColors(uiState.isDarkMode)
    var dismissTick by remember { mutableIntStateOf(0) }
    val dismissed = remember(dismissTick) { ReminderDismissStore.dismissedIds(context) }
    val reminders = remember(uiState.allTransactions, dismissTick) {
        ReminderExtractor.extractFromTransactions(uiState.allTransactions, dismissedIds = dismissed)
    }
    val filteredReminders = remember(reminders, uiState.searchQuery, uiState.selectedFilter, uiState.transactions) {
        val periodDates = uiState.transactions.map { it.date }.toSet()
        var list = reminders
        if (!uiState.selectedFilter.equals("All", ignoreCase = true)) {
            list = list.filter { rem ->
                rem.sourceDate in periodDates ||
                    (!rem.dueDateIso.isNullOrBlank() && rem.dueDateIso in periodDates)
            }
        }
        val q = uiState.searchQuery.trim()
        if (q.isNotBlank()) {
            list = list.filter { rem ->
                rem.title.contains(q, true) ||
                    rem.detail.contains(q, true) ||
                    rem.bankHint.contains(q, true) ||
                    rem.rawSms.contains(q, true) ||
                    (rem.dueLabel?.contains(q, true) == true)
            }
        }
        list
    }
    var selected by remember { mutableStateOf<SmsReminder?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Reminders", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ui.ink)
        Text(
            "Upcoming dues & offers (past due hidden after ${ReminderExtractor.EXPIRY_GRACE_DAYS} days)",
            fontSize = 12.sp,
            color = ui.muted
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (filteredReminders.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ui.card),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("No reminders yet", fontWeight = FontWeight.SemiBold, color = ui.ink)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "When banks send EMI due, credit-card bill or coupon expiry SMS, they show up here.",
                                fontSize = 13.sp,
                                color = ui.muted
                            )
                        }
                    }
                }
            } else {
                items(filteredReminders, key = { it.id }) { rem ->
                    ReminderCard(
                        rem = rem,
                        cardBg = ui.card,
                        ink = ui.ink,
                        muted = ui.muted,
                        onOpen = { selected = rem },
                        onMarkDone = {
                            ReminderDismissStore.markDone(context, rem.id)
                            dismissTick++
                        }
                    )
                }
            }
        }
    }

    selected?.let { rem ->
        ReminderDetailDialog(
            rem = rem,
            cardBg = ui.card,
            ink = ui.ink,
            muted = ui.muted,
            chip = ui.chip,
            onDismiss = { selected = null }
        )
    }
}

@Composable
fun ReminderCard(
    rem: SmsReminder,
    cardBg: Color,
    ink: Color,
    muted: Color,
    onOpen: () -> Unit,
    onMarkDone: () -> Unit
) {
    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val isOverdue = !rem.dueDateIso.isNullOrBlank() && rem.dueDateIso!! < todayIso
    val icon = when (rem.kind) {
        ReminderKind.COUPON -> Icons.Default.LocalOffer
        ReminderKind.PAYMENT_DUE -> Icons.Default.Payment
        ReminderKind.BILL -> Icons.Default.Receipt
        ReminderKind.OTHER -> Icons.Default.Notifications
    }
    val tint = when {
        isOverdue -> Color(0xFFEF5350)
        rem.kind == ReminderKind.COUPON -> Color(0xFFFFB74D)
        rem.kind == ReminderKind.PAYMENT_DUE -> Color(0xFFEF9A9A)
        rem.kind == ReminderKind.BILL -> Color(0xFF90CAF9)
        else -> Color(0xFF81C784)
    }
    val overdueBg = if (isOverdue) Color(0x33EF5350) else cardBg
    Card(
        colors = CardDefaults.cardColors(containerColor = overdueBg),
        shape = RoundedCornerShape(14.dp),
        border = if (isOverdue) BorderStroke(1.5.dp, Color(0xFFEF5350)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 8.dp).size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rem.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ink)
                        if (isOverdue) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFEF5350),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "OVERDUE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (!rem.dueLabel.isNullOrBlank()) {
                        Text(
                            if (isOverdue) "Overdue · ${rem.dueLabel}" else "Due / valid: ${rem.dueLabel}",
                            fontSize = 12.sp,
                            color = tint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (rem.bankHint.isNotBlank()) {
                        Text(rem.bankHint, fontSize = 11.sp, color = Color(0xFF81C784))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rem.detail, fontSize = 12.sp, color = muted, maxLines = 3)
                }
                IconButton(onClick = onMarkDone) {
                    Icon(Icons.Default.Cancel, contentDescription = "Remove", tint = muted, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onMarkDone, modifier = Modifier.weight(1f)) {
                    Text("Mark done", fontSize = 12.sp, color = Color(0xFF81C784))
                }
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text("View SMS", fontSize = 12.sp, color = Color(0xFF90CAF9))
                }
            }
        }
    }
}

@Composable
fun ReminderDetailDialog(
    rem: SmsReminder,
    cardBg: Color,
    ink: Color,
    muted: Color,
    chip: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(rem.rawSms))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            }) {
                Text("Copy", color = Color(0xFF90CAF9), fontWeight = FontWeight.SemiBold)
            }
        },
        title = { Text(rem.title, fontWeight = FontWeight.Bold, color = ink) },
        text = {
            Column {
                if (!rem.dueLabel.isNullOrBlank()) {
                    Text("Due / valid: ${rem.dueLabel}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF9A9A))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Surface(color = chip, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        rem.rawSms.ifBlank { "No original SMS stored for this item." },
                        color = ink,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun RawSmsDialog(
    title: String,
    body: String,
    isDarkMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val ui = rememberAppUiColors(isDarkMode)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(body))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            }) {
                Text("Copy", color = Color(0xFF90CAF9), fontWeight = FontWeight.SemiBold)
            }
        },
        title = { Text(title, fontWeight = FontWeight.Bold, color = ui.ink) },
        text = {
            Surface(color = ui.chip, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    body.ifBlank { "No original SMS stored for this item." },
                    color = ui.ink,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
        },
        containerColor = ui.card
    )
}
