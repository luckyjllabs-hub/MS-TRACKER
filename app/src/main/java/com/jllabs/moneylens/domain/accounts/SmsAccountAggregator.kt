package com.jllabs.moneylens.domain.accounts

import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.stage5.AccountParser
import com.jllabs.moneylens.parser.stage5.AmountParser
import com.jllabs.moneylens.parser.stage5.BalanceParser
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * One SMS-discovered bank account, loan, or credit card (bank + last digits).
 */
data class SmsAccountRow(
    val key: String,
    val shortBank: String,
    val last4: String,
    val displayName: String,
    val balanceMinor: Long?,
    val balanceDate: String?,
    val lastActivityDate: String?,
    val txCount: Int,
    val isCreditCard: Boolean = false,
    val isLoanAccount: Boolean = false,
    val isFasTagAccount: Boolean = false
)

object SmsAccountAggregator {

    fun derive(transactions: List<Transaction>): List<SmsAccountRow> {
        val creditCardFingerprints = buildCreditCardFingerprints(transactions)
        val loanFingerprints = buildLoanFingerprints(transactions)
        val fasTagFingerprints = buildFasTagFingerprints(transactions)
        val canonicalByBank = buildCanonicalLast4ByBank(transactions)

        val grouped = linkedMapOf<String, MutableList<Pair<Transaction, ResolvedAccount>>>()
        for (tx in transactions) {
            val resolved = resolve(
                tx, creditCardFingerprints, loanFingerprints, fasTagFingerprints, canonicalByBank
            ) ?: continue
            grouped.getOrPut(resolved.key) { mutableListOf() }.add(tx to resolved)
        }

        val epfoKeys = grouped.keys.filter { it.startsWith("EPFO|") }
        if (epfoKeys.isNotEmpty()) {
            val primaryEpfo = epfoKeys.first()
            for (tx in transactions) {
                if (!isEpfoRelated(tx)) continue
                if (resolve(
                        tx, creditCardFingerprints, loanFingerprints, fasTagFingerprints, canonicalByBank
                    ) != null
                ) continue
                val sample = grouped[primaryEpfo]?.firstOrNull()?.second ?: continue
                grouped.getOrPut(primaryEpfo) { mutableListOf() }.add(tx to sample)
            }
        }

        return grouped.values.map { pairs ->
            val sample = pairs.first().second
            val latestActivity = pairs.maxByOrNull { it.first.date + it.first.time }?.first?.date
            val forcedFt = sample.isFasTagAccount || pairs.any { isFasTagTransaction(it.first) }
            val forcedLoan = !forcedFt && (
                sample.isLoanAccount || pairs.any { isLoanTransaction(it.first) }
                )
            val forcedCc = !forcedFt && !forcedLoan && (
                sample.isCreditCard || pairs.any { isCreditCardTransaction(it.first) }
                )
            val smsBalance = pairs.mapNotNull { (tx, _) ->
                val bal = tx.availableBalance
                    ?: BalanceParser.extractDisplayBalanceMinor(tx.rawSms)
                bal?.let { Triple(tx.date, tx.createdAt, it) }
            }.maxWithOrNull(compareBy({ it.first }, { it.second }))

            val hasMoneyMove = pairs.any {
                it.first.type == TransactionType.INCOME ||
                    it.first.type == TransactionType.EXPENSE ||
                    it.first.type == TransactionType.TRANSFER
            }
            val computed = computeFlowBalanceMinor(pairs.map { it.first })
            val balanceMinor = smsBalance?.third
                ?: if (hasMoneyMove) computed else null
            val balanceDate = smsBalance?.first ?: latestActivity

            val prefix = when {
                forcedCc -> "${sample.shortBank} CC"
                forcedLoan -> "${sample.shortBank} Loan"
                forcedFt -> "${sample.shortBank} FASTag"
                else -> sample.shortBank
            }
            val kind = when {
                forcedCc -> "CC"
                forcedLoan -> "LN"
                forcedFt -> "FT"
                else -> "AC"
            }
            val display = when {
                forcedFt && sample.last4 == "FTAG" -> prefix
                forcedFt -> "$prefix XX${sample.last4}"
                else -> "$prefix XX${sample.last4}"
            }
            SmsAccountRow(
                key = "${sample.shortBank.uppercase()}|$kind|${sample.last4}",
                shortBank = sample.shortBank,
                last4 = sample.last4,
                displayName = display,
                balanceMinor = balanceMinor,
                balanceDate = balanceDate,
                lastActivityDate = latestActivity,
                txCount = pairs.size,
                isCreditCard = forcedCc,
                isLoanAccount = forcedLoan,
                isFasTagAccount = forcedFt
            )
        }.sortedWith(
            compareBy<SmsAccountRow> {
                when {
                    it.isCreditCard -> 3
                    it.isLoanAccount -> 2
                    it.isFasTagAccount -> 1
                    else -> 0
                }
            }.thenBy { it.displayName.lowercase(Locale.US) }
        )
    }

    private fun buildCanonicalLast4ByBank(transactions: List<Transaction>): Map<String, Map<String, String>> {
        val byBank = mutableMapOf<String, MutableSet<String>>()
        for (tx in transactions) {
            val last4 = resolveLast4(tx)
            if (last4.isBlank()) continue
            val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
                ?: detectBankFromSms(tx.rawSms)
            val short = shortBankLabel(bankSource)
            if (short.isBlank() || short.equals("unknown", ignoreCase = true)) continue
            byBank.getOrPut(short.uppercase(Locale.US)) { mutableSetOf() }.add(last4)
        }
        return byBank.mapValues { (_, set) ->
            set.associateWith { last4 ->
                val related = set.filter { it == last4 || it.endsWith(last4) || last4.endsWith(it) }
                AccountParser.preferCanonicalLast4(related)
            }
        }
    }

    private fun buildCreditCardFingerprints(transactions: List<Transaction>): Set<String> {
        val out = mutableSetOf<String>()
        for (tx in transactions) {
            if (!isCreditCardTransaction(tx)) continue
            val last4 = resolveLast4(tx)
            if (last4.isBlank()) continue
            val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
                ?: detectBankFromSms(tx.rawSms)
            val short = shortBankLabel(bankSource)
            if (short.isBlank() || short.equals("unknown", ignoreCase = true)) continue
            out.add(fingerprint(short, last4))
        }
        return out
    }

    private fun buildLoanFingerprints(transactions: List<Transaction>): Set<String> {
        val out = mutableSetOf<String>()
        for (tx in transactions) {
            if (!isLoanTransaction(tx)) continue
            val last4 = resolveLast4(tx)
            if (last4.isBlank()) continue
            val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
                ?: detectBankFromSms(tx.rawSms)
            val short = shortBankLabel(bankSource)
            if (short.isBlank() || short.equals("unknown", ignoreCase = true)) continue
            out.add(fingerprint(short, last4))
        }
        return out
    }

    private fun buildFasTagFingerprints(transactions: List<Transaction>): Set<String> {
        val out = mutableSetOf<String>()
        for (tx in transactions) {
            if (!isFasTagTransaction(tx)) continue
            val last4 = resolveLast4(tx)
            if (last4.isBlank()) continue
            val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
                ?: detectBankFromSms(tx.rawSms)
            val short = shortBankLabel(bankSource)
            if (short.isBlank() || short.equals("unknown", ignoreCase = true)) continue
            out.add(fingerprint(short, last4))
        }
        return out
    }

    private fun isCreditCardTransaction(tx: Transaction): Boolean =
        AccountParser.isCreditCardSms(tx.rawSms) ||
            tx.note.contains("credit card", ignoreCase = true) ||
            tx.accountId.contains("-cc-", ignoreCase = true)

    private fun isLoanTransaction(tx: Transaction): Boolean =
        AccountParser.isLoanAccountSms(tx.rawSms) ||
            tx.source.equals("SMS_LOAN", ignoreCase = true) ||
            tx.accountId.contains("-ln-", ignoreCase = true) ||
            (tx.merchant.contains("EMI due", ignoreCase = true) &&
                AccountParser.isLoanAccountSms(tx.rawSms)) ||
            (tx.source.equals("SMS_REMINDER", ignoreCase = true) &&
                AccountParser.isLoanAccountSms(tx.rawSms))

    private fun isFasTagTransaction(tx: Transaction): Boolean =
        AccountParser.isFasTagSms(tx.rawSms) ||
            tx.accountId.contains("-ft-", ignoreCase = true) ||
            tx.merchant.contains("fastag", ignoreCase = true)

    private fun fingerprint(shortBank: String, last4: String): String =
        "${shortBank.uppercase(Locale.US)}|$last4"

    /** Sum credits − debits when SMS has no available-balance line (may be negative). */
    fun computeFlowBalanceMinor(txs: List<Transaction>): Long {
        var bal = 0L
        for (tx in txs.sortedWith(compareBy({ it.date }, { it.time }, { it.createdAt }))) {
            when (tx.type) {
                TransactionType.INCOME -> bal += tx.amountMinor
                TransactionType.EXPENSE -> bal -= tx.amountMinor
                TransactionType.TRANSFER -> bal -= tx.amountMinor
                TransactionType.JUST_INFO -> {
                    val snap = tx.availableBalance ?: BalanceParser.extractDisplayBalanceMinor(tx.rawSms)
                    if (snap != null) bal = snap
                }
            }
        }
        return bal
    }

    fun accountIdFor(
        bank: String,
        last4: String,
        isCreditCard: Boolean = false,
        isLoan: Boolean = false,
        isFasTag: Boolean = false
    ): String {
        if (last4.isBlank()) return "acc-1"
        val short = shortBankLabel(bank)
        val kind = when {
            isCreditCard -> "cc"
            isLoan -> "ln"
            isFasTag -> "ft"
            else -> "ac"
        }
        return "sms-$kind-${short.lowercase().replace(Regex("""[^a-z0-9]+"""), "")}-$last4"
    }

    fun resolveLast4(tx: Transaction): String {
        // Prefer re-parse from raw SMS so FASTag Bal Rs.#### is never used as last4
        if (tx.rawSms.isNotBlank()) {
            val fromSms = AccountParser.extractAccountOrCardLast4(tx.rawSms)
            if (fromSms.isNotBlank()) return fromSms
            // Insurance/policy SMS without a bank mask must not keep a stale stored last4
            if (AccountParser.isInsuranceOrPolicySms(tx.rawSms) &&
                !Regex("""(?i)(?:bank\s+)?(?:a/?c|acct|account)\s*(?:no\.?)?\s*(?:ending(?:\s*(?:in|with))?)?\s*[xX*]+\d{3,}""")
                    .containsMatchIn(tx.rawSms)
            ) {
                return ""
            }
        }
        if (tx.accountLast4.isNotBlank()) return AccountParser.normalizeLastDigits(tx.accountLast4)
        return ""
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

    fun detailTitle(account: SmsAccountRow): String {
        val kind = when {
            account.isCreditCard -> "CC"
            account.isLoanAccount -> "Loan"
            account.isFasTagAccount -> "FASTag"
            else -> ""
        }
        return listOf(account.shortBank, kind, account.last4.takeUnless { it == "FTAG" })
            .filter { !it.isNullOrBlank() }
            .joinToString(" - ")
            .replace(" -  - ", " - ")
    }

    fun transactionsFor(account: SmsAccountRow, all: List<Transaction>): List<Transaction> {
        // Fast path: match this account only — do not rebuild fingerprints over the full ledger.
        return all.filter { matchesAccountFast(it, account) }
            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.time })
    }

    /** Lightweight match used by ledger UI (avoids O(n) fingerprint rebuilds). */
    private fun matchesAccountFast(tx: Transaction, account: SmsAccountRow): Boolean {
        if (account.shortBank.equals("EPFO", ignoreCase = true) && isEpfoRelated(tx)) {
            val last4 = resolveLast4(tx)
            return last4.isBlank() || last4 == account.last4 ||
                account.last4.endsWith(last4) || last4.endsWith(account.last4)
        }
        if (AccountParser.isInsuranceOrPolicySms(tx.rawSms) &&
            !Regex("""(?i)(?:bank\s+)?(?:a/?c|acct|account)\s*(?:no\.?)?\s*(?:ending(?:\s*(?:in|with))?)?\s*[xX*]+\d{3,}""")
                .containsMatchIn(tx.rawSms)
        ) {
            return false
        }
        val last4 = resolveLast4(tx)
        if (last4.isBlank()) return false
        val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
            ?: detectBankFromSms(tx.rawSms)
        val short = shortBankLabel(bankSource)
        if (!short.equals(account.shortBank, ignoreCase = true)) return false
        val sameMask = last4 == account.last4 ||
            last4.endsWith(account.last4) ||
            account.last4.endsWith(last4)
        if (!sameMask) return false
        return when {
            account.isFasTagAccount -> isFasTagTransaction(tx)
            account.isLoanAccount -> isLoanTransaction(tx)
            account.isCreditCard -> isCreditCardTransaction(tx)
            else -> !isFasTagTransaction(tx) && !isLoanTransaction(tx) && !isCreditCardTransaction(tx)
        }
    }

    fun matchesAccount(tx: Transaction, account: SmsAccountRow): Boolean {
        return matchesAccountFast(tx, account)
    }

    private fun matchesAccount(
        tx: Transaction,
        account: SmsAccountRow,
        creditCardFingerprints: Set<String>,
        loanFingerprints: Set<String>,
        fasTagFingerprints: Set<String>,
        canonicalByBank: Map<String, Map<String, String>>
    ): Boolean {
        val resolved = resolve(
            tx, creditCardFingerprints, loanFingerprints, fasTagFingerprints, canonicalByBank
        )
        if (resolved != null && keysMatch(resolved, account)) return true
        if (account.shortBank.equals("EPFO", ignoreCase = true) && isEpfoRelated(tx)) {
            val last4 = resolveLast4(tx)
            return last4.isBlank() || last4 == account.last4 ||
                account.last4.endsWith(last4) || last4.endsWith(account.last4)
        }
        return false
    }

    private fun keysMatch(resolved: ResolvedAccount, account: SmsAccountRow): Boolean {
        if (resolved.key == account.key) return true
        if (resolved.shortBank.equals(account.shortBank, ignoreCase = true)) {
            val sameMask = resolved.last4 == account.last4 ||
                resolved.last4.endsWith(account.last4) ||
                account.last4.endsWith(resolved.last4)
            if (sameMask) {
                if (account.isFasTagAccount && resolved.isFasTagAccount) return true
                if (account.isCreditCard && resolved.isCreditCard) return true
                if (account.isLoanAccount && resolved.isLoanAccount) return true
                if (!resolved.isCreditCard && !account.isCreditCard &&
                    !resolved.isLoanAccount && !account.isLoanAccount &&
                    !resolved.isFasTagAccount && !account.isFasTagAccount
                ) return true
            }
        }
        val legacy = "${resolved.shortBank.uppercase()}|${resolved.last4}"
        val accountLegacy = "${account.shortBank.uppercase()}|${account.last4}"
        return legacy == accountLegacy &&
            resolved.isCreditCard == account.isCreditCard &&
            resolved.isLoanAccount == account.isLoanAccount &&
            resolved.isFasTagAccount == account.isFasTagAccount
    }

    private data class ResolvedAccount(
        val key: String,
        val shortBank: String,
        val last4: String,
        val isCreditCard: Boolean,
        val isLoanAccount: Boolean,
        val isFasTagAccount: Boolean
    )

    private fun resolve(
        tx: Transaction,
        creditCardFingerprints: Set<String> = emptySet(),
        loanFingerprints: Set<String> = emptySet(),
        fasTagFingerprints: Set<String> = emptySet(),
        canonicalByBank: Map<String, Map<String, String>> = emptyMap()
    ): ResolvedAccount? {
        // Insurance/policy due SMS without a bank a/c must not become a bank account row
        if (AccountParser.isInsuranceOrPolicySms(tx.rawSms) &&
            !Regex("""(?i)(?:bank\s+)?(?:a/?c|acct|account)\s*(?:no\.?)?\s*(?:ending(?:\s*(?:in|with))?)?\s*[xX*]+\d{3,}""")
                .containsMatchIn(tx.rawSms)
        ) {
            return null
        }
        var last4 = resolveLast4(tx)
        if (last4.isBlank()) return null
        val bankSource = tx.bankName.takeUnless { isUnknownBank(it) }
            ?: detectBankFromSms(tx.rawSms)
        val short = shortBankLabel(bankSource)
        if (short.isBlank() || short.equals("unknown", ignoreCase = true)) return null
        last4 = canonicalByBank[short.uppercase(Locale.US)]?.get(last4) ?: last4
        val fp = fingerprint(short, last4)
        val isFt = isFasTagTransaction(tx) || fp in fasTagFingerprints
        val isLoan = !isFt && (
            isLoanTransaction(tx) || fp in loanFingerprints ||
                loanFingerprints.any {
                    it.startsWith("${short.uppercase(Locale.US)}|") &&
                        (it.endsWith(last4) || last4.endsWith(it.substringAfter('|')))
                }
            )
        val isCc = !isFt && !isLoan && (
            isCreditCardTransaction(tx) ||
                fp in creditCardFingerprints ||
                creditCardFingerprints.any {
                    it.startsWith("${short.uppercase(Locale.US)}|") &&
                        (it.endsWith(last4) || last4.endsWith(it.substringAfter('|')))
                }
            )
        val kind = when {
            isCc -> "CC"
            isLoan -> "LN"
            isFt -> "FT"
            else -> "AC"
        }
        return ResolvedAccount(
            key = "${short.uppercase()}|$kind|$last4",
            shortBank = short,
            last4 = last4,
            isCreditCard = isCc,
            isLoanAccount = isLoan,
            isFasTagAccount = isFt
        )
    }

    /**
     * Ledger amount for display. EPFO rows historically stored passbook balance as amount —
     * prefer contribution from raw SMS when present.
     */
    fun displayAmountMinor(tx: Transaction): Long {
        if (isEpfoRelated(tx)) {
            AmountParser.parseEpfoContributionMinor(tx.rawSms)?.let { return it }
            val bal = tx.availableBalance ?: BalanceParser.extractDisplayBalanceMinor(tx.rawSms)
            if (bal != null && tx.amountMinor == bal) return 0L
        }
        return tx.amountMinor
    }

    fun isEpfoRelated(tx: Transaction): Boolean {
        val blob = "${tx.bankName} ${tx.rawSms} ${tx.note}".lowercase()
        return blob.contains("epfo") || blob.contains("epassbook") || blob.contains("provident") ||
            blob.contains("passbook") || blob.contains("bgbng") ||
            shortBankLabel(tx.bankName).equals("EPFO", ignoreCase = true)
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
            lower.contains("baroda") || Regex("""\bbob\b""").containsMatchIn(lower) -> "BOB"
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
