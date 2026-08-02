package com.example.mstrackerapp.parser.stage3

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NonTransactionAlertFilterTest {

    @Test
    fun `EMI due reminder is non-transaction`() {
        val body = "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-07-26. Maintain adequate balance prior to the due date to avoid lien / bounce / penal charges."
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertFalse(SuccessDetector.isSuccessful(body))
    }

    @Test
    fun `premium due standing instruction is non-transaction`() {
        val body = "ICICIPru policy A1063717 is due. Premium of Rs. 5092 will be deducted on due date 13-Aug-26 as per standing instructions."
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
    }

    @Test
    fun `EPFO contribution received is non-transaction`() {
        val body = "Dear XXXXXXXX0990, your passbook balance against BGBNG**************2889 is Rs. 41,85,400/-. Contribution of Rs. 62,462/- for due month Jun-26 has been received."
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertFalse(SuccessDetector.isSuccessful(body))
    }

    @Test
    fun `NEFT credited to beneficiary is non-transaction`() {
        val body = "ICICI BANK NEFT Transaction with reference number IN12621248301795 for Rs. 25000.00 has been credited to the beneficiary account on 31-07-2026 at 09:45:12"
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertFalse(SuccessDetector.isSuccessful(body))
    }

    @Test
    fun `RTGS credited to beneficiary is non-transaction`() {
        val body = "Dear Customer, RTGS transaction with ICICI Bank Reference No. ICICR12026043010418308 for Rs.995600.00 has been credited to the Beneficiary Account XX9721 on 30-Apr-26 at 11:05:12."
        assertTrue(NonTransactionAlertFilter.isNonTransactionAlert(body))
    }

    @Test
    fun `actual NEFT debit still counts as transaction`() {
        val body = "ICICI Bank Acc XX346 debited Rs. 25,000.00 on 31-Jul-26 InfoBIL*NEFT*IN12.Avl Bal Rs. 13,12,032.84."
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertTrue(SuccessDetector.isSuccessful(body))
    }

    @Test
    fun `actual UPI debit still counts as transaction`() {
        val body = "Dear Customer, Acct XX346 debited for Rs 825.00 on 02-Aug-26; YASHIKA CHICKEN credited. UPI:621461530689. Call 18002662 for dispute."
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertTrue(SuccessDetector.isSuccessful(body))
    }

    @Test
    fun `interest deposit with avl bal still counts as transaction`() {
        val body = "Update! INR 5,279.00 deposited in HDFC Bank A/c XX0328 on 30-JUN-26 for Interest paid till 30-JUN-2026.Avl bal INR 7,45,056.48."
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(body))
    }

    @Test
    fun `HDFC Received IMPS credit is a transaction`() {
        val body = """
            Received!
            INR 12,181.00 in HDFC Bank A/c xx0328
            On 02-08-26
            For IMPS -AMITKUMAR- 621415757925
            Avl bal INR 10,09,205.48
        """.trimIndent()
        assertFalse(NonTransactionAlertFilter.isNonTransactionAlert(body))
        assertTrue(SuccessDetector.isSuccessful(body))
    }
}
