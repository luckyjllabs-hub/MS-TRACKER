package com.jllabs.moneylens.parser.regex

import org.junit.Assert.assertEquals
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun testParseHdfcBankSms() {
        val sms = "Alert: HDFC Bank Card ending in 1234 spent INR 450.00 at Starbucks. Ref No: 987654321. Avail Bal: INR 45,000.00"
        val metadata = BankSmsParser.parseMetadata(sms)

        assertEquals("HDFC Bank", metadata.bankName)
        assertEquals("1234", metadata.cardLast4)
        assertEquals("987654321", metadata.referenceNumber)
        assertEquals(45000.00, metadata.availableBalance!!, 0.001)
    }

    @Test
    fun testParseIciciUpiSms() {
        val sms = "ICICI Bank A/C XX5678 debited for Rs 280.00 via VPA swiggy@okicici. Txn ID: 12345678. Avail Bal Rs 12,500.50"
        val metadata = BankSmsParser.parseMetadata(sms)

        assertEquals("ICICI Bank", metadata.bankName)
        assertEquals("5678", metadata.accountLast4)
        assertEquals("swiggy@okicici", metadata.upiId)
        assertEquals("12345678", metadata.referenceNumber)
        assertEquals(12500.50, metadata.availableBalance!!, 0.001)
    }

    @Test
    fun testParseSbiAccountSms() {
        val sms = "Dear SBI User, your Account ending XX9012 has been debited by Rs 1,500.00 on 01Aug26. Available Balance is Rs. 35,000.00"
        val metadata = BankSmsParser.parseMetadata(sms)

        assertEquals("SBI", metadata.bankName)
        assertEquals("9012", metadata.accountLast4)
        assertEquals(35000.00, metadata.availableBalance!!, 0.001)
    }
}
