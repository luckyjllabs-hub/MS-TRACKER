package com.example.mstrackerapp.data.parser

import com.example.mstrackerapp.data.database.dao.SMSQueueDao
import com.example.mstrackerapp.data.database.dao.TransactionDao
import com.example.mstrackerapp.data.database.entities.SMSQueueEntity
import com.example.mstrackerapp.data.database.entities.TransactionEntity
import com.example.mstrackerapp.domain.accounts.SmsAccountAggregator
import com.example.mstrackerapp.domain.models.SmsProcessingStatus
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.parser.classifier.CategoryLearningManager
import com.example.mstrackerapp.parser.pipeline.SmsProcessingPipeline
import com.example.mstrackerapp.parser.stage5.AccountParser
import com.example.mstrackerapp.parser.stage5.BalanceParser
import com.example.mstrackerapp.parser.stage5.BankParser
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
            // Still capture EPFO / balance-only SMS into Finance account list
            saveAccountBalanceHint(rawSmsText, senderBank, timestamp)
            return SmsQueueStatus.IGNORED
        }
        if (result.amountMinor <= 0L) {
            saveAccountBalanceHint(rawSmsText, senderBank, timestamp)
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
        val accountId = SmsAccountAggregator.accountIdFor(result.bank, last4)

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
        val accountId = queueItem.suggestedAccountId
            .takeIf { it.isNotBlank() && it != "acc-1" }
            ?: SmsAccountAggregator.accountIdFor(result.bank.ifBlank { queueItem.bank }, last4)
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
        val accountId = queueItem.suggestedAccountId
            .takeIf { it.isNotBlank() && it != "acc-1" }
            ?: SmsAccountAggregator.accountIdFor(result.bank.ifBlank { queueItem.bank }, last4)
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
     * Persist EPFO / pure-balance SMS as JUST_INFO so Finance can list the account + balance
     * without treating it as income/expense.
     */
    private suspend fun saveAccountBalanceHint(rawSmsText: String, senderBank: String, timestamp: Long) {
        val last4 = AccountParser.extractAccountOrCardLast4(rawSmsText)
        if (last4.isBlank()) return
        val balance = BalanceParser.extractBalanceMinor(rawSmsText) ?: return
        val bank = BankParser.extractBank(senderBank, rawSmsText)
        val dateStr = dateFormat.format(Date(timestamp))
        val timeStr = timeFormat.format(Date(timestamp))
        if (transactionDao.countDuplicateTransaction(rawSmsText, balance, dateStr) > 0) return

        val accountId = SmsAccountAggregator.accountIdFor(bank, last4)
        transactionDao.insertTransaction(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = TransactionType.JUST_INFO.name,
                amountMinor = balance,
                accountId = accountId,
                categoryId = "cat-14",
                merchant = "Balance update",
                bankName = bank,
                accountLast4 = last4,
                date = dateStr,
                time = timeStr,
                note = "Account balance from SMS (not a debit/credit)",
                source = "SMS_BALANCE",
                confidence = "INFO",
                isReviewed = true,
                rawSms = rawSmsText,
                availableBalanceMinor = balance
            )
        )
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
