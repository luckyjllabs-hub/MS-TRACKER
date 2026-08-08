package com.jllabs.moneylens.parser.pipeline

import com.jllabs.moneylens.domain.models.MessageType
import com.jllabs.moneylens.domain.models.SmsProcessingStatus
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.stage1.MessageTypeClassifier
import com.jllabs.moneylens.parser.stage3.NonTransactionAlertFilter
import com.jllabs.moneylens.parser.stage3.SuccessDetector
import com.jllabs.moneylens.parser.stage4.DebitCreditDetector
import com.jllabs.moneylens.parser.stage5.AmountParser
import org.junit.Assert.*
import org.junit.Test

class IciciDebitCaptureRegressionTest {
    private val body = "ICICI Bank Acc XX346 debited Rs. 60,788.80 on 31-Jul-26 InfoBIL*INFT*FGZ9.Avl Bal Rs. 13,52,032.84.To dispute call 18002662 or SMS BLOCK 346 to 9215676766"

    @Test
    fun diagnose() {
        println("nonTxn=" + NonTransactionAlertFilter.isNonTransactionAlert(body))
        println("success=" + SuccessDetector.isSuccessful(body))
        println("msgType=" + MessageTypeClassifier.classify("VK-ICICIB-S", body))
        println("dc=" + DebitCreditDetector.detect(body))
        println("amt=" + AmountParser.parseAmountMinor(body))
        val r = SmsProcessingPipeline.process("VK-ICICIB-S", body, System.currentTimeMillis())
        println("status=${r.status} type=${r.transactionType} amt=${r.amountMinor} reason=${r.filterReason}")
        assertEquals(SmsProcessingStatus.SUCCESS, r.status)
        assertEquals(TransactionType.EXPENSE, r.transactionType)
        assertEquals(6078880L, r.amountMinor)
    }
}
