package com.jllabs.moneylens.parser.pipeline

import com.jllabs.moneylens.domain.models.MessageType
import com.jllabs.moneylens.domain.models.SmsTransactionSubType
import com.jllabs.moneylens.domain.models.SmsProcessingStatus
import com.jllabs.moneylens.domain.models.TransactionType

data class ParsedSmsResult(
    val status: SmsProcessingStatus,
    val messageType: MessageType = MessageType.UNKNOWN,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val subType: SmsTransactionSubType = SmsTransactionSubType.DEBIT,
    val amountMinor: Long = 0L,
    val bank: String = "",
    val merchant: String = "",
    val categoryId: String = "cat-14",
    val categorySource: String = "UNKNOWN",
    val referenceNumber: String = "",
    val upiId: String = "",
    val cardLast4: String = "",
    val accountLast4: String = "",
    val availableBalanceMinor: Long? = null,
    val confidenceScore: Int = 0,
    val confidenceLabel: String = "",
    val goToQueue: Boolean = true,
    val filterReason: String = ""
)
