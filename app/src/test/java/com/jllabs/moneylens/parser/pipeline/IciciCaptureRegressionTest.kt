package com.jllabs.moneylens.parser.pipeline

import com.jllabs.moneylens.domain.models.SmsProcessingStatus
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.stage1.MessageTypeClassifier
import com.jllabs.moneylens.parser.stage3.NonTransactionAlertFilter
import com.jllabs.moneylens.parser.stage3.SuccessDetector
import com.jllabs.moneylens.parser.stage4.DebitCreditDetector
import com.jllabs.moneylens.parser.stage5.AmountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IciciCaptureRegressionTest {

    @Test
    fun `icici debit with InfoBIL and BLOCK footer is captured`() {
        val body =
            "ICICI Bank Acc XX346 debited Rs. 60,788.80 on 31-Jul-26 InfoBIL*INFT*FGZ9.Avl Bal Rs. 13,52,032.84.To dispute call 18002662 or SMS BLOCK 346 to 9215676766"
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertTrue(SuccessDetector.isSuccessful(body))
        val r = SmsProcessingPipeline.process("VK-ICICIB-S", body, 0L)
        assertEquals("filter=${r.filterReason}", SmsProcessingStatus.SUCCESS, r.status)
        assertEquals(TransactionType.EXPENSE, r.transactionType)
        assertEquals(6_078_880L, r.amountMinor)
    }

    @Test
    fun `icici credit with credited colon Rs and NEFT is captured`() {
        val body =
            "ICICI Bank Account XX346 credited:Rs. 4,35,704.00 on 31-Jul-26. Info NEFT-HSBCN21271399739-SAMSUN. Available Balance is Rs. 14,12,821.64."
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertTrue(SuccessDetector.isSuccessful(body))
        assertEquals(43_570_400L, AmountParser.parseAmountMinor(body))
        println("dc=" + DebitCreditDetector.detect(body))
        println("type=" + MessageTypeClassifier.classify("VK-ICICIB-S", body))
        val r = SmsProcessingPipeline.process("VK-ICICIB-S", body, 0L)
        assertEquals("filter=${r.filterReason}", SmsProcessingStatus.SUCCESS, r.status)
        assertTrue(
            "type=${r.transactionType}",
            r.transactionType == TransactionType.INCOME || r.transactionType == TransactionType.TRANSFER
        )
        assertEquals(43_570_400L, r.amountMinor)
    }
}
