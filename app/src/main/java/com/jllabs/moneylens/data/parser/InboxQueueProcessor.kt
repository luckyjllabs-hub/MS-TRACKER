package com.jllabs.moneylens.data.parser

import com.jllabs.moneylens.data.database.dao.SMSQueueDao
import com.jllabs.moneylens.data.database.dao.TransactionDao
import com.jllabs.moneylens.data.database.entities.SMSQueueEntity
import com.jllabs.moneylens.data.database.entities.TransactionEntity
import com.jllabs.moneylens.domain.accounts.SmsAccountAggregator
import com.jllabs.moneylens.domain.models.SmsProcessingStatus
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.domain.reminders.ReminderExtractor
import com.jllabs.moneylens.domain.reminders.ReminderKind
import com.jllabs.moneylens.parser.classifier.CategoryLearningManager
import com.jllabs.moneylens.parser.pipeline.SmsProcessingPipeline
import com.jllabs.moneylens.parser.stage5.AccountParser
import com.jllabs.moneylens.parser.stage5.AmountParser
import com.jllabs.moneylens.parser.stage5.BalanceParser
import com.jllabs.moneylens.parser.stage5.BankParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SmsQueueStatus {
    PENDING_REVIEW, ACCEPTED_AUTO, ACCEPTED_MANUAL, IGNORED, DELETED
}

class InboxQueueProcessor(
    private val smsQueueDao: SMSQueueDao,
    private val transactionDao: TransactionDao,
    private val categoryLearningManager: CategoryLearningManager? = null
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    suspend fun processParsedSms(
        rawSmsText: String,
        senderBank: String,
        timestamp: Long,
        customMappings: Map<String, String> = emptyMap()
    ): SmsQueueStatus {
        // Dedup check
        if (smsQueueDao.countByRawTextAndTimestamp(rawSmsText, timestamp) > 0) {
            return SmsQueueStatus.PENDING_REVIEW
        }

        // Run 8-stage pipeline
        val result = SmsProcessingPipeline.process(senderBank, rawSmsText, timestamp, customMappings)

        if (result.status == SmsProcessingStatus.FILTERED) {
            // Still capture EPFO / balance / EMI-due / loan account SMS into Finance + Reminders
            saveAccountDiscoveryHint(rawSmsText, senderBank, timestamp)
            return SmsQueueStatus.IGNORED
        }
        if (result.amountMinor <= 0L) {
            saveAccountDiscoveryHint(rawSmsText, senderBank, timestamp)
            return SmsQueueStatus.IGNORED
        }

        val dateStr = dateFormat.format(Date(timestamp))
        val timeStr = timeFormat.format(Date(timestamp))

        // Dedup against transactions (check amount + date or raw SMS)
        if (transactionDao.countDuplicateTransaction(rawSmsText, result.amountMinor, dateStr) > 0) {
            return SmsQueueStatus.ACCEPTED_AUTO
        }

        val last4 = result.accountLast4.ifBlank {
            AccountParser.extractAccountOrCardLast4(rawSmsText)
        }
        val isCc = AccountParser.isCreditCardSms(rawSmsText)
        val isLoan = !isCc && AccountParser.isLoanAccountSms(rawSmsText)
        val isFasTag = !isCc && !isLoan && AccountParser.isFasTagSms(rawSmsText)
        val accountId = SmsAccountAggregator.accountIdFor(result.bank, last4, isCc, isLoan, isFasTag)

        return if (!result.goToQueue) {
            // High confidence — auto-accept to ledger
            val txEntity = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = result.transactionType.name,
                amountMinor = result.amountMinor,
                accountId = accountId,
                categoryId = result.categoryId,
                merchant = result.merchant,
                bankName = result.bank,
                accountLast4 = last4,
                referenceNumber = result.referenceNumber,
                date = dateStr,
                time = timeStr,
                note = "Auto-accepted [${result.subType.name}] from ${result.bank}",
                source = "SMS_AUTO",
                confidence = "${result.confidenceScore}%",
                isReviewed = true,
                rawSms = rawSmsText,
                availableBalanceMinor = result.availableBalanceMinor
            )
            transactionDao.insertTransaction(txEntity)
            SmsQueueStatus.ACCEPTED_AUTO
        } else {
            // Send to review queue
            val queueItem = SMSQueueEntity(
                id = UUID.randomUUID().toString(),
                rawText = rawSmsText,
                bank = result.bank,
                amountMinor = result.amountMinor,
                merchant = result.merchant,
                suggestedCategoryId = result.categoryId,
                suggestedAccountId = accountId,
                confidence = "${result.confidenceScore}% - ${result.confidenceLabel}",
                timestamp = timestamp
            )
            smsQueueDao.insertSmsItem(queueItem)
            SmsQueueStatus.PENDING_REVIEW
        }
    }

    suspend fun acceptItem(queueItem: SMSQueueEntity) {
        val result = SmsProcessingPipeline.process(queueItem.bank, queueItem.rawText, queueItem.timestamp)
        val last4 = result.accountLast4.ifBlank {
            AccountParser.extractAccountOrCardLast4(queueItem.rawText)
        }
        val isCc = AccountParser.isCreditCardSms(queueItem.rawText)
        val isLoan = !isCc && AccountParser.isLoanAccountSms(queueItem.rawText)
        val isFasTag = !isCc && !isLoan && AccountParser.isFasTagSms(queueItem.rawText)
        val accountId = queueItem.suggestedAccountId
            .takeIf { it.isNotBlank() && it != "acc-1" }
            ?: SmsAccountAggregator.accountIdFor(result.bank.ifBlank { queueItem.bank }, last4, isCc, isLoan, isFasTag)
        val txEntity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = result.transactionType.name,
            amountMinor = queueItem.amountMinor,
            accountId = accountId,
            categoryId = queueItem.suggestedCategoryId,
            merchant = queueItem.merchant,
            date = dateFormat.format(Date(queueItem.timestamp)),
            time = timeFormat.format(Date(queueItem.timestamp)),
            note = "Accepted from ${queueItem.bank} SMS",
            source = "SMS_MANUAL",
            bankName = queueItem.bank,
            accountLast4 = last4,
            confidence = queueItem.confidence,
            isReviewed = true,
            rawSms = queueItem.rawText,
            availableBalanceMinor = result.availableBalanceMinor
        )
        transactionDao.insertTransaction(txEntity)
        smsQueueDao.deleteSmsItem(queueItem.id)
    }

    suspend fun changeCategoryAndAccept(queueItem: SMSQueueEntity, newCategoryId: String) {
        categoryLearningManager?.onUserChangedCategory(queueItem.merchant, newCategoryId)
        val result = SmsProcessingPipeline.process(queueItem.bank, queueItem.rawText, queueItem.timestamp)
        val last4 = result.accountLast4.ifBlank {
            AccountParser.extractAccountOrCardLast4(queueItem.rawText)
        }
        val isCc = AccountParser.isCreditCardSms(queueItem.rawText)
        val isLoan = !isCc && AccountParser.isLoanAccountSms(queueItem.rawText)
        val isFasTag = !isCc && !isLoan && AccountParser.isFasTagSms(queueItem.rawText)
        val accountId = queueItem.suggestedAccountId
            .takeIf { it.isNotBlank() && it != "acc-1" }
            ?: SmsAccountAggregator.accountIdFor(result.bank.ifBlank { queueItem.bank }, last4, isCc, isLoan, isFasTag)
        val txEntity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = result.transactionType.name,
            amountMinor = queueItem.amountMinor,
            accountId = accountId,
            categoryId = newCategoryId,
            merchant = queueItem.merchant,
            date = dateFormat.format(Date(queueItem.timestamp)),
            time = timeFormat.format(Date(queueItem.timestamp)),
            note = "Accepted with category update from ${queueItem.bank} SMS",
            source = "SMS_MANUAL",
            bankName = queueItem.bank,
            accountLast4 = last4,
            confidence = queueItem.confidence,
            isReviewed = true,
            rawSms = queueItem.rawText,
            availableBalanceMinor = result.availableBalanceMinor
        )
        transactionDao.insertTransaction(txEntity)
        smsQueueDao.deleteSmsItem(queueItem.id)
    }

    suspend fun ignoreItem(queueItemId: String) { smsQueueDao.deleteSmsItem(queueItemId) }
    suspend fun deleteItem(queueItemId: String) { smsQueueDao.deleteSmsItem(queueItemId) }

    /**
     * Persist filtered SMS that still belong in Finance / Reminders:
     * - EPFO contribution / passbook balance
     * - Pure balance / loan balance notices
     * - EMI / payment due reminders (no balance required)
     */
    private suspend fun saveAccountDiscoveryHint(rawSmsText: String, senderBank: String, timestamp: Long) {
        val contribution = AmountParser.parseEpfoContributionMinor(rawSmsText)
        val isDue = isDueOrReminderSms(rawSmsText)
        val hasAccount = hasIdentifiableAccountNumber(rawSmsText)

        // Account screen: only create account-linked rows when SMS has a real a/c / card / loan / FASTag vehicle.
        // Due / EPFO can still be stored without an account (Reminders) — aggregator skips blank last4.
        if (!hasAccount && contribution == null && !isDue) return

        val last4 = if (hasAccount) {
            AccountParser.extractAccountOrCardLast4(rawSmsText)
        } else {
            ""
        }
        if (hasAccount && last4.isBlank()) return
        if (hasAccount &&
            last4.equals("FTAG", ignoreCase = true) &&
            AccountParser.extractVehicleNumber(rawSmsText).isBlank()
        ) return

        val balance = BalanceParser.extractDisplayBalanceMinor(rawSmsText)
        val isLoan = AccountParser.isLoanAccountSms(rawSmsText)
        val isFasTag = AccountParser.isFasTagSms(rawSmsText)
        val hasMaskedAccount = Regex(
            """(?i)(?:a/?c|acct|account).{0,24}[xX*]{2,}|a/c\s*no"""
        ).containsMatchIn(rawSmsText)
        // Persist discovery for balances, dues, loans, FASTag, or clear masked a/c (KYC etc.)
        if (balance == null && contribution == null && !isDue && !isLoan && !isFasTag && !hasMaskedAccount) return

        val bank = BankParser.extractBank(senderBank, rawSmsText)
        val dateStr = dateFormat.format(Date(timestamp))
        val timeStr = timeFormat.format(Date(timestamp))
        val amountMinor = when {
            contribution != null -> contribution
            isDue -> AmountParser.parseAmountMinor(rawSmsText) ?: 0L
            else -> 0L
        }
        val dupAmounts = listOfNotNull(amountMinor, balance, contribution, 0L).distinct()
        if (dupAmounts.any { transactionDao.countDuplicateTransaction(rawSmsText, it, dateStr) > 0 }) return

        val isCc = AccountParser.isCreditCardSms(rawSmsText)
        val accountId = if (last4.isBlank()) {
            "acc-1"
        } else {
            SmsAccountAggregator.accountIdFor(
                bank, last4, isCc, isLoan && !isCc, isFasTag && !isCc && !isLoan
            )
        }
        val type: String
        val merchant: String
        val note: String
        val source: String
        val categoryId: String
        when {
            contribution != null -> {
                type = TransactionType.INCOME.name
                merchant = "EPFO contribution"
                note = "EPFO contribution credited; passbook balance updated"
                source = "SMS_EPFO"
                categoryId = "cat-1"
            }
            isDue -> {
                type = TransactionType.JUST_INFO.name
                merchant = if (Regex("""(?i)\bemi\b""").containsMatchIn(rawSmsText)) "EMI due" else "Payment due"
                note = "Due reminder from SMS (not a debit/credit)"
                source = "SMS_REMINDER"
                categoryId = "cat-14"
            }
            isLoan -> {
                type = TransactionType.JUST_INFO.name
                merchant = "Loan account"
                note = "Loan account from SMS"
                source = "SMS_LOAN"
                categoryId = "cat-14"
            }
            isFasTag -> {
                type = TransactionType.JUST_INFO.name
                merchant = "FASTag"
                note = "FASTag account from SMS"
                source = "SMS_FASTAG"
                categoryId = "cat-14"
            }
            hasMaskedAccount && balance == null -> {
                type = TransactionType.JUST_INFO.name
                merchant = "Account notice"
                note = "Account discovered from SMS"
                source = "SMS_ACCOUNT"
                categoryId = "cat-14"
            }
            else -> {
                type = TransactionType.JUST_INFO.name
                merchant = "Balance update"
                note = "Account balance from SMS (not a debit/credit)"
                source = "SMS_BALANCE"
                categoryId = "cat-14"
            }
        }
        transactionDao.insertTransaction(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                amountMinor = amountMinor,
                accountId = accountId,
                categoryId = categoryId,
                merchant = merchant,
                bankName = bank,
                accountLast4 = last4,
                date = dateStr,
                time = timeStr,
                note = note,
                source = source,
                confidence = "INFO",
                isReviewed = true,
                rawSms = rawSmsText,
                availableBalanceMinor = balance
            )
        )
    }

    private fun isDueOrReminderSms(body: String): Boolean {
        val reminder = ReminderExtractor.fromSms(body, "probe", "2000-01-01", "") ?: return Regex(
            """(?i)\b(?:emi\b.{0,120}\bis\s+due\b|\bis\s+due\b.{0,40}\b(?:on|by)\b|payment\s+due|minimum\s+(?:amount\s+)?due)\b"""
        ).containsMatchIn(body)
        return reminder.kind == ReminderKind.PAYMENT_DUE || reminder.kind == ReminderKind.BILL
    }

    /** True when SMS explicitly names a masked bank a/c, card, loan a/c, EPFO UAN, or FASTag vehicle. */
    private fun hasIdentifiableAccountNumber(body: String): Boolean {
        if (AccountParser.isInsuranceOrPolicySms(body) &&
            !Regex("""(?i)(?:bank\s+)?(?:a/?c|acct|account)\s*(?:no\.?)?\s*(?:ending(?:\s*(?:in|with))?)?\s*[xX*]+\d{3,}""")
                .containsMatchIn(body)
        ) {
            return false
        }
        if (AccountParser.isFasTagSms(body)) {
            return AccountParser.extractVehicleNumber(body).isNotBlank()
        }
        val patterns = listOf(
            Regex("""(?i)(?:a/?c|acct|account).{0,24}[xX*]+\d{3,}"""),
            Regex("""(?i)(?:card|loan).{0,24}[xX*]+\d{3,}"""),
            Regex("""(?i)credit\s*card\s+account\s+\d?[xX*]+\d{3,}"""),
            Regex("""(?i)[A-Z]{3,8}\*{6,}\d{3,}""")
        )
        return patterns.any { it.containsMatchIn(body) }
    }

    companion object {
        // Keep these for backward compatibility if called from anywhere
        fun isOtpMessage(text: String): Boolean {
            val OTP_REGEX = Regex("""(?i)\b(?:otp|one[\s\-]time[\s\-](?:password|pin|code)|verification[\s\-]*code|do\s*not\s*share|passcode|2fa)\b""")
            return OTP_REGEX.containsMatchIn(text)
        }
        fun isPromotionalMessage(text: String): Boolean {
            val PROMO_REGEX = Regex("""(?i)\b(?:exclusive\s+offer|limited\s+time|click\s+here|apply\s+now|discount|cashback\s+offer|survey|feedback|win\s+a|lucky\s+draw)\b""")
            return PROMO_REGEX.containsMatchIn(text)
        }
    }
}
