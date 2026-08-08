package com.jllabs.moneylens.domain.models

// Stage 1 - 15 message types
enum class MessageType {
    FINANCIAL_TRANSACTION, BANK_ALERT, OTP, PROMOTIONAL, SHOPPING,
    DELIVERY, RECHARGE, BILL_REMINDER, LOAN, KYC, SECURITY_ALERT,
    SERVICE_MESSAGE, TELECOM, PERSONAL, UNKNOWN
}

// Stage 4 - 14 transaction sub-types
enum class SmsTransactionSubType {
    SALARY, REFUND, ATM, EMI, CASH_DEPOSIT, UPI_PAYMENT, CARD_PURCHASE,
    SUBSCRIPTION, TRANSFER_IN, TRANSFER_OUT, INTEREST_CREDIT, INTEREST_DEBIT,
    CREDIT, DEBIT, CARD_BILL_PAYMENT, INFO_ALERT
}

enum class SmsProcessingStatus { PENDING, PROCESSING, SUCCESS, FILTERED, FAILED }

enum class TransactionType { EXPENSE, INCOME, TRANSFER, JUST_INFO }

enum class AccountType { BANK, CASH, WALLET, CREDIT_CARD, UPI, SAVINGS }

data class Account(
    val id: String, val name: String, val type: AccountType, val institution: String,
    val startingBalanceMinor: Long, val icon: String, val includeInNetWorth: Boolean = true,
    val isArchived: Boolean = false, val order: Int = 0
)

data class Category(
    val id: String, val name: String, val icon: String, val color: String = "#8F9C8A",
    val monthlyLimitMinor: Long = 0L, val order: Int = 0, val isArchived: Boolean = false
)

data class Transaction(
    val id: String, val type: TransactionType, val amountMinor: Long, val accountId: String,
    val toAccountId: String? = null, val categoryId: String, val merchant: String,
    val date: String, val time: String, val note: String = "", val source: String = "Manual",
    val tags: List<String> = emptyList(), val createdAt: Long = System.currentTimeMillis(),
    val bankName: String = "", val accountLast4: String = "", val referenceNumber: String = "",
    val status: String = "CONFIRMED", val confidence: String = "HIGH",
    val isManual: Boolean = false, val isReviewed: Boolean = true, val rawSms: String = "",
    val upiId: String = "", val availableBalance: Long? = null,
    val messageType: String = "", val smsTransactionSubType: String = "",
    val smsSender: String = ""
)

data class Goal(
    val id: String, val name: String, val targetAmountMinor: Long,
    val currentSavedMinor: Long, val deadline: String, val icon: String,
    val linkedAccountId: String, val isCompleted: Boolean = false
)

data class SmsQueueItem(
    val id: String, val rawText: String, val bank: String, val amountMinor: Long,
    val merchant: String, val suggestedCategoryId: String, val suggestedAccountId: String,
    val confidence: String = "High Confidence", val timestamp: Long = System.currentTimeMillis(),
    val transactionSubType: String = "", val refNumber: String = "",
    val upiId: String = "", val cardLast4: String = "", val availableBalance: Long? = null,
    val senderAddress: String = "", val messageType: String = "FINANCIAL_TRANSACTION",
    val confidenceScore: Int = 75
)
