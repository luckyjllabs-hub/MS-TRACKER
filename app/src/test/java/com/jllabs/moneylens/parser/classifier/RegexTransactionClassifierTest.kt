package com.jllabs.moneylens.parser.classifier

import com.jllabs.moneylens.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class RegexTransactionClassifierTest {

    @Test
    fun testClassifySalary() {
        val sms = "Your account has been credited with Salary of INR 300,000.00 from ACME Corp"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.SALARY, result.category)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(0.80, result.score, 0.01)
    }

    @Test
    fun testClassifyRefund() {
        val sms = "Refund of Rs 450.00 for Swiggy order 1234 has been processed into account"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.REFUND, result.category)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(0.80, result.score, 0.01)
    }

    @Test
    fun testClassifyAtmWithdrawal() {
        val sms = "Rs 5,000.00 debited from account for ATM WDL at HDFC BANK ATM MUMBAI"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.ATM, result.category)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(0.95, result.score, 0.01)
    }

    @Test
    fun testClassifyEmi() {
        val sms = "Auto debit EMI of Rs 15,000.00 processed for Home Loan account 9876"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.EMI, result.category)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(0.80, result.score, 0.01)
    }

    @Test
    fun testClassifyInterest() {
        val sms = "Interest credited Rs 420.50 into your savings account ending 1234"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.INTEREST, result.category)
        assertEquals(TransactionType.INCOME, result.transactionType)
        assertEquals(0.80, result.score, 0.01)
    }

    @Test
    fun testClassifyTransfer() {
        val sms = "NEFT transfer of Rs 2,500.00 debited from account"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.TRANSFER, result.category)
        assertEquals(TransactionType.TRANSFER, result.transactionType)
        assertEquals(0.80, result.score, 0.01)
    }

    @Test
    fun testClassifyGeneralDebit() {
        val sms = "Rs 1,250.00 debited from HDFC card 1234 at Starbucks"
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.DEBIT, result.category)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(0.95, result.score, 0.01)
    }

    @Test
    fun testClassifyUnknown() {
        val sms = "Good morning! Your statement is available for download."
        val result = RegexTransactionClassifier.classify(sms)

        assertEquals(TransactionClassificationCategory.UNKNOWN, result.category)
        assertEquals(0.30, result.score, 0.01)
    }

    @Test fun rejectsOtpEvenWhenItContainsAnAmount() {
        val result = RegexTransactionClassifier.classify("Your OTP for Rs 5,000 payment is 123456. Do not share it.")
        assertEquals(false, result.isFinancial)
    }

    @Test fun rejectsDueAndPendingReminders() {
        assertEquals(false, RegexTransactionClassifier.classify("Payment due: Rs 2,000 on your card").isFinancial)
        assertEquals(false, RegexTransactionClassifier.classify("UPI payment of Rs 200 is pending").isFinancial)
    }

    @Test fun rejectsFailedAndReversedTransactions() {
        assertEquals(false, RegexTransactionClassifier.classify("Rs 300 debited transaction failed and reversed").isFinancial)
    }

    @Test fun recognizesCompletedCreditAndDebit() {
        assertEquals(TransactionType.INCOME, RegexTransactionClassifier.classify("HDFC: Rs 20,000 salary credited to account 1234").transactionType)
        assertEquals(TransactionType.EXPENSE, RegexTransactionClassifier.classify("HDFC: Rs 450 spent at a POS merchant").transactionType)
    }
}
