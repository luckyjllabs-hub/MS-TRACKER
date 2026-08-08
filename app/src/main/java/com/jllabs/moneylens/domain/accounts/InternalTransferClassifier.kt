package com.jllabs.moneylens.domain.accounts

import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType

/**
 * Detects own-account transfers so they are not counted as income + expense
 * (which inflates Overview Credit / Debit totals).
 */
object InternalTransferClassifier {

    private val TRANSFER_HINT = Regex(
        """(?i)\b(?:
            imps|neft|rtgs|fund\s+transfer|self\s+transfer|
            trf\s+to|transferred\s+to|transfer\s+to|transfer\s+from|
            to\s+a/?c|from\s+a/?c|money\s+transfer
        )\b""",
        RegexOption.COMMENTS
    )

    /** Ids that must not contribute to income/expense totals (paired own-account moves). */
    fun excludedFromIncomeExpenseIds(transactions: List<Transaction>): Set<String> {
        val excluded = mutableSetOf<String>()

        val moneyMoves = transactions.filter {
            it.type == TransactionType.INCOME ||
                it.type == TransactionType.EXPENSE ||
                it.type == TransactionType.TRANSFER
        }
        val byKey = moneyMoves.groupBy { "${it.date}|${it.amountMinor}" }
        for ((_, group) in byKey) {
            if (group.size < 2) continue
            val credits = group.filter {
                it.type == TransactionType.INCOME ||
                    (it.type == TransactionType.TRANSFER && looksLikeCreditLeg(it))
            }
            val debits = group.filter {
                it.type == TransactionType.EXPENSE ||
                    (it.type == TransactionType.TRANSFER && !looksLikeCreditLeg(it))
            }
            for (inc in credits) {
                for (exp in debits) {
                    if (inc.id == exp.id) continue
                    if (!looksLikeTransferLeg(inc) && !looksLikeTransferLeg(exp)) continue
                    val inLast4 = SmsAccountAggregator.resolveLast4(inc)
                    val outLast4 = SmsAccountAggregator.resolveLast4(exp)
                    val differentAccounts = inLast4.isNotBlank() && outLast4.isNotBlank() &&
                        inLast4 != outLast4 &&
                        !inLast4.endsWith(outLast4) && !outLast4.endsWith(inLast4)
                    val bothTransferish = looksLikeTransferLeg(inc) && looksLikeTransferLeg(exp)
                    if (differentAccounts || bothTransferish) {
                        excluded += inc.id
                        excluded += exp.id
                    }
                }
            }
        }
        return excluded
    }

    fun looksLikeCreditLeg(tx: Transaction): Boolean {
        if (tx.smsTransactionSubType.equals("TRANSFER_IN", ignoreCase = true)) return true
        val body = tx.rawSms.lowercase()
        return body.contains("credited") || body.contains("received") || body.contains("deposit")
    }

    fun looksLikeTransferLeg(tx: Transaction): Boolean {
        if (tx.type == TransactionType.TRANSFER) return true
        val blob = "${tx.rawSms} ${tx.note} ${tx.merchant}"
        if (TRANSFER_HINT.containsMatchIn(blob)) return true
        if (blob.contains("[TRANSFER_IN]", ignoreCase = true) ||
            blob.contains("[TRANSFER_OUT]", ignoreCase = true)
        ) return true
        return tx.smsTransactionSubType.equals("TRANSFER_IN", ignoreCase = true) ||
            tx.smsTransactionSubType.equals("TRANSFER_OUT", ignoreCase = true)
    }

    fun incomeExpenseTotals(transactions: List<Transaction>): Pair<Long, Long> {
        val skip = excludedFromIncomeExpenseIds(transactions)
        var income = 0L
        var expense = 0L
        for (tx in transactions) {
            if (tx.id in skip) continue
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountMinor
                TransactionType.EXPENSE -> expense += tx.amountMinor
                TransactionType.TRANSFER -> {
                    // Legacy rows may be TRANSFER from over-classification; still count by direction
                    // unless paired (already skipped).
                    if (looksLikeCreditLeg(tx)) income += tx.amountMinor
                    else expense += tx.amountMinor
                }
                else -> Unit
            }
        }
        return income to expense
    }
}
