package com.example.mstrackerapp.parser.classifier

import com.example.mstrackerapp.domain.models.SmsTransactionSubType
import com.example.mstrackerapp.parser.stage6.SmsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantNormalizerTest {

    @Test
    fun `normalizes amazon aliases`() {
        assertEquals("Amazon", MerchantNormalizer.normalize("AMZN"))
        assertEquals("Amazon", MerchantNormalizer.normalize("AMAZON PAY"))
        assertEquals("Amazon", MerchantNormalizer.normalize("Amazon Seller Services"))
        assertEquals("Amazon", MerchantNormalizer.normalize("APAY"))
    }

    @Test
    fun `normalizes swiggy aliases`() {
        assertEquals("Swiggy", MerchantNormalizer.normalize("SWIGGY LTD"))
        assertEquals("Swiggy", MerchantNormalizer.normalize("SWIGGY INSTAMART"))
        assertEquals("Swiggy", MerchantNormalizer.normalize("INSTAMART"))
    }

    @Test
    fun `normalizes uber aliases`() {
        assertEquals("Uber", MerchantNormalizer.normalize("UBER INDIA"))
        assertEquals("Uber", MerchantNormalizer.normalize("UBER TRIP"))
    }
}

class MerchantExtractionClassificationTest {

    @Test
    fun `extracts ICICI UPI merchant and classifies food`() {
        val body = "Dear Customer, Acct XX346 debited for Rs 825.00 on 02-Aug-26; YASHIKA CHICKEN credited. UPI:621461530689."
        val merchant = com.example.mstrackerapp.parser.stage5.MerchantExtractor.extractMerchant(body)
        assertEquals("Yashika Chicken", merchant)
        val cat = SmsCategory.classify(merchant, body, SmsTransactionSubType.UPI_PAYMENT)
        assertEquals("cat-1", cat.categoryId)
    }

    @Test
    fun `extracts MedPlus and classifies health`() {
        val body = "Dear Customer, Acct XX346 debited for Rs 412.00; MEDPLUS CHANNAS credited. UPI:123"
        val merchant = com.example.mstrackerapp.parser.stage5.MerchantExtractor.extractMerchant(body)
        assertEquals("MedPlus", merchant)
        val cat = SmsCategory.classify(merchant, body, SmsTransactionSubType.UPI_PAYMENT)
        assertEquals("cat-6", cat.categoryId)
        assertEquals("MERCHANT_DICT", cat.source)
    }

    @Test
    fun `Jio prepaid classifies as bills`() {
        val body = "Dear Customer, Acct XX346 debited for Rs 239.00; JIO PREPAID REC credited. UPI:999"
        val merchant = com.example.mstrackerapp.parser.stage5.MerchantExtractor.extractMerchant(body)
        assertEquals("Jio", merchant)
        val cat = SmsCategory.classify(merchant, body, SmsTransactionSubType.UPI_PAYMENT)
        assertEquals("cat-9", cat.categoryId)
    }

    @Test
    fun `keyword fuel pump classifies fuel`() {
        val body = "Rs.2000 spent at KESARI PETRO PARK on card XX1234"
        val merchant = com.example.mstrackerapp.parser.stage5.MerchantExtractor.extractMerchant(body)
        val cat = SmsCategory.classify(merchant, body, SmsTransactionSubType.CARD_PURCHASE)
        assertEquals("cat-8", cat.categoryId)
    }

    @Test
    fun `user learned overrides dictionary`() {
        val cat = SmsCategory.classify(
            merchant = "Swiggy",
            body = "Paid at Swiggy",
            subType = SmsTransactionSubType.UPI_PAYMENT,
            userMappings = mapOf("SWIGGY" to "cat-8")
        )
        assertEquals("cat-8", cat.categoryId)
        assertEquals("USER_LEARNED", cat.source)
    }

    @Test
    fun `unknown person UPI becomes transfer not other`() {
        val cat = SmsCategory.classify(
            merchant = "Sanchi Sivakuma",
            body = "Acct XX346 debited for Rs 500; SANCHI SIVAKUMA credited. UPI:1",
            subType = SmsTransactionSubType.UPI_PAYMENT
        )
        assertEquals("cat-13", cat.categoryId)
        assertEquals("SUBTYPE", cat.source)
    }

    @Test
    fun `placeholder merchant is not known`() {
        assertFalse(MerchantNormalizer.isKnownMerchant("BANK TRANSACTION"))
        assertFalse(MerchantNormalizer.isKnownMerchant("UPI TRANSFER"))
        assertTrue(MerchantNormalizer.isKnownMerchant("Amazon"))
    }
}
