package com.example.mstrackerapp.parser.stage3

import org.junit.Assert.*
import org.junit.Test

class SuccessDetectorTest {

    @Test
    fun `successful debit passes`() {
        assertTrue(SuccessDetector.isSuccessful("Rs.500 debited from A/C XX1234. Avl Bal Rs.10000"))
    }

    @Test
    fun `failed transaction is rejected`() {
        assertFalse(SuccessDetector.isSuccessful("Transaction of Rs.500 has failed. Please try again."))
    }

    @Test
    fun `pending transaction is rejected`() {
        assertFalse(SuccessDetector.isSuccessful("Your payment of Rs.500 is pending."))
    }

    @Test
    fun `OTP message is rejected`() {
        assertFalse(SuccessDetector.isSuccessful("Your OTP is 1234. Do not share."))
    }

    @Test
    fun `successful credit passes`() {
        assertTrue(SuccessDetector.isSuccessful("Rs.10000 credited to A/C XX1234."))
    }

    @Test
    fun `EMI due is rejected`() {
        assertFalse(
            SuccessDetector.isSuccessful(
                "EMI of INR 85956.00 for Axis Bank Loan A/c XX8508 is due on 10-07-26. Maintain adequate balance prior to the due date."
            )
        )
    }

    @Test
    fun `NEFT beneficiary confirmation is rejected`() {
        assertFalse(
            SuccessDetector.isSuccessful(
                "ICICI BANK NEFT Transaction with reference number IN12621248301795 for Rs. 25000.00 has been credited to the beneficiary account on 31-07-2026"
            )
        )
    }
}
