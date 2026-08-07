package com.example.mstrackerapp.presentation.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.domain.accounts.SmsAccountAggregator
import com.example.mstrackerapp.domain.accounts.SmsAccountRow
import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.utils.Money

private enum class AccountTxFilter { ALL, CREDIT, DEBIT }

@Composable
fun AccountLedgerScreen(
    account: SmsAccountRow,
    transactions: List<Transaction>,
    isPrivacyMasked: Boolean,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf(AccountTxFilter.ALL) }

    val accountTxs = remember(account, transactions) {
        SmsAccountAggregator.transactionsFor(account, transactions)
            .filter { it.type != TransactionType.JUST_INFO }
    }
    val filtered = remember(accountTxs, filter) {
        when (filter) {
            AccountTxFilter.ALL -> accountTxs
            AccountTxFilter.CREDIT -> accountTxs.filter { it.type == TransactionType.INCOME }
            AccountTxFilter.DEBIT -> accountTxs.filter { it.type == TransactionType.EXPENSE }
        }
    }
    val monthGroups = remember(filtered) {
        filtered.groupBy { it.date.take(7) } // yyyy-MM
            .toList()
            .sortedByDescending { it.first }
    }

    val showBal = !isPrivacyMasked
    val dateLabel = SmsAccountAggregator.formatBalanceDate(account.balanceDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2D332A)
                )
            }
            Text(
                SmsAccountAggregator.detailTitle(account),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D332A)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Available balance", fontSize = 13.sp, color = Color(0xFF2D332A))
        if (dateLabel.isNotBlank()) {
            Text("Updated on $dateLabel", fontSize = 12.sp, color = Color(0xFF888C84))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            when {
                account.balanceMinor == null -> "—"
                showBal -> formatRupee(account.balanceMinor)
                else -> "••••••••"
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Powered by SMS updates. Actual details may vary.",
            fontSize = 11.sp,
            color = Color(0xFF888C84)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            AccountFilterTab("All", filter == AccountTxFilter.ALL, Modifier.weight(1f)) {
                filter = AccountTxFilter.ALL
            }
            AccountFilterTab("Credit", filter == AccountTxFilter.CREDIT, Modifier.weight(1f)) {
                filter = AccountTxFilter.CREDIT
            }
            AccountFilterTab("Debit", filter == AccountTxFilter.DEBIT, Modifier.weight(1f)) {
                filter = AccountTxFilter.DEBIT
            }
        }
        HorizontalDivider(color = Color(0xFFE0E4DC), thickness = 1.dp)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No transactions for this account",
                        modifier = Modifier.padding(vertical = 32.dp),
                        color = Color(0xFF888C84),
                        fontSize = 14.sp
                    )
                }
            } else {
                monthGroups.forEach { (_, txs) ->
                    val headerDate = txs.firstOrNull()?.date ?: return@forEach
                    item(key = "hdr-${headerDate.take(7)}") {
                        Text(
                            SmsAccountAggregator.formatMonthYear(headerDate),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2D332A),
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )
                    }
                    items(txs, key = { it.id }) { tx ->
                        AccountTxRow(tx = tx, showBal = showBal)
                        HorizontalDivider(color = Color(0xFFE8EBE6), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountFilterTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF2E7DFF) else Color(0xFF555A52)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = if (selected) Color(0xFF2E7DFF) else Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {}
    }
}

@Composable
private fun AccountTxRow(tx: Transaction, showBal: Boolean) {
    val isCredit = tx.type == TransactionType.INCOME
    val amountColor = when {
        isCredit -> Color(0xFF2E7D32)
        else -> Color(0xFFE07050)
    }
    val label = when {
        tx.merchant.isNotBlank() && !tx.merchant.equals("Unknown", true) -> tx.merchant
        isCredit -> "CREDIT"
        else -> "DEBIT"
    }
    val day = SmsAccountAggregator.formatBalanceDate(tx.date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            day.ifBlank { tx.date },
            modifier = Modifier.width(64.dp),
            fontSize = 13.sp,
            color = Color(0xFF555A52)
        )
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2D332A),
            maxLines = 1
        )
        Text(
            if (showBal) formatRupee(tx.amountMinor) else "••••",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

internal fun formatRupee(minor: Long): String {
    val raw = Money.format(minor)
    return raw.replace("\u20B9", "\u20B9 ").replace(Regex("""\s+"""), " ").trim()
}
