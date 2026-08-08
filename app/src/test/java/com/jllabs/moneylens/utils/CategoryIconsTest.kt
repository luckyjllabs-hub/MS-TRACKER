package com.jllabs.moneylens.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconsTest {
    @Test
    fun `mojibake icon falls back to letter`() {
        assertEquals("R", CategoryIcons.display("âœ¨", "Rent"))
        assertEquals("L", CategoryIcons.display("âœ¨", "Loan"))
        assertTrue(CategoryIcons.isBroken("âœ¨"))
    }

    @Test
    fun `blank icon uses letter`() {
        assertEquals("N", CategoryIcons.sanitizeForStorage("", "New Category"))
    }
}
