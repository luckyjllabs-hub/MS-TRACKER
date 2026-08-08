package com.jllabs.moneylens.presentation.screens

import com.jllabs.moneylens.domain.models.SmsQueueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FilterInboxQueueTest {

    private fun item(id: String, timestamp: Long) = SmsQueueItem(
        id = id,
        rawText = "Rs.100 debited",
        bank = "HDFC",
        amountMinor = 10000,
        merchant = "Test",
        suggestedCategoryId = "cat-1",
        suggestedAccountId = "acc-1",
        timestamp = timestamp
    )

    private fun day(offsetDays: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return cal.timeInMillis
    }

    @Test
    fun `today yesterday week and month filters`() {
        val now = day(0, 15)
        val today = item("t", day(0, 10))
        val yesterday = item("y", day(-1, 10))
        val weekAgo = item("w", day(-5, 10))
        val earlierThisMonth = item("m", day(-2, 10))
        val old = item("o", day(-100, 10))
        val queue = listOf(today, yesterday, weekAgo, earlierThisMonth, old)

        assertEquals(listOf("t"), filterInboxQueue(queue, "Today", now).map { it.id })
        assertEquals(listOf("y"), filterInboxQueue(queue, "Yesterday", now).map { it.id })
        val weekIds = filterInboxQueue(queue, "This Week", now).map { it.id }
        assertTrue(weekIds.containsAll(listOf("t", "y", "w", "m")))
        val monthIds = filterInboxQueue(queue, "This Month", now).map { it.id }
        assertTrue(monthIds.containsAll(listOf("t", "y", "w", "m")))
        assertEquals(5, filterInboxQueue(queue, "All", now).size)
        assertTrue(filterInboxQueue(queue, "1 Year", now).map { it.id }.contains("o"))
        assertTrue(filterInboxQueue(queue, "Today", now).none { it.id == "o" })
    }

    @Test
    fun `seconds timestamp is normalized`() {
        val now = day(0, 15)
        val todaySecs = day(0, 10) / 1000L
        val queue = listOf(item("t", todaySecs))
        assertEquals(1, filterInboxQueue(queue, "Today", now).size)
    }

    @Test
    fun `zero timestamp treated as now for today`() {
        val now = day(0, 15)
        val queue = listOf(item("z", 0L))
        assertEquals(1, filterInboxQueue(queue, "Today", now).size)
    }
}
