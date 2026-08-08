package com.jllabs.moneylens.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun testNegativeMoneyFormattingSingleMinus() {
        val formatted = Money.format(-318500L)
        assertEquals("-₹3,185.00", formatted)
    }

    @Test
    fun testPositiveMoneyFormatting() {
        val formatted = Money.format(50000L)
        assertEquals("₹500.00", formatted)
    }

    @Test
    fun testZeroMoneyFormatting() {
        val formatted = Money.format(0L)
        assertEquals("₹0.00", formatted)
    }

    @Test
    fun testAbsoluteMoneyFormatting() {
        val formatted = Money.format(-318500L, absolute = true)
        assertEquals("₹3,185.00", formatted)
    }
}
