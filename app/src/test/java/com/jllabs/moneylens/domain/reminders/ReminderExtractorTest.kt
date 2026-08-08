package com.jllabs.moneylens.domain.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderExtractorTest {

    @Test
    fun `expired coupon with year 2023 is dropped`() {
        val body = "Your coupon CODE123 is valid till 15-08-2023. Shop now!"
        val rem = ReminderExtractor.fromSms(body, "1", "2023-08-01", "HDFC")!!
        assertTrue(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
    }

    @Test
    fun `future due is kept`() {
        val body = "EMI of INR 5000 is due on 15-09-2026. Maintain balance."
        val rem = ReminderExtractor.fromSms(body, "2", "2026-08-01", "Axis Bank")!!
        assertFalse(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
        assertEquals("2026-09-15", rem.dueDateIso)
    }

    @Test
    fun `due within 10 day grace is kept`() {
        val body = "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 01-08-26."
        val rem = ReminderExtractor.fromSms(body, "2b", "2026-07-25", "Axis Bank")!!
        assertEquals("2026-08-01", rem.dueDateIso)
        // 7 days past due — still within grace
        assertFalse(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
    }

    @Test
    fun `due older than 10 day grace is expired`() {
        val body = "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-07-26."
        val rem = ReminderExtractor.fromSms(body, "2c", "2026-07-05", "Axis Bank")!!
        assertEquals("2026-07-10", rem.dueDateIso)
        // 29 days past due
        assertTrue(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
    }

    @Test
    fun `due without year uses sms year and rolls forward if needed`() {
        val body = "Payment due on 10 Aug. Please pay."
        val rem = ReminderExtractor.fromSms(body, "3", "2026-08-01", "ICICI")!!
        assertEquals("2026-08-10", rem.dueDateIso)
        assertFalse(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
    }

    @Test
    fun `past due without year from old sms expires`() {
        val body = "Bill is due on 05 Aug"
        val rem = ReminderExtractor.fromSms(body, "4", "2025-08-01", "SBI")!!
        // 05 Aug 2025 is before today 2026
        assertTrue(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
    }

    @Test
    fun `old sms without due date hidden after 90 days`() {
        val body = "Exclusive offer expires soon! Use code SAVE20"
        val rem = ReminderExtractor.fromSms(body, "5", "2023-01-01", "Paytm")!!
        assertNull(rem.dueDateIso)
        assertTrue(ReminderExtractor.isExpired(rem, todayIso = "2026-08-08"))
    }

    @Test
    fun `emi preferred over coupon-like wording`() {
        val body = "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-08-26. Avoid penal charges."
        val rem = ReminderExtractor.fromSms(body, "6", "2026-08-05", "Axis Bank")!!
        assertEquals(ReminderKind.PAYMENT_DUE, rem.kind)
        assertEquals("EMI due", rem.title)
    }

    @Test
    fun `refund credited with revised total due is not a reminder`() {
        val body = "URBANCOMPANY refund of Rs 298.00 credited to ICICI Bank Credit Card XX0000 on 15-JUL-26. Revised total due Rs 57,065.11, minimum due Rs 2,572.00"
        assertNull(ReminderExtractor.fromSms(body, "7", "2026-07-16", "ICICI"))
    }

    @Test
    fun `reminders order upcoming future dues before overdue`() {
        val txs = listOf(
            com.jllabs.moneylens.domain.models.Transaction(
                id = "a",
                type = com.jllabs.moneylens.domain.models.TransactionType.JUST_INFO,
                amountMinor = 0,
                accountId = "acc-1",
                categoryId = "cat-14",
                merchant = "EMI due",
                date = "2026-07-25",
                time = "10:00",
                note = "",
                bankName = "Axis",
                rawSms = "EMI of INR 100 for Axis Bank Loan A/c XX8508 is due on 01-08-26."
            ),
            com.jllabs.moneylens.domain.models.Transaction(
                id = "b",
                type = com.jllabs.moneylens.domain.models.TransactionType.JUST_INFO,
                amountMinor = 0,
                accountId = "acc-1",
                categoryId = "cat-14",
                merchant = "EMI due",
                date = "2026-08-01",
                time = "10:00",
                note = "",
                bankName = "Axis",
                rawSms = "EMI of INR 200 for Axis Bank Loan A/c XX8509 is due on 15-09-26."
            )
        )
        val rem = ReminderExtractor.extractFromTransactions(txs, todayIso = "2026-08-08")
        assertTrue(rem.size >= 2)
        assertEquals("2026-09-15", rem.first().dueDateIso)
    }
}
