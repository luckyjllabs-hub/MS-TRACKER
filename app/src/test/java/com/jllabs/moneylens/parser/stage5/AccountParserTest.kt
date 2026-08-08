package com.jllabs.moneylens.parser.stage5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountParserTest {

    @Test
    fun `icici credit card XX0018`() {
        val body = "INR 50,000.00 spent using ICICI Bank Card XX0018 on 07-Aug-26 on BHIMA JEWELLERS. Avl Limit: INR 3,40,000.00."
        assertEquals("0018", AccountParser.extractAccountOrCardLast4(body))
        assertTrue(AccountParser.isCreditCardSms(body))
        assertEquals(3_40_000_00L, BalanceParser.extractDisplayBalanceMinor(body))
    }

    @Test
    fun `icici credit card account 4xxx0018`() {
        val body = "Dear Customer, Payment of INR 60,788.80 has been received on your ICICI Bank Credit Card Account 4xxx0018 on 31-JUL-26.Thank you."
        assertEquals("0018", AccountParser.extractAccountOrCardLast4(body))
        assertTrue(AccountParser.isCreditCardSms(body))
    }

    @Test
    fun `hdfc A_C single x5247`() {
        val body = "Sent Rs.105.00\nFrom HDFC Bank A/C x5247\nTo GUNDARAYA\nOn 03/04/25"
        assertEquals("5247", AccountParser.extractAccountOrCardLast4(body))
        assertFalse(AccountParser.isCreditCardSms(body))
    }

    @Test
    fun `hdfc XX5247 and XXXX1873`() {
        assertEquals("5247", AccountParser.extractAccountOrCardLast4(
            "Update! INR 5,309.00 deposited in HDFC Bank A/c XX5247 on 30-SEP-24.Avl bal INR 10,23,853.90."
        ))
        assertEquals("1873", AccountParser.extractAccountOrCardLast4(
            "Amt Deducted! Rs.100.00 from your HDFC Bank A/c XXXX1873 for Money Transfer via HDFC Bank Online Banking."
        ))
    }

    @Test
    fun `canara XXXX1640`() {
        val body = "An amount of INR 48.00 has been CREDITED to your account XXXX1640 on 28/03/2025 towards interest. Total Avail.bal INR 10,936.00. - Canara Bank"
        assertEquals("1640", AccountParser.extractAccountOrCardLast4(body))
        assertEquals(10_936_00L, BalanceParser.extractDisplayBalanceMinor(body))
    }

    @Test
    fun `sbi long masked a_c takes last4`() {
        assertEquals("2985", AccountParser.extractAccountOrCardLast4(
            "Your A/C XXXXX082985 Credited INR 2,000.00 on 18/05/24. Avl Bal INR 79,428.00-SBI"
        ))
        assertEquals("9642", AccountParser.extractAccountOrCardLast4(
            "Dear Customer, Your a/c no. XXXXXXXX9642 is debited for Rs.200000.00 on 09-10-23"
        ))
        assertEquals("9642", AccountParser.extractAccountOrCardLast4(
            "Your A/C XXXXX079642 Credited INR 2,00,000.00 on 12/01/24"
        ))
    }

    @Test
    fun `icici acct XX346 three digits still works`() {
        val body = "ICICI Bank Acct XX346 debited for Rs 197.00 on 07-Aug-26; MEDPLUS CHANNAS credited."
        assertEquals("346", AccountParser.extractAccountLast4(body))
        assertFalse(AccountParser.isCreditCardSms(body))
    }

    @Test
    fun `icici FASTag does not use Bal as account last4`() {
        val body = "Rs.135 paid at MERLAPAKA TOLL PLAZA for KA03MK9502 on 17-01-2026 09:12:16 with ICICI Bank FASTag. Bal Rs.240. Call 18002100104 for dispute"
        assertTrue(AccountParser.isFasTagSms(body))
        assertEquals("9502", AccountParser.extractAccountOrCardLast4(body))
        assertFalse(AccountParser.isCreditCardSms(body))
        assertEquals(240_00L, BalanceParser.extractDisplayBalanceMinor(body))
    }

    @Test
    fun `fasTag recharge without vehicle uses FTAG id`() {
        val body = "Recharge of Rs. 200.00 for ICICI Bank FASTag is processed on 30/03/2026. Balance is Rs. 395.00 ."
        assertTrue(AccountParser.isFasTagSms(body))
        assertEquals("FTAG", AccountParser.extractAccountOrCardLast4(body))
    }

    @Test
    fun `sbi long masks 9642 2985 9653`() {
        assertEquals("9642", AccountParser.extractAccountOrCardLast4(
            "Your A/C XXXXX079642 Credited INR 2,00,000.00 on 12/01/24 -Deposited by Cash by SELF. Avl Bal INR 5,50,761.10-SBI"
        ))
        assertEquals("2985", AccountParser.extractAccountOrCardLast4(
            "Your A/C XXXXX082985 Credited INR 2,000.00 on 18/05/24. Avl Bal INR 79,428.00-SBI"
        ))
        assertEquals("9653", AccountParser.extractAccountOrCardLast4(
            "Dear Customer, Your A/C XXXXX079653 has a credit by Cheque of Rs 19,20,000.00 on 08/06/26. Avl Bal Rs 24,27,705.76.-SBI"
        ))
        assertEquals("9653", AccountParser.extractAccountOrCardLast4(
            "Dear Customer, INR 5,00,000.00 credited to your A/c No XX9653 on 04/06/2026 through RTGS"
        ))
    }

    @Test
    fun `credit card facility on savings a_c is not credit card`() {
        val body = "Important Info: A Lifetime-Free Credit Card facility has been enabled on your HDFC Bank A/c xx5247. Check: https://1.hdfc.bank.in/"
        assertEquals("5247", AccountParser.extractAccountOrCardLast4(body))
        assertFalse(AccountParser.isCreditCardSms(body))
    }

    @Test
    fun `icici pru policy due has no last4`() {
        val body =
            "ICICIPru policy A1063717 is due. Premium of Rs. 5092 will be deducted on due date 13-Aug-26 as per standing instructions."
        assertTrue(AccountParser.isInsuranceOrPolicySms(body))
        assertEquals("", AccountParser.extractAccountOrCardLast4(body))
    }

    @Test
    fun `axis loan a_c and ending with XXXX2346`() {
        assertEquals("8508", AccountParser.extractAccountOrCardLast4(
            "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-08-26."
        ))
        assertTrue(AccountParser.isLoanAccountSms(
            "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-08-26."
        ))
        assertEquals("2346", AccountParser.extractAccountOrCardLast4(
            "Premium Debit Alert from bank A/C no ending with XXXX2346 for ICICIPru policy"
        ))
        assertEquals("8970", AccountParser.extractAccountOrCardLast4(
            "ICICI Bank Home Loan XX8970 has unclaimed balance of Rs 0.01."
        ))
    }

    @Test
    fun `epfo bgbng masked account`() {
        val body = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 41,85,400/-."
        assertEquals("2889", AccountParser.extractAccountLast4(body))
        assertEquals(4_185_400_00L, BalanceParser.extractBalanceMinor(body))
    }

    @Test
    fun `loan available balance of INR`() {
        val body = "Dear Customer the available balance of INR 0.01 in your ICICI Bank Loan Account XX8970 would be transferred"
        assertEquals(1L, BalanceParser.extractDisplayBalanceMinor(body))
        assertEquals("8970", AccountParser.extractAccountOrCardLast4(body))
    }
}
