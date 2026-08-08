package com.jllabs.moneylens.parser.stage5

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
    fun `parses EPFO contribution not passbook balance`() {
        val body = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 41,85,400/-. Contribution of Rs. 62,462/- for due month Jun-26 has been received."
        assertEquals(62_462_00L, AmountParser.parseAmountMinor(body))
        assertEquals(62_462_00L, AmountParser.parseEpfoContributionMinor(body))
        assertEquals(4_185_400_00L, BalanceParser.extractDisplayBalanceMinor(body))
    }
}
