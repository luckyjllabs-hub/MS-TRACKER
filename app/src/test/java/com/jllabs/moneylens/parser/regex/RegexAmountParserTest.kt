package com.jllabs.moneylens.parser.regex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegexAmountParserTest {

    @Test
    fun testParseInrAmount() {
        val result = RegexAmountParser.parseAmount("Alert: Spend of INR 450.00 on Food at Starbucks")
        assertEquals(450.00, result!!, 0.001)
    }

    @Test
    fun testParseRupeesSymbolWithComma() {
        val result = RegexAmountParser.parseAmount("Txn: ₹ 1,450.50 debited from HDFC card 1234")
        assertEquals(1450.50, result!!, 0.001)
    }

    @Test
    fun testParseRsDotFormat() {
        val result = RegexAmountParser.parseAmount("Your account was debited for Rs. 10,000 at Uber")
        assertEquals(10000.00, result!!, 0.001)
    }

    @Test
    fun testParseIndianNumberFormatting() {
        val result = RegexAmountParser.parseAmount("INR 1,20,500.75 credited to account")
        assertEquals(120500.75, result!!, 0.001)
    }

    @Test
    fun testParseFallbackDecimalWithoutCurrency() {
        val result = RegexAmountParser.parseAmount("Debited 420.50 for Swiggy order")
        assertEquals(420.50, result!!, 0.001)
    }

    @Test
    fun testParseInvalidText() {
        val result = RegexAmountParser.parseAmount("Hello world no amount here")
        assertNull(result)
    }
}
