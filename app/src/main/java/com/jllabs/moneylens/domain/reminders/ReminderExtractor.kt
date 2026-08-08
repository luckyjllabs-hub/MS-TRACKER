package com.jllabs.moneylens.domain.reminders

import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.parser.stage5.AccountParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReminderKind { PAYMENT_DUE, COUPON, BILL, OTHER }

data class SmsReminder(
    val id: String,
    val title: String,
    val detail: String,
    val kind: ReminderKind,
    val dueLabel: String?,
    /** ISO yyyy-MM-dd when parseable; used to drop expired items. */
    val dueDateIso: String?,
    val bankHint: String,
    val rawSms: String,
    val sourceDate: String
)

object ReminderExtractor {

    /** Keep dues visible up to this many days after the due date. */
    const val EXPIRY_GRACE_DAYS = 10

    private val EXPLICIT_BANK_AC = Regex(
        """(?i)(?:bank\s+)?(?:a/?c|acct|account)\s*(?:no\.?)?\s*(?:ending(?:\s*(?:in|with))?)?\s*[xX*]+\d{3,}"""
    )
    private val MASKED_LAST4 = Regex("""(?i)[xX*]{2,}(\d{3,6})\b""")

    private val DUE = Regex(
        """(?i)\b(?:
            (?:emi|loan|premium|bill|payment|card)\b.{0,120}\bis\s+due\b|
            \bis\s+due\b.{0,40}\b(?:on|by)\b|
            payment\s+(?:is\s+)?due|
            due\s+(?:date|on|by)\b|
            (?:please\s+)?pay\s+(?:the\s+)?(?:minimum\s+)?(?:amount\s+)?due\b|
            maintain\s+adequate\s+balance.{0,40}due\s+date
        )\b""",
        RegexOption.COMMENTS
    )
    private val COUPON = Regex(
        """(?i)\b(?:coupon|voucher|promo\s*code|offer\s+expires?|valid\s+(?:till|until|upto)|expir(?:e|es|ing)|cashback\s+offer|discount\s+code)\b"""
    )
    private val BILL = Regex(
        """(?i)\b(?:credit\s*card\s+bill|statement\s+is\s+(?:sent|generated)|bill\s+generated|pay\s+by\s+\d)\b"""
    )

    /** Refund / credit confirmations often mention residual "total due" — not reminders. */
    private val REFUND_OR_CREDIT_NOTICE = Regex(
        """(?i)\b(?:
            refund\s+of\b|
            \brefunded\b|
            has\s+been\s+credited\b|
            \bcredited\s+to\b
        )\b""",
        RegexOption.COMMENTS
    )
    private val EXPLICIT_DUE_REMINDER = Regex(
        """(?i)\b(?:
            (?:emi|loan|premium)\b.{0,100}\bis\s+due\b|
            \bis\s+due\s+(?:on|by)\b|
            payment\s+(?:is\s+)?due\s+(?:on|by)\b|
            maintain\s+adequate\s+balance.{0,60}due\s+date
        )\b""",
        RegexOption.COMMENTS
    )

    private val DUE_DATE_PATTERNS = listOf(
        Regex("""(?i)(?:due\s+(?:on|by|date)?\s*|valid\s+(?:till|until|upto)\s*|expires?\s+(?:on\s+)?|pay\s+by\s+)(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})"""),
        Regex("""(?i)(?:due\s+(?:on|by|date)?\s*|valid\s+(?:till|until|upto)\s*|expires?\s+(?:on\s+)?|pay\s+by\s+)(\d{1,2}[-/][A-Za-z]{3,9}[-/]\d{2,4})"""),
        Regex("""(?i)(?:due\s+(?:on|by|date)?\s*|valid\s+(?:till|until|upto)\s*|expires?\s+(?:on\s+)?)(\d{1,2}\s+[A-Za-z]{3,9}(?:\s+\d{2,4})?)"""),
        Regex("""(?i)(?:due\s+(?:on|by|date)?\s*|valid\s+(?:till|until|upto)\s*|expires?\s+(?:on\s+)?)(\d{1,2}-[A-Za-z]{3}-\d{2,4})""")
    )

    fun extractFromTransactions(
        transactions: List<Transaction>,
        todayIso: String = today(),
        dismissedIds: Set<String> = emptySet()
    ): List<SmsReminder> {
        val out = linkedMapOf<String, SmsReminder>()
        for (tx in transactions) {
            val body = tx.rawSms.ifBlank { tx.note }
            if (body.isBlank()) continue
            val reminder = fromSms(body, tx.id, tx.date, tx.bankName) ?: continue
            if (reminder.id in dismissedIds) continue
            if (isExpired(reminder, todayIso)) continue
            val dedupeKey = contentKey(reminder)
            val existing = out[dedupeKey]
            if (existing == null || isNewer(reminder, existing)) {
                out[dedupeKey] = reminder
            }
        }
        // Upcoming (future) dues first — soonest first — then overdue, then undated
        return out.values.sortedWith(
            compareBy<SmsReminder> { rem ->
                val due = rem.dueDateIso
                when {
                    due == null -> 2
                    due < todayIso -> 1
                    else -> 0
                }
            }
                .thenBy { it.dueDateIso ?: "9999" }
                .thenByDescending { it.sourceDate }
        )
    }

    fun fromSms(body: String, @Suppress("UNUSED_PARAMETER") idSeed: String, smsDateIso: String, bank: String): SmsReminder? {
        // "refund of Rs X credited … Revised total due" is a credit notice, not a due reminder
        if (REFUND_OR_CREDIT_NOTICE.containsMatchIn(body) && !EXPLICIT_DUE_REMINDER.containsMatchIn(body)) {
            return null
        }
        val kind = when {
            // Prefer payment-due over coupon when both match (EMI SMS shouldn't become offers)
            DUE.containsMatchIn(body) -> ReminderKind.PAYMENT_DUE
            BILL.containsMatchIn(body) -> ReminderKind.BILL
            COUPON.containsMatchIn(body) -> ReminderKind.COUPON
            else -> return null
        }
        val title = when (kind) {
            ReminderKind.COUPON -> "Coupon / offer"
            ReminderKind.PAYMENT_DUE -> when {
                Regex("""(?i)\bemi\b""").containsMatchIn(body) -> "EMI due"
                Regex("""(?i)\bpremium\b""").containsMatchIn(body) -> "Premium due"
                else -> "Payment due"
            }
            ReminderKind.BILL -> "Bill / statement"
            ReminderKind.OTHER -> "Reminder"
        }
        val rawDue = DUE_DATE_PATTERNS.firstNotNullOfOrNull { it.find(body)?.groupValues?.getOrNull(1)?.trim() }
        val dueIso = parseDueDate(rawDue, smsDateIso)
        val detail = body.replace(Regex("""\s+"""), " ").trim().take(160)
        val stableBody = body.replace(Regex("""\s+"""), " ").trim().lowercase(Locale.US)
        return SmsReminder(
            id = "rem-${kind.name}-${dueIso.orEmpty()}-${stableBody.hashCode()}",
            title = title,
            detail = detail,
            kind = kind,
            dueLabel = rawDue ?: dueIso?.let { formatDisplay(it) },
            dueDateIso = dueIso,
            bankHint = bank,
            rawSms = body,
            sourceDate = smsDateIso
        )
    }

    fun forAccount(
        accountKeyLast4: String,
        bankShort: String,
        transactions: List<Transaction>,
        todayIso: String = today(),
        dismissedIds: Set<String> = emptySet()
    ): List<SmsReminder> {
        return extractFromTransactions(transactions, todayIso, dismissedIds).filter { rem ->
            if (rem.kind == ReminderKind.COUPON) return@filter false
            matchesAccount(rem.rawSms, accountKeyLast4, bankShort)
        }
    }

    /**
     * Attach a reminder to an account only when the SMS names that mask,
     * or (with no mask) the bank as a whole word — not "ICICI" inside "ICICIPru".
     * Insurance/policy dues without a bank a/c stay on the global Reminders tab only.
     */
    fun matchesAccount(body: String, accountKeyLast4: String, bankShort: String): Boolean {
        if (body.isBlank()) return false
        val last4 = accountKeyLast4.trim()
        if (last4.isNotBlank() && body.contains(last4)) return true

        if (AccountParser.isInsuranceOrPolicySms(body) &&
            !EXPLICIT_BANK_AC.containsMatchIn(body)
        ) {
            return false
        }

        val masked = MASKED_LAST4.findAll(body)
            .map { it.groupValues[1].takeLast(4) }
            .filter { it.isNotBlank() }
            .toSet()
        if (masked.isNotEmpty()) {
            return last4.isNotBlank() &&
                masked.any { it == last4 || it.endsWith(last4) || last4.endsWith(it) }
        }

        if (bankShort.isBlank()) return false
        return Regex("""(?i)\b${Regex.escape(bankShort)}\b""").containsMatchIn(body)
    }

    /**
     * Hide after [EXPIRY_GRACE_DAYS] past the due date.
     * Dues/bills without a parseable due date expire sooner than coupons.
     */
    fun isExpired(
        reminder: SmsReminder,
        todayIso: String = today(),
        graceDays: Int = EXPIRY_GRACE_DAYS
    ): Boolean {
        val due = reminder.dueDateIso
        if (due != null) {
            val daysPastDue = daysBetween(due, todayIso)
            return daysPastDue > graceDays
        }
        val maxAgeDays = when (reminder.kind) {
            ReminderKind.PAYMENT_DUE, ReminderKind.BILL -> 35
            else -> 90
        }
        return daysBetween(reminder.sourceDate, todayIso) > maxAgeDays
    }

    /** True when this SMS body is a due/bill reminder that should not appear in ledgers once expired. */
    fun isExpiredDueSms(body: String, smsDateIso: String, todayIso: String = today()): Boolean {
        if (body.isBlank()) return false
        val rem = fromSms(body, "x", smsDateIso, "") ?: return false
        if (rem.kind == ReminderKind.COUPON) return false
        return isExpired(rem, todayIso)
    }

    /**
     * Parse due/expiry date. If year missing, use SMS year (or next year if month/day already passed).
     */
    fun parseDueDate(raw: String?, smsDateIso: String): String? {
        if (raw.isNullOrBlank()) return null
        val smsCal = parseIso(smsDateIso) ?: Calendar.getInstance()
        val cleaned = raw.trim().replace(Regex("""\s+"""), " ")

        val formats = listOf(
            "dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy",
            "dd-MMM-yyyy", "dd-MMM-yy", "dd/MMM/yyyy", "dd/MMM/yy",
            "dd MMM yyyy", "dd MMM yy", "dd MMM", "d MMM yyyy", "d MMM yy", "d MMM",
            "dd-MMM-yy", "d-MMM-yy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(cleaned) ?: continue
                val cal = Calendar.getInstance().apply { time = parsed }
                val hasYear = fmt.contains("y", ignoreCase = true)
                if (!hasYear) {
                    cal.set(Calendar.YEAR, smsCal.get(Calendar.YEAR))
                    // If that date is already before the SMS date, roll to next year
                    if (cal.before(smsCal)) cal.add(Calendar.YEAR, 1)
                } else if (cal.get(Calendar.YEAR) < 100) {
                    // 2-digit year → 2000+
                    val y = cal.get(Calendar.YEAR)
                    cal.set(Calendar.YEAR, if (y < 100) 2000 + y else y)
                }
                return iso(cal.time)
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }

    private fun contentKey(reminder: SmsReminder): String =
        "${reminder.kind}|${reminder.dueDateIso.orEmpty()}|${reminder.rawSms.replace(Regex("""\s+"""), " ").trim().lowercase(Locale.US)}"

    private fun isNewer(a: SmsReminder, b: SmsReminder): Boolean {
        val dueCmp = (a.dueDateIso ?: "").compareTo(b.dueDateIso ?: "")
        if (dueCmp != 0) return dueCmp > 0
        return a.sourceDate >= b.sourceDate
    }

    private fun today(): String = iso(Date())

    private fun iso(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun parseIso(iso: String): Calendar? = try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso) ?: return null
        Calendar.getInstance().apply { time = d }
    } catch (_: Exception) {
        null
    }

    private fun formatDisplay(iso: String): String = try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso) ?: return iso
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
    } catch (_: Exception) {
        iso
    }

    private fun daysBetween(fromIso: String, toIso: String): Int {
        val a = parseIso(fromIso)?.timeInMillis ?: return 999
        val b = parseIso(toIso)?.timeInMillis ?: return 999
        return ((b - a) / (24 * 60 * 60 * 1000L)).toInt()
    }
}
