package com.jllabs.moneylens.parser.stage4

import com.jllabs.moneylens.domain.models.SmsTransactionSubType
import com.jllabs.moneylens.domain.models.TransactionType
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

    @Test
    fun `available balance does not turn own-account credit into info`() {
        val body = "Your A/c *1737 is credited with Rs.15000.00 on 31-07-26 by SHWETANK CHOUDHARY. Available balance is Rs. 18025.20 - Indian Bank"
        val result = DebitCreditDetector.detect(body)
        assertEquals(TransactionType.INCOME, result.transactionType)
    }

    @Test
    fun `icici credited colon form is income despite available balance`() {
        val body = "ICICI Bank Account XX932 credited:Rs.456666.00 on 31-Jul-26. Info NEFT-HSBCN21271399500-SAMSUN. Available Balance is Rs. 5677788"
        val result = DebitCreditDetector.detect(body)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(SmsTransactionSubType.TRANSFER_IN, result.subType)
    }
}
