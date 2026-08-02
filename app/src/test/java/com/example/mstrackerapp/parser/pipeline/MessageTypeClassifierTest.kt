package com.example.mstrackerapp.parser.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTypeClassifierTest {
    @Test fun `classifies real ICICI UPI debit as financial`() {
        val body = "ICICI Bank Acct XX346 debited for Rs 825.00; SANCHI credited. UPI:621461530689"
        assertEquals(SmsMessageType.FINANCIAL_TRANSACTION, MessageTypeClassifier.classify("AD-ICICIT-S", body))
        assertTrue(FinancialFilter.isFinancialTransaction(body, MessageTypeClassifier.classify("AD-ICICIT-S", body)))
        assertTrue(SuccessDetector.isCompleted(body))
        assertEquals(DetectedTransactionType.UPI_PAYMENT, DebitCreditDetector.detect(body))
    }

    @Test fun `rejects real OTP and utility due notice`() {
        val otp = "3022 is your One Time Password (OTP) for Lenskart. OTP is valid for 15 mins."
        val due = "Please pay the pending dues Rs.1,242 immediately for your DPNG Connection."
        assertEquals(SmsMessageType.OTP, MessageTypeClassifier.classify("BT-LENSKT-S", otp))
        assertEquals(SmsMessageType.BILL_REMINDER, MessageTypeClassifier.classify("VM-GGLIND-S", due))
        assertFalse(SuccessDetector.isCompleted(due))
    }

    @Test fun `classifies retailer offer as promotional`() {
        assertEquals(SmsMessageType.PROMOTIONAL, MessageTypeClassifier.classify("VM-MYCLIQ-S", "Your requested Westside update is ready. Check the latest Sale. Check Now"))
    }
}
