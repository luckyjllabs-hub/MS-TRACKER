package com.jllabs.moneylens.domain.accounts

import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalTransferClassifierTest {

    @Test
    fun `paired transfer same day amount excluded from income expense`() {
        val out = Transaction(
            id = "1",
            type = TransactionType.EXPENSE,
            amountMinor = 500_00,
            accountId = "a",
            categoryId = "c",
            merchant = "Self",
            date = "2026-08-01",
            time = "10:00",
            rawSms = "Acct XX1111 debited for IMPS to XX2222 Rs.500",
            accountLast4 = "1111",
            bankName = "HDFC"
        )
        val `in` = Transaction(
            id = "2",
            type = TransactionType.INCOME,
            amountMinor = 500_00,
            accountId = "b",
            categoryId = "c",
            merchant = "Self",
            date = "2026-08-01",
            time = "10:01",
            rawSms = "Acct XX2222 credited by IMPS from XX1111 Rs.500",
            accountLast4 = "2222",
            bankName = "ICICI"
        )
        val salary = Transaction(
            id = "3",
            type = TransactionType.INCOME,
            amountMinor = 1000_00,
            accountId = "a",
            categoryId = "c",
            merchant = "Payroll",
            date = "2026-08-01",
            time = "09:00",
            rawSms = "Salary credited Rs.1000 to XX1111",
            accountLast4 = "1111",
            bankName = "HDFC"
        )
        val (income, expense) = InternalTransferClassifier.incomeExpenseTotals(listOf(out, `in`, salary))
        assertEquals(1000_00, income)
        assertEquals(0L, expense)
        assertTrue("1" in InternalTransferClassifier.excludedFromIncomeExpenseIds(listOf(out, `in`, salary)))
        assertTrue("2" in InternalTransferClassifier.excludedFromIncomeExpenseIds(listOf(out, `in`, salary)))
    }
}
