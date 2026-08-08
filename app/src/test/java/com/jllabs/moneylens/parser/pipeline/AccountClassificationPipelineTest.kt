package com.jllabs.moneylens.parser.pipeline

import com.jllabs.moneylens.domain.accounts.SmsAccountAggregator
import com.jllabs.moneylens.domain.models.SmsProcessingStatus
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.stage5.AccountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountClassificationPipelineTest {

    @Test
    fun `icici FASTag toll uses vehicle last4 not Bal 240`() {
        val body =
            "Rs.135 paid at MERLAPAKA TOLL PLAZA for KA03MK9502 on 17-01-2026 09:12:16 with ICICI Bank FASTag. Bal Rs.240. Call 18002100104 for dispute"
        val result = SmsProcessingPipeline.process("AX-ICICIT-S", body, 1L)
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(135_00L, result.amountMinor)
        assertEquals("9502", result.accountLast4)
        assertEquals(240_00L, result.availableBalanceMinor)
        assertTrue(AccountParser.isFasTagSms(body))
        assertFalse(result.accountLast4 == "240")

        val accountId = SmsAccountAggregator.accountIdFor(
            result.bank, result.accountLast4, isFasTag = true
        )
        assertTrue(accountId.contains("-ft-"))
        assertTrue(accountId.endsWith("-9502"))
    }

    @Test
    fun `sbi XX9653 credit by cheque is income`() {
        val body =
            "Dear Customer, Your A/C XXXXX079653 has a credit by Cheque of Rs 19,20,000.00 on 08/06/26. Avl Bal Rs 24,27,705.76.-SBI"
        val result = SmsProcessingPipeline.process("VM-SBIINB-S", body, 1L)
        assertEquals(
            "filterReason=${result.filterReason} type=${result.transactionType} amt=${result.amountMinor}",
            SmsProcessingStatus.SUCCESS,
            result.status
        )
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals("9653", result.accountLast4)
        assertEquals(19_20_000_00L, result.amountMinor)
    }

    @Test
    fun `sbi XX2985 and XX9642 credits parse last4`() {
        val a = SmsProcessingPipeline.process(
            "VM-SBIINB-S",
            "Your A/C XXXXX082985 Credited INR 2,000.00 on 18/05/24. Avl Bal INR 79,428.00-SBI",
            1L
        )
        assertEquals(SmsProcessingStatus.SUCCESS, a.status)
        assertEquals("2985", a.accountLast4)
        assertEquals(TransactionType.INCOME, a.transactionType)

        val b = SmsProcessingPipeline.process(
            "VM-SBIUPI-S",
            "Dear SBI User, your A/c X9642-credited by Rs.6000 on 20Jun26 transfer from CHANDRAMOULI SANCHI Ref No 617164914269 -SBI",
            1L
        )
        assertEquals(SmsProcessingStatus.SUCCESS, b.status)
        assertEquals("9642", b.accountLast4)
        assertEquals(TransactionType.INCOME, b.transactionType)
    }

    @Test
    fun `sbi long mask 079642 normalizes to 9642`() {
        val result = SmsProcessingPipeline.process(
            "VM-SBIINB-S",
            "Your A/C XXXXX079642 Credited INR 2,00,000.00 on 12/01/24. Avl Bal INR 5,50,761.10-SBI",
            1L
        )
        assertEquals(SmsProcessingStatus.SUCCESS, result.status)
        assertEquals("9642", result.accountLast4)
    }
}
