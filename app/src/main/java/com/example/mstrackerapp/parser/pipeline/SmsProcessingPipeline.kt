package com.example.mstrackerapp.parser.pipeline

import com.example.mstrackerapp.domain.models.MessageType
import com.example.mstrackerapp.domain.models.SmsProcessingStatus
import com.example.mstrackerapp.parser.stage1.MessageTypeClassifier
import com.example.mstrackerapp.parser.stage2.FinancialFilter
import com.example.mstrackerapp.parser.stage3.SuccessDetector
import com.example.mstrackerapp.parser.stage4.DebitCreditDetector
import com.example.mstrackerapp.parser.stage5.*
import com.example.mstrackerapp.parser.stage6.SmsCategory
import com.example.mstrackerapp.parser.stage7.ConfidenceCalculator

object SmsProcessingPipeline {

    /**
     * Processes an SMS through all 8 stages.
     * @param userMappings Map of merchant name -> categoryId from user_learned_mappings
     */
    fun process(
        sender: String,
        body: String,
        timestamp: Long,
        userMappings: Map<String, String> = emptyMap()
    ): ParsedSmsResult {

        // --- Stage 1: Message Type Classification ---
        val messageType = MessageTypeClassifier.classify(sender, body)
        if (messageType != MessageType.FINANCIAL_TRANSACTION) {
            return ParsedSmsResult(
                status = SmsProcessingStatus.FILTERED,
                messageType = messageType,
                filterReason = "Stage1: Non-financial type: $messageType"
            )
        }

        // --- Stage 2: Financial Filter ---
        if (!FinancialFilter.passes(sender, body)) {
            return ParsedSmsResult(
                status = SmsProcessingStatus.FILTERED,
                messageType = messageType,
                filterReason = "Stage2: Failed financial filter (no bank sender + amount + verb)"
            )
        }

        // --- Stage 3: Success Detection ---
        if (!SuccessDetector.isSuccessful(body)) {
            return ParsedSmsResult(
                status = SmsProcessingStatus.FILTERED,
                messageType = messageType,
                filterReason = "Stage3: Non-successful transaction (pending/failed/reminder)"
            )
        }

        // --- Stage 4: Debit/Credit Detection ---
        val debitCreditResult = DebitCreditDetector.detect(body)

        // --- Stage 5: Information Extraction ---
        val amountMinor = AmountParser.parseAmountMinor(body) ?: 0L
        if (amountMinor <= 0L) {
            return ParsedSmsResult(
                status = SmsProcessingStatus.FILTERED,
                messageType = messageType,
                filterReason = "Stage5: Could not extract a valid amount"
            )
        }
        val bank = BankParser.extractBank(sender, body)
        val merchant = MerchantExtractor.extractMerchant(body)
        val referenceNumber = ReferenceParser.extractReference(body)
        val upiId = ReferenceParser.extractUpiId(body)
        val cardLast4 = AccountParser.extractCardLast4(body)
        val accountLast4 = AccountParser.extractAccountLast4(body)
        val availableBalance = BalanceParser.extractBalanceMinor(body)

        // --- Stage 6: Category Classification ---
        val categoryResult = SmsCategory.classify(merchant, body, debitCreditResult.subType, userMappings)

        // --- Stage 7: Confidence Calculation ---
        val isKnownBank = bank != "Unknown Bank" && !bank.equals(sender, ignoreCase = true)
        val isMerchantKnown = merchant != "Unknown"
        val confidenceResult = ConfidenceCalculator.calculate(
            isKnownBankSender = isKnownBank,
            amountFound = amountMinor > 0,
            isMerchantKnown = isMerchantKnown,
            hasReferenceNumber = referenceNumber.isNotEmpty(),
            hasBalance = availableBalance != null,
            isTransactionTypeClear = true,
            hasUpiId = upiId.isNotEmpty(),
            categorySource = categoryResult.source
        )

        // --- Stage 8: Route ---
        return ParsedSmsResult(
            status = SmsProcessingStatus.SUCCESS,
            messageType = messageType,
            transactionType = debitCreditResult.transactionType,
            subType = debitCreditResult.subType,
            amountMinor = amountMinor,
            bank = bank,
            merchant = merchant,
            categoryId = categoryResult.categoryId,
            categorySource = categoryResult.source,
            referenceNumber = referenceNumber,
            upiId = upiId,
            cardLast4 = cardLast4,
            accountLast4 = accountLast4,
            availableBalanceMinor = availableBalance,
            confidenceScore = confidenceResult.score,
            confidenceLabel = confidenceResult.label,
            goToQueue = confidenceResult.goToQueue,
            filterReason = ""
        )
    }
}
