package com.jllabs.moneylens.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.domain.accounts.SmsAccountAggregator
import com.jllabs.moneylens.domain.accounts.SmsAccountRow
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.domain.reminders.ReminderDismissStore
import com.jllabs.moneylens.domain.reminders.ReminderExtractor
import com.jllabs.moneylens.domain.reminders.SmsReminder
import com.jllabs.moneylens.parser.stage5.AmountParser
import com.jllabs.moneylens.theme.rememberAppUiColors
import androidx.compose.runtime.mutableIntStateOf

private enum class AccountTxFilter { ALL, CREDIT, DEBIT, DUE }

private data class MonthLedgerGroup(
    val monthKey: String,
    val txs: List<Transaction>,
    val netMinor: Long
)

@Composable
fun AccountLedgerScreen(
    account: SmsAccountRow,
    transactions: List<Transaction>,
    isPrivacyMasked: Boolean,
    isDarkMode: Boolean = false,
    onBack: () -> Unit
) {
    val ui = rememberAppUiColors(isDarkMode)
    var filter by remember { mutableStateOf(AccountTxFilter.ALL) }
    var smsToShow by remember { mutableStateOf<String?>(null) }
    var reminderSms by remember { mutableStateOf<SmsReminder?>(null) }
    val context = LocalContext.current
    var dismissTick by remember { mutableIntStateOf(0) }
    val dismissed = remember(dismissTick) { ReminderDismissStore.dismissedIds(context) }

    val accountTxs = remember(account.key, transactions) {
        val allForAccount = SmsAccountAggregator.transactionsFor(account, transactions)
        allForAccount.filter { tx ->
            val maybeDue = tx.type == TransactionType.JUST_INFO ||
                tx.source.equals("SMS_REMINDER", true) ||
                tx.rawSms.contains("due", ignoreCase = true)
            val isExpiredDue = maybeDue && ReminderExtractor.isExpiredDueSms(tx.rawSms, tx.date)
            when (tx.type) {
                TransactionType.JUST_INFO -> {
                    if (isExpiredDue) return@filter false
                    account.shortBank.equals("EPFO", ignoreCase = true) ||
                        tx.source.equals("SMS_REMINDER", true) ||
                        tx.source.equals("SMS_LOAN", true) ||
                        tx.source.equals("SMS_BALANCE", true) ||
                        tx.source.equals("SMS_EPFO", true) ||
                        tx.merchant.contains("due", true) ||
                        tx.merchant.contains("loan", true) ||
                        tx.rawSms.contains("contribution", true) ||
                        tx.rawSms.contains("is due", true)
                }
                else -> !isExpiredDue
            }
        }
    }
    val filtered = remember(accountTxs, filter) {
        when (filter) {
            AccountTxFilter.ALL -> accountTxs.filter {
                !(it.source.equals("SMS_REMINDER", true) ||
                    it.merchant.contains("EMI due", true) ||
                    it.merchant.contains("Payment due", true) ||
                    it.merchant.contains("Premium due", true) ||
                    (it.type == TransactionType.JUST_INFO &&
                        it.rawSms.contains("is due", true) &&
                        !it.rawSms.contains("contribution", true)))
            }
            AccountTxFilter.CREDIT -> accountTxs.filter {
                it.type == TransactionType.INCOME ||
                    (account.shortBank.equals("EPFO", true) &&
                        AmountParser.parseEpfoContributionMinor(it.rawSms) != null)
            }
            AccountTxFilter.DEBIT -> accountTxs.filter { it.type == TransactionType.EXPENSE }
            AccountTxFilter.DUE -> emptyList()
        }
    }
    val monthGroups = remember(filtered) {
        filtered.groupBy { it.date.take(7) }
            .toList()
            .sortedByDescending { it.first }
            .map { (month, txs) ->
                val credit = txs.sumOf { tx ->
                    val amt = if (account.shortBank.equals("EPFO", true)) {
                        SmsAccountAggregator.displayAmountMinor(tx)
                    } else {
                        tx.amountMinor
                    }
                    when {
                        tx.type == TransactionType.INCOME -> amt
                        else -> 0L
                    }
                }
                val debit = txs.sumOf { tx ->
                    if (tx.type == TransactionType.EXPENSE) tx.amountMinor else 0L
                }
                MonthLedgerGroup(monthKey = month, txs = txs, netMinor = credit - debit)
            }
    }
    val reminders = remember(accountTxs, dismissTick) {
        ReminderExtractor.forAccount(
            account.last4,
            account.shortBank,
            accountTxs,
            dismissedIds = dismissed
        )
    }

    val showBal = !isPrivacyMasked
    val dateLabel = SmsAccountAggregator.formatBalanceDate(account.balanceDate)
    val balLabel = when {
        account.isCreditCard -> "Available limit"
        account.isLoanAccount -> "Loan balance"
        else -> "Available balance"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ui.ink)
            }
            Column {
                Text(
                    SmsAccountAggregator.detailTitle(account),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ui.ink
                )
                Text(
                    when {
                        account.isCreditCard -> "Credit card"
                        account.isLoanAccount -> "Loan account"
                        account.isFasTagAccount -> "FASTag"
                        else -> "Bank account"
                    },
                    fontSize = 12.sp,
                    color = when {
                        account.isCreditCard -> Color(0xFF90CAF9)
                        account.isLoanAccount -> Color(0xFFCE93D8)
                        account.isFasTagAccount -> Color(0xFFFFB74D)
                        else -> Color(0xFF81C784)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(balLabel, fontSize = 13.sp, color = ui.ink)
        if (dateLabel.isNotBlank()) {
            Text("Updated on $dateLabel", fontSize = 12.sp, color = ui.muted)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            when {
                showBal -> formatRupee(account.balanceMinor ?: 0L)
                else -> "••••••••"
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                (account.balanceMinor ?: 0L) < 0 -> Color(0xFFEF9A9A)
                else -> ui.ink
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Powered by SMS updates. Actual details may vary.",
            fontSize = 11.sp,
            color = ui.muted
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            AccountFilterTab("All", filter == AccountTxFilter.ALL, ui.ink, ui.muted, Modifier.weight(1f)) {
                filter = AccountTxFilter.ALL
            }
            AccountFilterTab("Credit", filter == AccountTxFilter.CREDIT, ui.ink, ui.muted, Modifier.weight(1f)) {
                filter = AccountTxFilter.CREDIT
            }
            AccountFilterTab("Debit", filter == AccountTxFilter.DEBIT, ui.ink, ui.muted, Modifier.weight(1f)) {
                filter = AccountTxFilter.DEBIT
            }
            AccountFilterTab("Due", filter == AccountTxFilter.DUE, ui.ink, ui.muted, Modifier.weight(1f)) {
                filter = AccountTxFilter.DUE
            }
        }
        HorizontalDivider(color = ui.divider, thickness = 1.dp)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filter == AccountTxFilter.DUE) {
                if (reminders.isEmpty()) {
                    item {
                        Text(
                            "No upcoming dues for this account",
                            modifier = Modifier.padding(vertical = 32.dp),
                            color = ui.muted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    items(reminders, key = { it.id }) { rem ->
                        ReminderCard(
                            rem = rem,
                            cardBg = ui.card,
                            ink = ui.ink,
                            muted = ui.muted,
                            onOpen = { reminderSms = rem },
                            onMarkDone = {
                                ReminderDismissStore.markDone(context, rem.id)
                                dismissTick++
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "No transactions for this account",
                        modifier = Modifier.padding(vertical = 32.dp),
                        color = ui.muted,
                        fontSize = 14.sp
                    )
                }
            } else {
                monthGroups.forEach { group ->
                    val headerDate = group.txs.firstOrNull()?.date ?: return@forEach
                    item(key = "hdr-${group.monthKey}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                SmsAccountAggregator.formatMonthYear(headerDate),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ui.ink,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (showBal) "Net ${formatRupee(group.netMinor)}" else "Net ••••",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF90CAF9)
                            )
                        }
                    }
                    items(group.txs, key = { it.id }) { tx ->
                        AccountTxRow(
                            tx = tx,
                            showBal = showBal,
                            ink = ui.ink,
                            muted = ui.muted,
                            onClick = {
                                smsToShow = tx.rawSms.ifBlank { tx.note }.ifBlank { "No original SMS stored." }
                            }
                        )
                        HorizontalDivider(color = ui.divider, thickness = 1.dp)
                    }
                }
            }
        }
    }

    smsToShow?.let { body ->
        RawSmsDialog(
            title = "Original SMS",
            body = body,
            isDarkMode = isDarkMode,
            onDismiss = { smsToShow = null }
        )
    }
    reminderSms?.let { rem ->
        ReminderDetailDialog(
            rem = rem,
            cardBg = ui.card,
            ink = ui.ink,
            muted = ui.muted,
            chip = ui.chip,
            onDismiss = { reminderSms = null }
        )
    }
}

@Composable
private fun AccountFilterTab(
    label: String,
    selected: Boolean,
    ink: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = Color(0xFF81C784)
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else muted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = if (selected) accent else Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {}
    }
}

@Composable
private fun AccountTxRow(
    tx: Transaction,
    showBal: Boolean,
    ink: Color,
    muted: Color,
    onClick: () -> Unit
) {
    val isEpfo = tx.merchant.equals("EPFO contribution", true) ||
        tx.rawSms.contains("contribution", ignoreCase = true)
    val displayAmount = when {
        isEpfo -> SmsAccountAggregator.displayAmountMinor(tx)
        else -> tx.amountMinor
    }
    val isCredit = tx.type == TransactionType.INCOME || isEpfo
    val amountColor = when {
        displayAmount == 0L && tx.type == TransactionType.JUST_INFO -> muted
        isCredit -> Color(0xFF81C784)
        else -> Color(0xFFEF9A9A)
    }
    val label = when {
        isEpfo -> "EPFO contribution"
        tx.merchant.contains("EMI due", true) ||
            (tx.type == TransactionType.JUST_INFO && tx.rawSms.contains("emi", true) &&
                tx.rawSms.contains("due", true)) -> "EMI due"
        tx.merchant.contains("Payment due", true) || tx.merchant.contains("Premium due", true) ->
            tx.merchant
        tx.merchant.equals("Loan account", true) -> "Loan account"
        tx.type == TransactionType.JUST_INFO -> "BALANCE UPDATE"
        tx.merchant.isNotBlank() && !tx.merchant.equals("Unknown", true) -> tx.merchant
        isCredit -> "CREDIT"
        else -> "DEBIT"
    }
    val day = remember(tx.date) { SmsAccountAggregator.formatBalanceDate(tx.date) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            day.ifBlank { tx.date },
            modifier = Modifier.width(64.dp),
            fontSize = 13.sp,
            color = muted
        )
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ink,
            maxLines = 1
        )
        Text(
            when {
                !showBal -> "••••"
                displayAmount == 0L && tx.type == TransactionType.JUST_INFO -> "—"
                else -> formatRupee(displayAmount)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

internal fun formatRupee(minor: Long): String {
    val raw = com.jllabs.moneylens.utils.Money.format(minor)
    return raw.replace("\u20B9", "\u20B9 ").replace(Regex("""\s+"""), " ").trim()
}
