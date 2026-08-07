package com.example.mstrackerapp.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mstrackerapp.domain.models.*

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["merchant"]),
        Index(value = ["date"]),
        Index(value = ["createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String,
    val merchantId: String? = null,
    val merchant: String,
    val bankName: String = "",
    val accountLast4: String = "",
    val referenceNumber: String = "",
    val date: String,
    val time: String,
    val note: String = "",
    val source: String = "Manual",
    val status: String = "CONFIRMED",
    val confidence: String = "HIGH",
    val isManual: Boolean = false,
    val isReviewed: Boolean = true,
    val rawSms: String = "",
    val availableBalanceMinor: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Transaction(
        id = id,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        amountMinor = amountMinor,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        merchant = merchant,
        date = date,
        time = time,
        note = note,
        source = source,
        createdAt = createdAt,
        bankName = bankName,
        accountLast4 = accountLast4,
        referenceNumber = referenceNumber,
        status = status,
        confidence = confidence,
        isManual = isManual,
        isReviewed = isReviewed,
        rawSms = rawSms,
        availableBalance = availableBalanceMinor
    )
}

@Entity(
    tableName = "categories",
    indices = [Index(value = ["order"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String = "#8F9C8A",
    val monthlyLimitMinor: Long = 0L,
    val order: Int = 0,
    val isArchived: Boolean = false
) {
    fun toDomain() = Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        monthlyLimitMinor = monthlyLimitMinor,
        order = order,
        isArchived = isArchived
    )

    companion object {
        fun fromDomain(category: Category) = CategoryEntity(
            id = category.id,
            name = category.name,
            icon = category.icon,
            color = category.color,
            monthlyLimitMinor = category.monthlyLimitMinor,
            order = category.order,
            isArchived = category.isArchived
        )
    }
}

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultCategoryId: String,
    val icon: String = "🏪"
)

@Entity(tableName = "merchant_mappings")
data class MerchantMappingEntity(
    @PrimaryKey val id: String,
    val rawSmsPattern: String,
    val merchantId: String,
    val categoryId: String
)

@Entity(tableName = "merchant_aliases")
data class MerchantAliasEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val aliasPattern: String
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["order"])]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val institution: String,
    val startingBalanceMinor: Long,
    val icon: String,
    val includeInNetWorth: Boolean = true,
    val isArchived: Boolean = false,
    val order: Int = 0
) {
    fun toDomain() = Account(
        id = id,
        name = name,
        type = try { AccountType.valueOf(type) } catch (e: Exception) { AccountType.BANK },
        institution = institution,
        startingBalanceMinor = startingBalanceMinor,
        icon = icon,
        includeInNetWorth = includeInNetWorth,
        isArchived = isArchived,
        order = order
    )
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val limitMinor: Long,
    val monthYear: String
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentSavedMinor: Long,
    val deadline: String,
    val icon: String,
    val linkedAccountId: String,
    val isCompleted: Boolean = false
) {
    fun toDomain() = Goal(
        id = id,
        name = name,
        targetAmountMinor = targetAmountMinor,
        currentSavedMinor = currentSavedMinor,
        deadline = deadline,
        icon = icon,
        linkedAccountId = linkedAccountId,
        isCompleted = isCompleted
    )
}

@Entity(
    tableName = "sms_inbox",
    indices = [Index(value = ["timestamp"])]
)
data class SMSInboxEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val isProcessed: Boolean = false,
    val messageType: String = "UNKNOWN",
    val processingStatus: String = "RECEIVED",
    val parserVersion: Int = 2,
    val rawHash: String = ""
)

@Entity(
    tableName = "sms_queue",
    indices = [Index(value = ["timestamp"])]
)
data class SMSQueueEntity(
    @PrimaryKey val id: String,
    val rawText: String,
    val bank: String,
    val amountMinor: Long,
    val merchant: String,
    val suggestedCategoryId: String,
    val suggestedAccountId: String,
    val confidence: String = "High Confidence",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain() = SmsQueueItem(
        id = id,
        rawText = rawText,
        bank = bank,
        amountMinor = amountMinor,
        merchant = merchant,
        suggestedCategoryId = suggestedCategoryId,
        suggestedAccountId = suggestedAccountId,
        confidence = confidence,
        timestamp = timestamp
    )
}

@Entity(tableName = "regex_rules")
data class RegexRuleEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val pattern: String,
    val ruleType: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String = "#3B7A57"
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val filePath: String,
    val mimeType: String = "image/jpeg"
)
