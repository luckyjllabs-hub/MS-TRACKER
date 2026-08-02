package com.example.mstrackerapp.parser.stage1

import com.example.mstrackerapp.domain.models.MessageType
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTypeClassifierTest {

    @Test
    fun `OTP message is classified as OTP`() {
        val result = MessageTypeClassifier.classify(
            sender = "HDFCBK",
            body = "Your OTP is 123456. Do not share with anyone."
        )
        assertEquals(MessageType.OTP, result)
    }

    @Test
    fun `HDFC debit message is FINANCIAL_TRANSACTION`() {
        val result = MessageTypeClassifier.classify(
            sender = "HDFCBK",
            body = "Rs.500.00 debited from A/C XX1234 on 01-Aug-26. Info:UPI. Avl Bal:Rs.12,345.00"
        )
        assertEquals(MessageType.FINANCIAL_TRANSACTION, result)
    }

    @Test
    fun `Promotional message is PROMOTIONAL`() {
        val result = MessageTypeClassifier.classify(
            sender = "OFFERS",
            body = "Exclusive offer! Click here to win a free iPhone. Limited time deal."
        )
        assertEquals(MessageType.PROMOTIONAL, result)
    }

    @Test
    fun `Loan offer is LOAN`() {
        val result = MessageTypeClassifier.classify(
            sender = "BANKLN",
            body = "You have a pre-approved personal loan of Rs.5,00,000. Apply now!"
        )
        assertEquals(MessageType.LOAN, result)
    }

    @Test
    fun `KYC message is KYC`() {
        val result = MessageTypeClassifier.classify(
            sender = "SBIINB",
            body = "Your KYC is pending. Please complete your KYC to avoid account restrictions."
        )
        assertEquals(MessageType.KYC, result)
    }
}
