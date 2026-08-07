package com.example.mstrackerapp.domain.accounts

import com.example.mstrackerapp.domain.models.Transaction
import com.example.mstrackerapp.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAccountAggregatorTest {

    @Test
    fun `groups by bank and last4 and picks latest sms balance`() {
        val txs = listOf(
            tx(
                bank = "HDFC Bank",
                date = "2026-08-01",
                raw = "Credit Alert! Rs.7158.00 credited to HDFC Bank A/c XX0328 on 01-08-26 from VPA r.rajeshbarath-2@oksbi"
            ),
            tx(
                bank = "HDFC Bank",
                date = "2026-08-05",
                createdAt = 200L,
                raw = "Update! INR 100 deposited in HDFC Bank A/c XX0328. Avl bal INR 11,26,187.48"
            ),
            tx(
                bank = "ICICI Bank",
                date = "2026-08-07",
                raw = "Dear Customer, Acct XX2346 is credited with Rs 275.00. Available balance is Rs 12,61,678.49"
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        assertEquals(2, rows.size)
        val hdfc = rows.first { it.last4 == "0328" }
        assertEquals("HDFC XX0328", hdfc.displayName)
        assertEquals(1_126_187_48L, hdfc.balanceMinor)
        assertEquals("2026-08-05", hdfc.balanceDate)
        val icici = rows.first { it.last4 == "2346" }
        assertEquals(1_261_678_49L, icici.balanceMinor)
    }

    @Test
    fun `icici XX346 debit sms appears as finance account even when accountLast4 blank`() {
        val body = "ICICI Bank Acct XX346 debited for Rs 197.00 on 07-Aug-26; MEDPLUS CHANNAS credited. UPI:621948393659. Call 18002662 for dispute. SMS BLOCK 346 to 9215676766."
        val txs = listOf(
            tx(bank = "ICICI Bank", date = "2026-08-07", raw = body, last4 = ""),
            tx(
                bank = "ICICI Bank",
                date = "2026-08-06",
                raw = "Dear Customer, Acct XX346 debited for Rs 825.00 on 02-Aug-26; YASHIKA CHICKEN credited. UPI:621461530689.",
                last4 = ""
            ),
            tx(
                bank = "ICICI Bank",
                date = "2026-07-31",
                createdAt = 50L,
                raw = "ICICI Bank Acc XX346 debited Rs. 25,000.00 on 31-Jul-26 InfoBIL*NEFT*IN12.Avl Bal Rs. 13,12,032.84.",
                last4 = ""
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        assertEquals(1, rows.size)
        val icici = rows.single()
        assertEquals("346", icici.last4)
        assertEquals("ICICI XX346", icici.displayName)
        assertEquals(3, icici.txCount)
        assertEquals(1_312_032_84L, icici.balanceMinor)
        assertEquals("2026-07-31", icici.balanceDate)

        val ledger = SmsAccountAggregator.transactionsFor(icici, txs)
        assertEquals(3, ledger.size)
    }

    @Test
    fun `older month accounts still appear when mixed with current month`() {
        val txs = listOf(
            tx(
                bank = "HDFC Bank",
                date = "2026-08-05",
                raw = "Update! INR 100 deposited in HDFC Bank A/c XX0328. Avl bal INR 11,26,187.48"
            ),
            tx(
                bank = "HDFC Bank",
                date = "2025-11-15",
                raw = "Spent Rs.100 on HDFC Bank Card ending 5247. Avl bal INR 23636.20"
            ),
            tx(
                bank = "SBI",
                date = "2026-04-03",
                raw = "Dear Customer, Your a/c no. XXXXXXXX9653 is credited by Rs.1000.00 on 03-04-26. Available balance is Rs. 291671.76-SBI"
            ),
            tx(
                bank = "Canara Bank",
                date = "2026-03-30",
                raw = "Canara Bank A/c XX1640 credited Rs 500. Avl Bal Rs 10936"
            ),
            tx(
                bank = "Unknown Bank",
                date = "2026-04-13",
                raw = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 30,39,163/-."
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        assertTrue(rows.any { it.displayName == "HDFC XX0328" })
        assertTrue(rows.any { it.displayName == "HDFC XX5247" })
        assertTrue(rows.any { it.displayName == "SBI XX9653" })
        assertTrue(rows.any { it.displayName == "CANARA XX1640" })
        assertTrue("Got: ${rows.map { it.displayName }}", rows.any { it.displayName == "EPFO XX2889" })
        assertTrue(rows.size >= 5)
    }

    private fun tx(
        bank: String,
        date: String,
        raw: String,
        createdAt: Long = 1L,
        last4: String = ""
    ) = Transaction(
        id = "$date-$createdAt-${raw.hashCode()}",
        type = TransactionType.EXPENSE,
        amountMinor = 10000,
        accountId = "acc-1",
        categoryId = "cat-14",
        merchant = "Test",
        date = date,
        time = "10:00",
        bankName = bank,
        accountLast4 = last4,
        rawSms = raw,
        createdAt = createdAt
    )
}
