package com.example.mstrackerapp.parser.stage5

import org.junit.Assert.*
import org.junit.Test

class AmountParserTest {

    @Test
    fun `parses INR prefix amount`() {
        assertEquals(150000L, AmountParser.parseAmountMinor("INR 1,500.00 debited"))
    }

    @Test
    fun `parses rupee symbol`() {
        assertEquals(50000L, AmountParser.parseAmountMinor("₹500.00 paid to Swiggy"))
    }

    @Test
    fun `parses verb-first amount`() {
        assertEquals(120050L, AmountParser.parseAmountMinor("debited Rs 1,200.50 from account"))
    }

    @Test
    fun `returns null for no amount`() {
        assertNull(AmountParser.parseAmountMinor("Your OTP is 1234"))
    }

    @Test
    fun `parses large amount with commas`() {
        assertEquals(5000000L, AmountParser.parseAmountMinor("Rs.50,000.00 credited to your account"))
    }
}
