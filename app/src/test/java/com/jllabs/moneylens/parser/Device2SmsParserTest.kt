package com.jllabs.moneylens.parser

import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.parser.stage4.DebitCreditDetector
import com.jllabs.moneylens.parser.stage5.AmountParser
import com.jllabs.moneylens.parser.stage5.BankParser
import com.jllabs.moneylens.parser.stage5.MerchantExtractor
import com.jllabs.moneylens.parser.stage6.SmsCategory
import org.junit.Assert.*
import org.junit.Test

class Device2SmsParserTest {

    @Test
    fun testHyderabadIraniExtractionAndCategory() {
        val body = "Rs 120.00 debited from ICICI Bank A/C ending 1234 at HYDERABAD IRANI on 01-Aug-2026. Ref: 987654."
        val merchant = MerchantExtractor.extractMerchant(body)
        val dc = DebitCreditDetector.detect(body)
        val category = SmsCategory.classify(merchant, body, dc.subType)

        assertEquals("HYDERABAD IRANI", merchant)
        assertEquals(TransactionType.EXPENSE, dc.transactionType)
        assertEquals("Food", category.categoryName)
    }

    @Test
    fun testBlinkitExtractionAndCategory() {
        val body = "Paid Rs 480.00 to BLINKIT from HDFC Bank A/C ending 5678 on 01-Aug-2026."
        val merchant = MerchantExtractor.extractMerchant(body)
        val dc = DebitCreditDetector.detect(body)
        val category = SmsCategory.classify(merchant, body, dc.subType)

        assertEquals("BLINKIT", merchant)
        assertEquals(TransactionType.EXPENSE, dc.transactionType)
        assertEquals("Shopping", category.categoryName)
    }

    @Test
    fun testMedplusExtractionAndCategory() {
        val body = "Rs 580.50 debited at MEDPLUS CHANNAS from ICICI Bank A/C 9999."
        val merchant = MerchantExtractor.extractMerchant(body)
        val dc = DebitCreditDetector.detect(body)
        val category = SmsCategory.classify(merchant, body, dc.subType)

        assertTrue(merchant.contains("MEDPLUS"))
        assertEquals(TransactionType.EXPENSE, dc.transactionType)
        assertEquals("Health", category.categoryName)
    }

    @Test
    fun testPrepaidCardExtractionNotDebit() {
        val body = "Your ICICI Bank Prepaid Card ending 5097 has been debited for Rs 4152.00 at APOLLO PHARMACY on 01-Aug-2026."
        val merchant = MerchantExtractor.extractMerchant(body)
        val dc = DebitCreditDetector.detect(body)
        val category = SmsCategory.classify(merchant, body, dc.subType)

        assertFalse(merchant.contains("PREPAID"))
        assertNotEquals("DEBIT", merchant)
        assertEquals("APOLLO PHARMACY", merchant)
        assertEquals("Health", category.categoryName)
    }

    @Test
    fun testIncomeNotDebitMerchant() {
        val body = "Rs 14504.00 credited to HDFC Bank A/C 1234 on 01-Aug-2026 by salary transfer."
        val merchant = MerchantExtractor.extractMerchant(body)
        val dc = DebitCreditDetector.detect(body)

        assertNotEquals("DEBIT", merchant)
        assertEquals(TransactionType.INCOME, dc.transactionType)
    }

    @Test
    fun testPersonTransferMerchant() {
        val body = "Rs 10000.00 debited from ICICI Bank A/C 1234; HANUMANTHAPPA R credited."
        val merchant = MerchantExtractor.extractMerchant(body)
        val dc = DebitCreditDetector.detect(body)

        assertEquals("HANUMANTHAPPA R", merchant)
        assertEquals(TransactionType.EXPENSE, dc.transactionType)
    }

    @Test
    fun testHdfcCardPaymentCreditedToCardNotIncome() {
        val body = "HDFC Bank Cardmember, Online Payment of Rs.14504 vide Ref# 2121052225m9iLE was credited to your card ending 9355 On 31/JUL/2026_value Date 31/JUL/2026"
        val amount = AmountParser.parseAmountMinor(body)
        val dc = DebitCreditDetector.detect(body)

        assertEquals(1450400L, amount)
        assertEquals(TransactionType.EXPENSE, dc.transactionType)
        assertEquals(com.jllabs.moneylens.domain.models.SmsTransactionSubType.CARD_BILL_PAYMENT, dc.subType)
    }
}
