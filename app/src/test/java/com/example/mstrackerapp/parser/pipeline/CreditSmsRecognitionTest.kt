package com.example.mstrackerapp.parser.pipeline

import com.example.mstrackerapp.domain.models.SmsProcessingStatus
import com.example.mstrackerapp.domain.models.TransactionType
import com.example.mstrackerapp.parser.stage3.NonTransactionAlertFilter
import com.example.mstrackerapp.parser.stage4.DebitCreditDetector
import com.example.mstrackerapp.parser.stage5.AmountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression: on some devices every credit SMS was dropped because
 * "Available balance is…" tripped INFO/balance filters before own-account credit detection.
 */
class CreditSmsRecognitionTest {

    private val indianBank = "Your A/c *1737 is credited with Rs.15000.00 on 31-07-26 by SHWETANK CHOUDHARY. RRN 621217694961. Available balance is Rs. 18025.20 - Indian Bank"
    private val sbiImps = "Dear Customer, Your a/c no. XXXXXXXX6916 is credited by Rs.85000.00 on 31-07-26 by a/c linked to mobile 9XXXXXX090-SHWETANK C (IMPS Ref# 621209397395)-SBI"
    private val iciciNeft = "ICICI Bank Account XX932 credited:Rs.456666.00 on 31-Jul-26. Info NEFT-HSBCN21271399500-SAMSUN. Available Balance is Rs. 5677788"
    private val sbiTransferFrom = "Dear SBI User, your A/c X9642-credited by Rs.6000 on 20Jun26 transfer from CHANDRAMOULI SANCHI Ref No 617164914269 -SBI"
    private val creditedRsWithBalance = "Your A/c XX1737 is credited Rs.15000.00 on 31-07-26. Available balance is Rs. 18025.20"

    @Test
    fun `indian bank credit with available balance is income`() {
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(indianBank))
        val dc = DebitCreditDetector.detect(indianBank)
        assertEquals(TransactionType.INCOME, dc.transactionType)
        val result = SmsProcessingPipeline.process("VM-IDIBNK-S", indianBank, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(1_500_000L, result.amountMinor)
        assertTrue(result.merchant.contains("SHWETANK", ignoreCase = true))
    }

    @Test
    fun `sbi imps credit with masked a c no is income`() {
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(sbiImps))
        val result = SmsProcessingPipeline.process("VM-SBIINB-S", sbiImps, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(8_500_000L, result.amountMinor)
    }

    @Test
    fun `icici credited colon amount with available balance is income`() {
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(iciciNeft))
        assertEquals(456666.0, AmountParser.parseRupees(iciciNeft)!!, 0.01)
        val result = SmsProcessingPipeline.process("AX-ICICIT-S", iciciNeft, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(45_666_600L, result.amountMinor)
    }

    @Test
    fun `sbi transfer-from credit is income`() {
        val result = SmsProcessingPipeline.process("VM-SBIUPI-S", sbiTransferFrom, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(600_000L, result.amountMinor)
    }

    @Test
    fun `credited Rs without with-by still not balance-only when available balance present`() {
        // Systemic failure mode: available-balance credits that omit "with/by"
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(creditedRsWithBalance))
        val dc = DebitCreditDetector.detect(creditedRsWithBalance)
        assertEquals(TransactionType.INCOME, dc.transactionType)
        val result = SmsProcessingPipeline.process("VM-IDIBNK-S", creditedRsWithBalance, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.INCOME, result.transactionType)
    }

    @Test
    fun `beneficiary credit confirmation still filtered`() {
        val body = "ICICI BANK NEFT Transaction with reference number IN12621248301795 for Rs. 25000.00 has been credited to the beneficiary account on 31-07-2026"
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
        val result = SmsProcessingPipeline.process("AX-ICICIT-S", body, 1L)
        assertEquals(SmsProcessingStatus.FILTERED, result.status)
    }

    @Test
    fun `upi debit with payee credited is still expense`() {
        val body = "Dear Customer, Acct XX346 debited for Rs 825.00 on 02-Aug-26; YASHIKA CHICKEN credited. UPI:621461530689."
        val dc = DebitCreditDetector.detect(body)
        assertEquals(TransactionType.EXPENSE, dc.transactionType)
        val result = SmsProcessingPipeline.process("AX-ICICIT-S", body, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
    }

    @Test
    fun `pure balance alert without credit verb is filtered`() {
        val body = "Available balance is Rs. 18025.20 in your a/c. - Indian Bank"
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
        val result = SmsProcessingPipeline.process("VM-IDIBNK-S", body, 1L)
        assertEquals(SmsProcessingStatus.FILTERED, result.status)
    }
}
