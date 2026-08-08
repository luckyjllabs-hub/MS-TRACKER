package com.jllabs.moneylens.parser.classifier

import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.pipeline.DebitCreditDetector
import com.jllabs.moneylens.parser.pipeline.DetectedTransactionType
import com.jllabs.moneylens.parser.pipeline.FinancialFilter
import com.jllabs.moneylens.parser.pipeline.MessageTypeClassifier
import com.jllabs.moneylens.parser.pipeline.SuccessDetector

enum class TransactionClassificationCategory {
    DEBIT, CREDIT, SALARY, REFUND, TRANSFER, CASH_WITHDRAWAL, ATM, LOAN, EMI, INTEREST, UNKNOWN
}

enum class ClassificationConfidenceScore(val score: Double) {
    HIGH(0.95), MEDIUM(0.75), LOW(0.40), UNKNOWN(0.10)
}

/** Result of the SMS admission pipeline. Only [isFinancial] && [isSuccessful] messages may be queued. */
data class TransactionClassificationResult(
    val category: TransactionClassificationCategory,
    val transactionType: TransactionType,
    val confidence: ClassificationConfidenceScore,
    val score: Double,
    val isFinancial: Boolean = false,
    val isSuccessful: Boolean = false,
    val rejectionReason: String? = null
)

object RegexTransactionClassifier {
    private val nonFinancial = Regex("""\b(otp|one[ -]?time (?:password|pin|code)|verification code|passcode|2fa|password reset|login alert|kyc|courier|delivery|missed call|loan offer|credit card offer|advertisement|exclusive offer|apply now|click here|recharge reminder|bill reminder|emi reminder)\b""", RegexOption.IGNORE_CASE)
    private val unsuccessful = Regex("""\b(payment due|minimum amount due|upcoming payment|\bdue\b|reminder|scheduled|pending|failed|declined|reversed?|reversal|cancelled|could not process|attempted|initiated)\b""", RegexOption.IGNORE_CASE)
    private val debit = Regex("""\b(debited|auto debit|spent|paid|purchase|withdrawn|cash withdrawal|atm withdrawal|\batm\b|\bpos\b|upi payment|bill payment|emi deducted|subscription renewed|transfer to)\b""", RegexOption.IGNORE_CASE)
    private val credit = Regex("""\b(credited|salary|payroll|refund|interest credited|cashback|received|deposit|transfer received|cash deposit|neft credit|imps credit)\b""", RegexOption.IGNORE_CASE)
    private val success = Regex("""\b(debited|auto debit|credited|withdrawn|received|spent|paid|purchase|transfer(?:red)?|payment successful|cash withdrawal|pos purchase|atm withdrawal|cash deposit|deposit|refund)\b""", RegexOption.IGNORE_CASE)
    private val amount = Regex("""(?:₹|rs\.?|inr)\s*[\d,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)
    private val bank = Regex("""\b(hdfc|icici|sbi|axis|kotak|idfc|indusind|yes bank|federal bank|bank of baroda|canara)\b""", RegexOption.IGNORE_CASE)

    fun classify(smsText: String): TransactionClassificationResult {
        val text = smsText.trim()
        fun rejected(reason: String) = TransactionClassificationResult(
            TransactionClassificationCategory.UNKNOWN, TransactionType.EXPENSE,
            ClassificationConfidenceScore.UNKNOWN, 0.30, rejectionReason = reason
        )
        if (text.isBlank()) return rejected("Empty message")
        val messageType = MessageTypeClassifier.classify("", text)
        if (!FinancialFilter.isFinancialTransaction(text, messageType) || nonFinancial.containsMatchIn(text)) return rejected("Non-financial message: $messageType")
        if (!SuccessDetector.isCompleted(text) || unsuccessful.containsMatchIn(text)) return rejected("Incomplete or unsuccessful transaction")
        if (!success.containsMatchIn(text)) return rejected("No completed transaction signal")
        if (!amount.containsMatchIn(text)) return rejected("No monetary amount")

        val detectedType = DebitCreditDetector.detect(text)
        val isCredit = detectedType in setOf(DetectedTransactionType.CREDIT, DetectedTransactionType.SALARY, DetectedTransactionType.REFUND, DetectedTransactionType.INTEREST, DetectedTransactionType.CASH_DEPOSIT) || credit.containsMatchIn(text)
        val isDebit = detectedType in setOf(DetectedTransactionType.DEBIT, DetectedTransactionType.CASH_WITHDRAWAL, DetectedTransactionType.ATM, DetectedTransactionType.CARD_PURCHASE, DetectedTransactionType.UPI_PAYMENT, DetectedTransactionType.EMI, DetectedTransactionType.SUBSCRIPTION) || debit.containsMatchIn(text) || Regex("""\b(?:neft|imps|rtgs)\s+transfer\b[^.]{0,60}\bto\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        if (isCredit == isDebit) return rejected("Ambiguous debit or credit direction")

        val category = when {
            text.contains(Regex("""\bsalary|payroll\b""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.SALARY
            text.contains(Regex("""\brefund|cashback\b""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.REFUND
            text.contains(Regex("""\batm\b""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.ATM
            text.contains(Regex("""cash withdrawal""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.CASH_WITHDRAWAL
            text.contains(Regex("""\bemi\b""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.EMI
            text.contains(Regex("""\binterest\b""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.INTEREST
            text.contains(Regex("""\b(imps|neft|rtgs|transfer(?:red)?)\b""", RegexOption.IGNORE_CASE)) -> TransactionClassificationCategory.TRANSFER
            isCredit -> TransactionClassificationCategory.CREDIT
            else -> TransactionClassificationCategory.DEBIT
        }
        val type = when (category) {
            TransactionClassificationCategory.SALARY, TransactionClassificationCategory.REFUND,
            TransactionClassificationCategory.CREDIT, TransactionClassificationCategory.INTEREST -> TransactionType.INCOME
            TransactionClassificationCategory.TRANSFER -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        val score = if (bank.containsMatchIn(text)) 0.95 else 0.80
        return TransactionClassificationResult(category, type, if (score >= .9) ClassificationConfidenceScore.HIGH else ClassificationConfidenceScore.MEDIUM, score, true, true)
    }
}
