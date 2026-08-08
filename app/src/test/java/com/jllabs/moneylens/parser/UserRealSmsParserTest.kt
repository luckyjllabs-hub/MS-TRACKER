package com.jllabs.moneylens.parser

import com.jllabs.moneylens.domain.models.MessageType
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.stage1.MessageTypeClassifier
import com.jllabs.moneylens.parser.stage2.FinancialFilter
import com.jllabs.moneylens.parser.stage4.DebitCreditDetector
import com.jllabs.moneylens.parser.stage5.AmountParser
import org.junit.Assert.*
import org.junit.Test

class UserRealSmsParserTest {

    @Test
    fun testUserRealSmsParsing() {
        val userSmsList = listOf(
            Triple(
                "JD-SBIUPI-S",
                "Dear User UPI LITE Top-up on SBI Rs.500.00 is successful. Ref No 620937108999. If not done by u, call 18001234 -SBI",
                Pair(TransactionType.EXPENSE, 50000L)
            ),
            Triple(
                "JD-QCAMZN-S",
                "Payment of Rs 501.00 using Apay balance is successful at A.in. Updated balance is Rs 972.82. If not u? call 18001200163 - SMS via Pine Labs",
                Pair(TransactionType.EXPENSE, 50100L)
            ),
            Triple(
                "JD-SBIUPI-S",
                "Dear User UPI LITE Top-up on SBI Rs.500.00 is successful. Ref No 619053132082. If not done by u, call 18001234 -SBI",
                Pair(TransactionType.EXPENSE, 50000L)
            ),
            Triple(
                "VM-SBIUPI-S",
                "Dear SBI User, your A/c X9642-credited by Rs.6000 on 20Jun26 transfer from CHANDRAMOULI SANCHI Ref No 617164914269 -SBI",
                Pair(TransactionType.INCOME, 600000L)
            ),
            Triple(
                "BG-QCAMZN-S",
                "Payment of Rs 199.00 using Apay balance is successful at A.in. Updated balance is Rs 1473.82. If not u? call 18001200163 - SMS via Pine Labs",
                Pair(TransactionType.EXPENSE, 19900L)
            ),
            Triple(
                "JD-SBIUPI-S",
                "Dear User UPI LITE Top-up on SBI Rs.4495.00 is successful. Ref No 652215140391. If not done by u, call 18001234 -SBI",
                Pair(TransactionType.EXPENSE, 449500L)
            ),
            Triple(
                "JD-SBIUPI-S",
                "Dear User UPI LITE Top-up on SBI Rs.500.00 is successful. Ref No 652240654534. If not done by u, call 18001234 -SBI",
                Pair(TransactionType.EXPENSE, 50000L)
            ),
            Triple(
                "JK-SBIUPI-S",
                "Your UPI-Mandate for Rs.500.00 is successfully created towards MADHURI  GORLA from A/c No: XXXXXX9642. UMN:f2c9e68eb5d447a793651346a7dfdfc0@okaxis. If not you, kindly report on 18001234. -SBI",
                Pair(TransactionType.EXPENSE, 50000L)
            ),
            Triple(
                "JD-SBIUPI-S",
                "Dear UPI user A/C X9642 debited by 40.00 on date 04Jun26 trf to NARASIMHAMURTHY Refno 615587721741 If not u? call-1800111109 for other services-18001234-SBI",
                Pair(TransactionType.EXPENSE, 4000L)
            ),
            Triple(
                "JK-SBIUPI-S",
                "Dear SBI User, your A/c X9642-credited by Rs.190 on 23Apr26 transfer from SANCHI SIVAKUMAR Ref No 313980659898 -SBI",
                Pair(TransactionType.INCOME, 19000L)
            ),
            Triple(
                "JK-SBIUPI-S",
                "Dear SBI User, your A/c X9642-credited by Rs.5000 on 22Apr26 transfer from CHANDRAMOULI SANCHI Ref No 611283029036 -SBI",
                Pair(TransactionType.INCOME, 500000L)
            ),
            Triple(
                "JK-SBIUPI-S",
                "Dear UPI user A/C X9642 debited by 30.00 on date 21Apr26 trf to Rajeev S K Refno 611153814407 If not u? call-1800111109 for other services-18001234-SBI",
                Pair(TransactionType.EXPENSE, 3000L)
            ),
            Triple(
                "JD-SBIUPI-S",
                "Dear UPI user A/C X9642 debited by 50.00 on date 19Apr26 trf to SAVITHA Refno 610962485624 If not u? call-1800111109 for other services-18001234-SBI",
                Pair(TransactionType.EXPENSE, 5000L)
            ),
            Triple(
                "JD-SBIUPI-S",
                "Dear UPI user A/C X9642 debited by 4298.00 on date 23Feb26 trf to P K DEPARTMENTAL Refno 605489759790 If not u? call-1800111109 for other services-18001234-SBI",
                Pair(TransactionType.EXPENSE, 429800L)
            )
        )

        for ((sender, body, expected) in userSmsList) {
            val passesFilter = FinancialFilter.passes(sender, body)
            assertTrue("Message from $sender should pass financial filter: $body", passesFilter)

            val debitCredit = DebitCreditDetector.detect(body)
            assertEquals("Transaction type mismatch for $body", expected.first, debitCredit.transactionType)

            val amountMinor = AmountParser.parseAmountMinor(body)
            assertNotNull("Amount should be parsed for $body", amountMinor)
            assertEquals("Amount mismatch for $body", expected.second, amountMinor)
        }
    }
}
