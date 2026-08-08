package com.jllabs.moneylens.domain.accounts

import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.domain.reminders.ReminderExtractor
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
        assertTrue("Got: ${rows.map { it.displayName }}", rows.any {
            it.last4 == "5247" && it.isCreditCard && it.displayName.contains("HDFC")
        })
        assertTrue(rows.any { it.displayName == "SBI XX9653" })
        assertTrue(rows.any { it.displayName == "CANARA XX1640" })
        assertTrue("Got: ${rows.map { it.displayName }}", rows.any { it.displayName == "EPFO XX2889" })
        assertTrue(rows.size >= 5)
    }

    @Test
    fun `icici card XX0018 appears only under credit cards`() {
        val txs = listOf(
            tx(
                bank = "ICICI Bank",
                date = "2026-08-07",
                raw = "INR 50,000.00 spent using ICICI Bank Card XX0018 on 07-Aug-26 on BHIMA JEWELLERS. Avl Limit: INR 3,40,000.00."
            ),
            tx(
                bank = "ICICI Bank",
                date = "2026-07-31",
                // Payment SMS historically may land as AC if card hint missed
                raw = "Dear Customer, Payment of INR 60,788.80 has been received on your ICICI Bank Credit Card Account 4xxx0018 on 31-JUL-26.Thank you.",
                accountId = "sms-ac-icici-0018"
            ),
            tx(
                bank = "ICICI Bank",
                date = "2026-07-20",
                // Legacy row with blank raw classification path via last4 only
                raw = "ICICI Bank Acc XX0018 debited Rs. 100.00",
                last4 = "0018",
                accountId = "sms-ac-icici-0018"
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        val matches = rows.filter { it.last4 == "0018" }
        assertEquals("Got: ${rows.map { it.displayName }}", 1, matches.size)
        assertTrue(matches.single().isCreditCard)
        assertEquals("ICICI CC XX0018", matches.single().displayName)
        assertTrue(rows.none { it.last4 == "0018" && !it.isCreditCard })
    }

    @Test
    fun `epfo ledger amount uses contribution not passbook balance`() {
        val body = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 41,85,400/-. Contribution of Rs. 62,462/- for due month Jun-26 has been received."
        val bad = tx(
            bank = "EPFO",
            date = "2026-06-22",
            raw = body,
            last4 = "2889"
        ).copy(
            type = TransactionType.JUST_INFO,
            amountMinor = 4_185_400_00L,
            availableBalance = 4_185_400_00L,
            merchant = "Balance update"
        )
        assertEquals(62_462_00L, SmsAccountAggregator.displayAmountMinor(bad))
    }

    @Test
    fun `icici XX346 merges with ending XXXX2346`() {
        val txs = listOf(
            tx(
                bank = "ICICI Bank",
                date = "2026-08-07",
                raw = "Dear Customer, Acct XX346 is credited with Rs 275.00 on 07-Aug-26. Available balance is Rs 12,61,678.49"
            ),
            tx(
                bank = "ICICI Bank",
                date = "2026-07-10",
                raw = "Premium Debit Alert from bank A/C no ending with XXXX2346 for ICICIPru policy no A1063717."
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        val icici = rows.filter { it.shortBank == "ICICI" && !it.isCreditCard }
        assertEquals("Got: ${rows.map { it.displayName }}", 1, icici.size)
        assertEquals("2346", icici.single().last4)
        assertEquals("ICICI XX2346", icici.single().displayName)
    }

    @Test
    fun `axis loan EMI due creates finance account fingerprint`() {
        val body = "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-08-26. Maintain adequate balance prior to the due date to avoid lien / bounce / penal charges."
        val txs = listOf(
            tx(bank = "Axis Bank", date = "2026-08-05", raw = body, last4 = "8508").copy(
                type = TransactionType.JUST_INFO,
                amountMinor = 85_956_00L,
                merchant = "EMI due",
                source = "SMS_REMINDER"
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        assertTrue("Got: ${rows.map { it.displayName }}", rows.any {
            it.isLoanAccount && (it.displayName == "AXIS Loan XX8508" || it.last4 == "8508")
        })
        val rem = ReminderExtractor.extractFromTransactions(txs, todayIso = "2026-08-08")
        assertTrue(rem.any { it.title.contains("EMI", ignoreCase = true) })
    }

    @Test
    fun `icici FASTag groups under FASTag not Bal 240`() {
        val body = "Rs.135 paid at MERLAPAKA TOLL PLAZA for KA03MK9502 on 17-01-2026 09:12:16 with ICICI Bank FASTag. Bal Rs.240. Call 18002100104 for dispute"
        val txs = listOf(
            tx(bank = "ICICI Bank", date = "2026-01-17", raw = body, last4 = "240").copy(
                type = TransactionType.EXPENSE,
                amountMinor = 135_00L,
                availableBalance = 240_00L,
                merchant = "MERLAPAKA TOLL PLAZA"
            )
        )
        val rows = SmsAccountAggregator.derive(txs)
        assertTrue("Got: ${rows.map { it.displayName }}", rows.any { it.isFasTagAccount })
        assertTrue(rows.none { it.last4 == "240" && !it.isFasTagAccount })
        val ft = rows.first { it.isFasTagAccount }
        assertEquals("9502", ft.last4)
        assertTrue(ft.displayName.contains("FASTag", ignoreCase = true))
        assertEquals(240_00L, ft.balanceMinor)
    }

    @Test
    fun `sbi accounts 9653 2985 9642 appear when sms present`() {
        val txs = listOf(
            tx(
                bank = "SBI",
                date = "2026-06-08",
                raw = "Dear Customer, Your A/C XXXXX079653 has a credit by Cheque of Rs 19,20,000.00 on 08/06/26. Avl Bal Rs 24,27,705.76.-SBI"
            ).copy(type = TransactionType.INCOME, amountMinor = 19_20_000_00L, availableBalance = 24_27_705_76L),
            tx(
                bank = "SBI",
                date = "2024-05-18",
                raw = "Your A/C XXXXX082985 Credited INR 2,000.00 on 18/05/24. Avl Bal INR 79,428.00-SBI"
            ).copy(type = TransactionType.INCOME, amountMinor = 2_000_00L, availableBalance = 79_428_00L),
            tx(
                bank = "SBI",
                date = "2024-01-12",
                raw = "Your A/C XXXXX079642 Credited INR 2,00,000.00 on 12/01/24. Avl Bal INR 5,50,761.10-SBI"
            ).copy(type = TransactionType.INCOME, amountMinor = 2_00_000_00L, availableBalance = 5_50_761_10L)
        )
        val rows = SmsAccountAggregator.derive(txs)
        assertTrue(rows.any { it.displayName == "SBI XX9653" })
        assertTrue(rows.any { it.displayName == "SBI XX2985" })
        assertTrue(rows.any { it.displayName == "SBI XX9642" })
    }

    private fun tx(
        bank: String,
        date: String,
        raw: String,
        createdAt: Long = 1L,
        last4: String = "",
        accountId: String = "acc-1"
    ) = Transaction(
        id = "$date-$createdAt-${raw.hashCode()}",
        type = TransactionType.EXPENSE,
        amountMinor = 10000,
        accountId = accountId,
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
