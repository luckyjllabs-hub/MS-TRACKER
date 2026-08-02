package com.example.mstrackerapp.data.parser

import com.example.mstrackerapp.data.database.dao.SMSQueueDao
import com.example.mstrackerapp.data.database.dao.TransactionDao
import com.example.mstrackerapp.data.database.entities.SMSQueueEntity
import com.example.mstrackerapp.data.database.entities.TransactionEntity
import com.example.mstrackerapp.domain.models.SmsProcessingStatus
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.parser.classifier.CategoryLearningManager
import com.example.mstrackerapp.parser.pipeline.SmsProcessingPipeline
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
            return SmsQueueStatus.IGNORED
        }
        if (result.amountMinor <= 0L) {
            return SmsQueueStatus.IGNORED
        }

        val dateStr = dateFormat.format(Date(timestamp))
        val timeStr = timeFormat.format(Date(timestamp))

        // Dedup against transactions (check amount + date or raw SMS)
        if (transactionDao.countDuplicateTransaction(rawSmsText, result.amountMinor, dateStr) > 0) {
            return SmsQueueStatus.ACCEPTED_AUTO
        }

        return if (!result.goToQueue) {
            // High confidence — auto-accept to ledger
            val txEntity = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = result.transactionType.name,
                amountMinor = result.amountMinor,
                accountId = "acc-1",
                categoryId = result.categoryId,
                merchant = result.merchant,
                bankName = result.bank,
                accountLast4 = result.accountLast4,
                referenceNumber = result.referenceNumber,
                date = dateStr,
                time = timeStr,
                note = "Auto-accepted [${result.subType.name}] from ${result.bank}",
                source = "SMS_AUTO",
                confidence = "${result.confidenceScore}%",
                isReviewed = true,
                rawSms = rawSmsText
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
                suggestedAccountId = "acc-1",
                confidence = "${result.confidenceScore}% - ${result.confidenceLabel}",
                timestamp = timestamp
            )
            smsQueueDao.insertSmsItem(queueItem)
            SmsQueueStatus.PENDING_REVIEW
        }
    }

    suspend fun acceptItem(queueItem: SMSQueueEntity) {
        val result = SmsProcessingPipeline.process(queueItem.bank, queueItem.rawText, queueItem.timestamp)
        val txEntity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = result.transactionType.name,
            amountMinor = queueItem.amountMinor,
            accountId = queueItem.suggestedAccountId,
            categoryId = queueItem.suggestedCategoryId,
            merchant = queueItem.merchant,
            date = dateFormat.format(Date(queueItem.timestamp)),
            time = timeFormat.format(Date(queueItem.timestamp)),
            note = "Accepted from ${queueItem.bank} SMS",
            source = "SMS_MANUAL",
            bankName = queueItem.bank,
            confidence = queueItem.confidence,
            isReviewed = true,
            rawSms = queueItem.rawText
        )
        transactionDao.insertTransaction(txEntity)
        smsQueueDao.deleteSmsItem(queueItem.id)
    }

    suspend fun changeCategoryAndAccept(queueItem: SMSQueueEntity, newCategoryId: String) {
        categoryLearningManager?.onUserChangedCategory(queueItem.merchant, newCategoryId)
        val result = SmsProcessingPipeline.process(queueItem.bank, queueItem.rawText, queueItem.timestamp)
        val txEntity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = result.transactionType.name,
            amountMinor = queueItem.amountMinor,
            accountId = queueItem.suggestedAccountId,
            categoryId = newCategoryId,
            merchant = queueItem.merchant,
            date = dateFormat.format(Date(queueItem.timestamp)),
            time = timeFormat.format(Date(queueItem.timestamp)),
            note = "Accepted with category update from ${queueItem.bank} SMS",
            source = "SMS_MANUAL",
            bankName = queueItem.bank,
            confidence = queueItem.confidence,
            isReviewed = true,
            rawSms = queueItem.rawText
        )
        transactionDao.insertTransaction(txEntity)
        smsQueueDao.deleteSmsItem(queueItem.id)
    }

    suspend fun ignoreItem(queueItemId: String) { smsQueueDao.deleteSmsItem(queueItemId) }
    suspend fun deleteItem(queueItemId: String) { smsQueueDao.deleteSmsItem(queueItemId) }

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
