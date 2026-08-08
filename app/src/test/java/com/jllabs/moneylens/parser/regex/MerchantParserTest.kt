package com.jllabs.moneylens.parser.regex

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantParserTest {

    @Test
    fun testParseUpiMerchant() {
        val result = MerchantParser.parseMerchant("Alert: Paid Rs 450 via VPA swiggy@hdfcbank on 01-08-2026")
        assertEquals("SWIGGY", result)
    }

    @Test
    fun testParseCardMerchant() {
        val result = MerchantParser.parseMerchant("INR 1,450.50 spent on Card 1234 at STARBUCKS on 01-Aug")
        assertEquals("STARBUCKS", result)
    }

    @Test
    fun testParsePosMerchant() {
        val result = MerchantParser.parseMerchant("Txn of Rs 2,500.00 at POS txn at RELIANCE RETAIL Mumbai")
        assertEquals("RELIANCE RETAIL MUMBAI", result)
    }

    @Test
    fun testParseAtmWithdrawal() {
        val result = MerchantParser.parseMerchant("Rs 5,000 debited for ATM WDL HDFC BANK ATM MUMBAI")
        assertEquals("ATM - HDFC BANK ATM MUMBAI", result)
    }

    @Test
    fun testParseNeftCredit() {
        val result = MerchantParser.parseMerchant("NEFT Cr-SALARY ACME CORP ref 987654321 credited")
        assertEquals("SALARY ACME CORP", result)
    }

    @Test
    fun testParseImpsMerchant() {
        val result = MerchantParser.parseMerchant("IMPS/P2A/6789/AMAZON PAY credited Rs 1,200.00")
        assertEquals("AMAZON PAY", result)
    }
}
