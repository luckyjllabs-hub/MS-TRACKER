package com.jllabs.moneylens.parser.stage5

import com.jllabs.moneylens.domain.accounts.SmsAccountAggregator
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.domain.reminders.ReminderExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IciciPruAccountParserTest {

    private val policyDue =
        "ICICIPru policy A1063717 is due. Premium of Rs. 5092 will be deducted on due date 13-Aug-26 " +
            "as per standing instructions. You may choose to pay in advance now at " +
            "https://s.ipru.co/ICICIP/08o21vxe Click https://s.ipru.co/ICICIP/mws1p4cz to view details " +
            "or download premium notice."

    @Test
    fun `policy due sms has no account last4`() {
        assertTrue(AccountParser.isInsuranceOrPolicySms(policyDue))
        assertEquals("", AccountParser.extractAccountOrCardLast4(policyDue))
        assertEquals("ICICI Pru", BankParser.extractBank("AX-ICICIP-S", policyDue))
    }

    @Test
    fun `policy due does not create finance account or attach to ICICI XX7356`() {
        val policyTx = Transaction(
            id = "p1",
            type = TransactionType.JUST_INFO,
            amountMinor = 5_092_00L,
            accountId = "acc-1",
            categoryId = "cat-rem",
            merchant = "Premium due",
            date = "2026-08-08",
            time = "10:00",
            bankName = "ICICI Pru",
            accountLast4 = "7356",
            source = "SMS_REMINDER",
            confidence = "INFO",
            isReviewed = true,
            rawSms = policyDue
        )
        val bankTx = Transaction(
            id = "b1",
            type = TransactionType.EXPENSE,
            amountMinor = 100_00L,
            accountId = "acc-1",
            categoryId = "cat-14",
            merchant = "UPI",
            date = "2026-08-07",
            time = "09:00",
            bankName = "ICICI Bank",
            accountLast4 = "7356",
            source = "SMS",
            confidence = "HIGH",
            isReviewed = true,
            rawSms = "ICICI Bank Acct XX7356 debited with Rs 100.00 on 07-Aug-26."
        )
        val rows = SmsAccountAggregator.derive(listOf(policyTx, bankTx))
        assertFalse(rows.any { it.last4 == "3717" || it.displayName.contains("Pru", true) })
        assertEquals(1, rows.count { it.shortBank == "ICICI" && it.last4 == "7356" && !it.isCreditCard })

        assertFalse(
            ReminderExtractor.matchesAccount(policyDue, accountKeyLast4 = "7356", bankShort = "ICICI")
        )
        val rem = ReminderExtractor.forAccount(
            "7356",
            "ICICI",
            listOf(policyTx, bankTx),
            todayIso = "2026-08-08"
        )
        assertTrue(rem.none { it.rawSms.contains("ICICIPru") })
        assertTrue(
            ReminderExtractor.extractFromTransactions(listOf(policyTx), todayIso = "2026-08-08")
                .any { it.title.contains("Premium", ignoreCase = true) }
        )
    }
}
