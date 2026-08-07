package com.example.mstrackerapp.parser.stage5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantExtractorCreditNameTest {

    @Test
    fun `icici credited from person name`() {
        val body = "Dear Customer, Acct XX346 is credited with Rs 275.00 on 07-Aug-26 from RAVI SANKAR GUN. UPI:621958773310-ICICI Bank"
        val merchant = MerchantExtractor.extractMerchant(body)
        assertTrue("Got: $merchant", merchant.contains("Ravi", ignoreCase = true) && merchant.contains("Sankar", ignoreCase = true))
        assertFalse("Got: $merchant", merchant.equals("Vi", ignoreCase = true))
        assertFalse("Got: $merchant", merchant.equals("Ola", ignoreCase = true))
    }

    @Test
    fun `hdfc tpt deposit extracts polamreddy not ola`() {
        val body = "Update! INR 13,572.00 deposited in HDFC Bank A/c XX0328 on 05-AUG-26 for XXXXXXXXXX7408-TPT-HDFC72732E5BFB93-POLAMREDDY KARTHIK REDDY.Avl bal INR 10,85,754.48. Cheque deposits in A/C are subject to clearing"
        val merchant = MerchantExtractor.extractMerchant(body)
        assertTrue("Got: $merchant", merchant.contains("Polamreddy", ignoreCase = true))
        assertFalse("Got: $merchant", merchant.equals("Ola", ignoreCase = true))
    }

    @Test
    fun `hdfc credit from VPA extracts handle not VPA`() {
        val body = "Credit Alert!\nRs.7158.00 credited to HDFC Bank A/c XX0328 on 01-08-26 from VPA r.rajeshbarath-2@oksbi (UPI 621320527288)"
        val merchant = MerchantExtractor.extractMerchant(body)
        assertTrue("Got: $merchant", merchant.contains("rajeshbarath", ignoreCase = true))
        assertFalse("Got: $merchant", merchant.equals("VPA", ignoreCase = true))
        assertFalse("Got: $merchant", merchant.equals("Upi", ignoreCase = true))
    }
}
