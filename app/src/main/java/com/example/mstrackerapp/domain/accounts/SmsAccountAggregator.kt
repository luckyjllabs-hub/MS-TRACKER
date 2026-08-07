package com.example.mstrackerapp.domain.accounts

import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.parser.stage5.AccountParser
import com.example.mstrackerapp.parser.stage5.BalanceParser
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * One SMS-discovered bank account (bank + last4), with latest available balance from SMS text.
 */
data class SmsAccountRow(
    val key: String,
    val shortBank: String,
    val last4: String,
    val displayName: String,
    val balanceMinor: Long?,
    val balanceDate: String?,
    val lastActivityDate: String?,
    val txCount: Int
)

object SmsAccountAggregator {

    fun derive(transactions: List<Transaction>): List<SmsAccountRow> {
        val grouped = linkedMapOf<String, MutableList<Pair<Transaction, ResolvedAccount>>>()
        for (tx in transactions) {
            val resolved = resolve(tx) ?: continue
            grouped.getOrPut(resolved.key) { mutableListOf() }.add(tx to resolved)
        }

        return grouped.values.map { pairs ->
            val sample = pairs.first().second
            val latestActivity = pairs.maxByOrNull { it.first.date + it.first.time }?.first?.date
            val withBalance = pairs.mapNotNull { (tx, _) ->
                val bal = tx.availableBalance ?: BalanceParser.extractBalanceMinor(tx.rawSms)
                bal?.let { Triple(tx.date, tx.createdAt, it) }
            }
            val best = withBalance.maxWithOrNull(compareBy({ it.first }, { it.second }))
            SmsAccountRow(
                key = sample.key,
                shortBank = sample.shortBank,
                last4 = sample.last4,
                displayName = sample.displayName,
                balanceMinor = best?.third,
                balanceDate = best?.first,
                lastActivityDate = latestActivity,
                txCount = pairs.size
            )
        }.sortedWith(
            compareByDescending<SmsAccountRow> { it.balanceDate ?: it.lastActivityDate ?: "" }
                .thenBy { it.displayName }
        )
    }

    fun accountIdFor(bank: String, last4: String): String {
        if (last4.isBlank()) return "acc-1"
        val short = shortBankLabel(bank)
        return "sms-${short.lowercase().replace(Regex("""[^a-z0-9]+"""), "")}-$last4"
    }

    fun displayNameFor(bank: String, last4: String): String {
        val short = shortBankLabel(bank)
        return if (last4.isBlank()) short else "$short XX$last4"
    }

    fun resolveLast4(tx: Transaction): String {
        if (tx.accountLast4.isNotBlank()) return tx.accountLast4
        if (tx.rawSms.isBlank()) return ""
        return AccountParser.extractAccountOrCardLast4(tx.rawSms)
    }

    fun formatBalanceDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFmt = SimpleDateFormat("dd MMM", Locale.US)
            val d = inFmt.parse(isoDate) ?: return isoDate
            outFmt.format(d)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatMonthYear(isoDate: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
            val d = inFmt.parse(isoDate) ?: return isoDate.take(7)
            outFmt.format(d)
        } catch (_: Exception) {
            isoDate.take(7)
        }
    }

    /** Title like "ICICI - 2346" for the bank ledger screen. */
    fun detailTitle(account: SmsAccountRow): String = "${account.shortBank} - ${account.last4}"

    fun transactionsFor(account: SmsAccountRow, all: List<Transaction>): List<Transaction> {
        return all.filter { matchesAccount(it, account) }
            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.time })
    }

    fun matchesAccount(tx: Transaction, account: SmsAccountRow): Boolean {
        val resolved = resolve(tx) ?: return false
        return resolved.key == account.key
    }

    private data class ResolvedAccount(
        val key: String,
        val shortBank: String,
        val last4: String,
        val displayName: String
    )

    private fun resolve(tx: Transaction): ResolvedAccount? {
        val last4 = resolveLast4(tx)
        if (last4.isBlank()) return null
        val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
            ?: detectBankFromSms(tx.rawSms)
        val short = shortBankLabel(bankSource)
        if (short.isBlank() || short.equals("unknown", ignoreCase = true)) return null
        val display = "$short XX$last4"
        return ResolvedAccount(
            key = "${short.uppercase()}|$last4",
            shortBank = short,
            last4 = last4,
            displayName = display
        )
    }

    private fun isUnknownBank(raw: String): Boolean {
        val s = raw.trim()
        if (s.isBlank()) return true
        val lower = s.lowercase()
        return lower == "unknown" || lower == "unknown bank" || lower.startsWith("unknown")
    }

    fun shortBankLabel(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return ""
        val lower = s.lowercase()
        return when {
            lower.contains("icici") -> "ICICI"
            lower.contains("hdfc") -> "HDFC"
            lower.contains("epfo") || lower.contains("provident") || lower.contains("epassbook") ||
                lower.contains("passbook") || lower.contains("bgbng") -> "EPFO"
            lower.contains("sbi") || lower.contains("state bank") -> "SBI"
            lower.contains("canara") || lower.contains("cnrb") -> "CANARA"
            lower.contains("axis") -> "AXIS"
            lower.contains("kotak") -> "KOTAK"
            lower.contains("indus") -> "INDUSIND"
            lower.contains("indian bank") || lower.contains("idib") -> "INDIAN"
            lower.contains("federal") -> "FEDERAL"
            lower.contains("yes bank") -> "YES"
            lower.contains("pnb") || lower.contains("punjab") -> "PNB"
            lower.contains("baroda") || lower.contains("bob") -> "BOB"
            lower.contains("idfc") -> "IDFC"
            lower.contains("rbl") -> "RBL"
            lower.contains("union") -> "UNION"
            lower.contains("paytm") -> "PAYTM"
            isUnknownBank(s) -> ""
            else -> s.replace(Regex("""(?i)\s*bank\s*$"""), "").trim().take(12).uppercase()
        }
    }

    private fun detectBankFromSms(raw: String): String {
        val lower = raw.lowercase()
        return when {
            "hdfc" in lower -> "HDFC"
            "icici" in lower -> "ICICI"
            "canara" in lower || "cnrb" in lower -> "CANARA"
            "axis" in lower -> "AXIS"
            "epfo" in lower || "epassbook" in lower || "provident fund" in lower ||
                "passbook balance" in lower || "bgbng" in lower -> "EPFO"
            "kotak" in lower -> "KOTAK"
            "indian bank" in lower -> "INDIAN"
            "sbi" in lower || "state bank" in lower -> "SBI"
            else -> ""
        }
    }
}
