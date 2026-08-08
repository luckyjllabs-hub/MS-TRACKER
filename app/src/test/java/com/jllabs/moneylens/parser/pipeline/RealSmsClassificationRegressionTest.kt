package com.jllabs.moneylens.parser.pipeline

import com.jllabs.moneylens.domain.models.SmsProcessingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression against real SMS templates from the supplied raw CSV dumps.
 */
class RealSmsClassificationRegressionTest {

    @Test
    fun `EMI due is filtered`() {
        val result = SmsProcessingPipeline.process(
            sender = "AD-AXISBK-T",
            body = "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-07-26. Maintain adequate balance prior to the due date to avoid lien / bounce / penal charges.",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.FILTERED, result.status)
    }

    @Test
    fun `NEFT beneficiary confirmation is filtered`() {
        val result = SmsProcessingPipeline.process(
            sender = "JD-ICICIT-S",
            body = "ICICI BANK NEFT Transaction with reference number IN12621248301795 for Rs. 25000.00 has been credited to the beneficiary account on 31-07-2026 at 09:45:12",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.FILTERED, result.status)
    }

    @Test
    fun `EPFO contribution is filtered`() {
        val result = SmsProcessingPipeline.process(
            sender = "VA-EPFOHO-G",
            body = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 41,85,400/-. Contribution of Rs. 62,462/- for due month Jun-26 has been received.",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.FILTERED, result.status)
    }

    @Test
    fun `ICICI UPI Yashika Chicken is Food not Other`() {
        val result = SmsProcessingPipeline.process(
            sender = "AD-ICICIT-S",
            body = "Dear Customer, Acct XX346 debited for Rs 825.00 on 02-Aug-26; YASHIKA CHICKEN credited. UPI:621461530689. Call 18002662 for dispute.",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals("Yashika Chicken", result.merchant)
        assertEquals("cat-1", result.categoryId)
        assertNotEquals("cat-14", result.categoryId)
    }

    @Test
    fun `actual NEFT debit still succeeds`() {
        val result = SmsProcessingPipeline.process(
            sender = "JD-ICICIT-S",
            body = "ICICI Bank Acc XX346 debited Rs. 25,000.00 on 31-Jul-26 InfoBIL*NEFT*IN12.Avl Bal Rs. 13,12,032.84.To dispute call 18002662",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertTrue(result.amountMinor > 0)
    }

    @Test
    fun `MedPlus classifies Health`() {
        val result = SmsProcessingPipeline.process(
            sender = "AD-ICICIT-S",
            body = "Dear Customer, Acct XX346 debited for Rs 356.00 on 01-Aug-26; MEDPLUS CHANNAS credited. UPI:111.",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals("cat-6", result.categoryId)
    }

    @Test
    fun `Green City supermarket classifies Shopping`() {
        val result = SmsProcessingPipeline.process(
            sender = "AD-ICICIT-S",
            body = "Dear Customer, Acct XX346 debited for Rs 1240.00 on 01-Aug-26; GREEN CITY SUPER credited. UPI:222.",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals("cat-3", result.categoryId)
    }

    @Test
    fun `HDFC Received IMPS is income credit`() {
        val body = """
            Received!
            INR 12,181.00 in HDFC Bank A/c xx0328
            On 02-08-26
            For IMPS -AMITKUMAR- 621415757925
            Avl bal INR 10,09,205.48
        """.trimIndent()
        val result = SmsProcessingPipeline.process(
            sender = "VM-HDFCBK-S",
            body = body,
            timestamp = System.currentTimeMillis()
        )
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(com.jllabs.moneylens.domain.models.TransactionType.INCOME, result.transactionType)
        assertEquals(1218100L, result.amountMinor)
        assertTrue(result.merchant.contains("Amitkumar", ignoreCase = true))
    }
}
