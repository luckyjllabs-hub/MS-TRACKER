package com.example.mstrackerapp.parser.stage4

import com.example.mstrackerapp.domain.models.SmsTransactionSubType
import com.example.mstrackerapp.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class DebitCreditDetectorTest {

    @Test
    fun `detects salary credit`() {
        val result = DebitCreditDetector.detect("Salary credited to your account Rs.50000")
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(SmsTransactionSubType.SALARY, result.subType)
    }

    @Test
    fun `detects ATM withdrawal`() {
        val result = DebitCreditDetector.detect("ATM WDL Rs.5000 from A/C XX1234")
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(SmsTransactionSubType.ATM, result.subType)
    }

    @Test
    fun `detects refund`() {
        val result = DebitCreditDetector.detect("Refund of Rs.500 credited to your account")
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(SmsTransactionSubType.REFUND, result.subType)
    }

    @Test
    fun `detects UPI debit`() {
        val result = DebitCreditDetector.detect("UPI Rs.200 debited from A/C XX1234. VPA user@upi")
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(SmsTransactionSubType.UPI_PAYMENT, result.subType)
    }
}
