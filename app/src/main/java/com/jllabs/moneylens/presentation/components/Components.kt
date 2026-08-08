package com.jllabs.moneylens.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.BuildConfig
import com.jllabs.moneylens.R
import com.jllabs.moneylens.domain.models.*
import com.jllabs.moneylens.presentation.navigation.AppTab
import com.jllabs.moneylens.utils.Money

@Composable
fun appTextFieldColors(isDark: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = if (isDark) Color(0xFFE8EDE8) else Color(0xFF2D332A),
    unfocusedTextColor = if (isDark) Color(0xFFE8EDE8) else Color(0xFF2D332A),
    disabledTextColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF2D332A),
    focusedContainerColor = if (isDark) Color(0xFF2A322A) else Color.White,
    unfocusedContainerColor = if (isDark) Color(0xFF2A322A) else Color.White,
    disabledContainerColor = if (isDark) Color(0xFF2A322A) else Color.White,
    focusedBorderColor = Color(0xFF3B7A57),
    unfocusedBorderColor = if (isDark) Color(0xFF3A453A) else Color(0xFFD0D5CE),
    disabledBorderColor = if (isDark) Color(0xFF3A453A) else Color(0xFFD0D5CE),
    focusedLabelColor = Color(0xFF3B7A57),
    unfocusedLabelColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF555A52),
    disabledLabelColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF555A52),
    focusedPlaceholderColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079),
    unfocusedPlaceholderColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079),
    disabledPlaceholderColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079),
    cursorColor = Color(0xFF3B7A57),
    focusedLeadingIconColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079),
    unfocusedLeadingIconColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079),
    focusedTrailingIconColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079),
    unfocusedTrailingIconColor = if (isDark) Color(0xFFA8B2A8) else Color(0xFF7C8079)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarHeader(
    activeTab: AppTab,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.moneylens_icon_master),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    val appName = stringResource(R.string.app_name)
                    val versionLabel = "v${BuildConfig.VERSION_NAME}"
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = scheme.onSurface
                                )
                            ) {
                                append(appName)
                            }
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    color = scheme.onSurfaceVariant,
                                    baselineShift = BaselineShift.Subscript
                                )
                            ) {
                                append(versionLabel)
                            }
                        },
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        fontSize = 10.sp,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        actions = {
            Surface(
                color = scheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "LOCAL PRIVACY",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                    tint = scheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.background,
            titleContentColor = scheme.onSurface,
            actionIconContentColor = scheme.onSurface
        )
    )
}

@Composable
fun MoneyLensBottomNavigation(
    activeTab: AppTab,
    smsCount: Int,
    onTabSelect: (AppTab) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        selectedTextColor = Color(0xFF3B7A57),
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
        indicatorColor = Color(0xFF3B7A57)
    )

    NavigationBar(containerColor = scheme.surface, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = activeTab == AppTab.OVERVIEW,
            onClick = { onTabSelect(AppTab.OVERVIEW) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
            label = { Text("Overview", fontSize = 10.sp, fontWeight = if (activeTab == AppTab.OVERVIEW) FontWeight.Bold else FontWeight.SemiBold) },
            colors = navColors
        )
        NavigationBarItem(
            selected = activeTab == AppTab.REMINDERS,
            onClick = { onTabSelect(AppTab.REMINDERS) },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
            label = { Text("Reminders", fontSize = 10.sp, fontWeight = if (activeTab == AppTab.REMINDERS) FontWeight.Bold else FontWeight.SemiBold) },
            colors = navColors
        )
        NavigationBarItem(
            selected = activeTab == AppTab.ACCOUNTS,
            onClick = { onTabSelect(AppTab.ACCOUNTS) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts") },
            label = { Text("Accounts", fontSize = 10.sp, fontWeight = if (activeTab == AppTab.ACCOUNTS) FontWeight.Bold else FontWeight.SemiBold) },
            colors = navColors
        )
        NavigationBarItem(
            selected = activeTab == AppTab.SETTINGS,
            onClick = { onTabSelect(AppTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp, fontWeight = if (activeTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.SemiBold) },
            colors = navColors
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
    isDarkMode: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val sign = if (isExpense) "-" else "+"
    val color = when {
        isExpense && isDarkMode -> Color(0xFFEF9A9A)
        isExpense -> Color(0xFFC62828)
        isDarkMode -> Color(0xFF81C784)
        else -> Color(0xFF2E7D32)
    }
    val avatarBg = when {
        isExpense && isDarkMode -> Color(0xFF3A2828)
        isExpense -> Color(0xFFFFEBEE)
        isDarkMode -> Color(0xFF26332A)
        else -> Color(0xFFE8F5E9)
    }
    val subtitle = if (isDarkMode) Color(0xFFA8B2A8) else Color(0xFF7C8079)

    val displayTitle = if (transaction.merchant.isNotBlank() && !transaction.merchant.equals("CRED", ignoreCase = true)) {
        transaction.merchant
    } else if (transaction.bankName.isNotBlank() && !transaction.bankName.equals("CRED", ignoreCase = true)) {
        transaction.bankName
    } else "Bank Transaction"

    val accountOrBankDisplay = if (transaction.bankName.isNotBlank() && !transaction.bankName.equals("CRED", ignoreCase = true)) {
        transaction.bankName
    } else accountName

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = categoryIcon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "$categoryName • $accountOrBankDisplay",
                    fontSize = 11.sp,
                    color = subtitle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPrivacyMasked) "₹ • • •" else "$sign${Money.format(transaction.amountMinor, absolute = true)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color,
                    softWrap = false,
                    maxLines = 1
                )
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (isDarkMode) Color(0xFFEF9A9A).copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.6f)
                        )
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
    isDarkMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, Double, String, String, String, String) -> Unit
) {
    val ui = com.jllabs.moneylens.theme.rememberAppUiColors(isDarkMode)
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ui.card,
        titleContentColor = ui.ink,
        textContentColor = ui.ink,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .imePadding(),
        title = { Text("Add Transaction (₹ INR)", color = ui.ink) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B7A57),
                            selectedLabelColor = Color.White,
                            containerColor = ui.chip,
                            labelColor = ui.ink
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B7A57),
                            selectedLabelColor = Color.White,
                            containerColor = ui.chip,
                            labelColor = ui.ink
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹ INR)") },
                    singleLine = true,
                    colors = appTextFieldColors(isDarkMode),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Source (e.g. Starbucks, Uber)") },
                    singleLine = true,
                    colors = appTextFieldColors(isDarkMode),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Remarks") },
                    singleLine = true,
                    colors = appTextFieldColors(isDarkMode),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57), contentColor = Color.White),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Save Transaction", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF81C784)),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Cancel", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            }
        }
    )
}
