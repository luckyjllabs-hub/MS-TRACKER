package com.example.mstrackerapp.parser.stage5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountParserTest {

    @Test
    fun `icici acct XX346 three digits`() {
        val body = "ICICI Bank Acct XX346 debited for Rs 197.00 on 07-Aug-26; MEDPLUS CHANNAS credited. UPI:621948393659. Call 18002662 for dispute. SMS BLOCK 346 to 9215676766."
        assertEquals("346", AccountParser.extractAccountLast4(body))
    }

    @Test
    fun `icici dear customer acct XX346`() {
        val body = "Dear Customer, Acct XX346 debited for Rs 825.00 on 02-Aug-26; YASHIKA CHICKEN credited. UPI:621461530689. Call 18002662 for dispute."
        assertEquals("346", AccountParser.extractAccountLast4(body))
    }

    @Test
    fun `icici Acc XX346 without t`() {
        val body = "ICICI Bank Acc XX346 debited Rs. 25,000.00 on 31-Jul-26 InfoBIL*NEFT*IN12.Avl Bal Rs. 13,12,032.84."
        assertEquals("346", AccountParser.extractAccountLast4(body))
    }

    @Test
    fun `icici Account XX932 three digits`() {
        val body = "ICICI Bank Account XX932 credited:Rs.456666.00 on 31-Jul-26. Info NEFT-HSBCN21271399500-SAMSUN. Available Balance is Rs. 5677788"
        assertEquals("932", AccountParser.extractAccountLast4(body))
    }

    @Test
    fun `hdfc A_c XX0328 four digits still works`() {
        val body = "Credit Alert! Rs.7158.00 credited to HDFC Bank A/c XX0328 on 01-08-26 from VPA r.rajeshbarath-2@oksbi"
        assertEquals("0328", AccountParser.extractAccountLast4(body))
    }

    @Test
    fun `does not pick BLOCK or helpline digits`() {
        val body = "ICICI Bank Acct XX346 debited for Rs 197.00. Call 18002662 for dispute. SMS BLOCK 346 to 9215676766."
        assertEquals("346", AccountParser.extractAccountLast4(body))
        assertTrue(AccountParser.extractAccountLast4(body) != "2662")
        assertTrue(AccountParser.extractAccountLast4(body) != "1800")
    }

    @Test
    fun `epfo bgbng masked account`() {
        val body = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 41,85,400/-. Contribution of Rs. 62,462/- for due month Jun-26 has been received."
        assertEquals("2889", AccountParser.extractAccountLast4(body))
        assertEquals(4_185_400_00L, BalanceParser.extractBalanceMinor(body))
    }

    @Test
    fun `card ending without in keyword`() {
        val body = "Thank you for using your HDFC Bank Card ending 5247 for Rs.500.00 on 15-Nov-25. Avl bal INR 23636.20"
        assertEquals("5247", AccountParser.extractAccountOrCardLast4(body))
    }
}
