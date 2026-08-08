package com.jllabs.moneylens.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jllabs.moneylens.domain.models.Account
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.Transaction
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAndShareCsv(
        context: Context,
        filterName: String,
        netWorthMinor: Long,
        incomeMinor: Long,
        expenseMinor: Long,
        savingsMinor: Long,
        transactions: List<Transaction>
    ) {
        try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val csvFile = File(exportsDir, "MoneyLens_Summary_Transactions_$timeStamp.csv")

            FileWriter(csvFile).use { writer ->
                // 1. Summary Header Section
                writer.append("=== MONEYLENS FINANCIAL SUMMARY ===\n")
                writer.append("Filter Period,${escapeCsv(filterName)}\n")
                writer.append("Export Date,${escapeCsv(SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()))}\n")
                writer.append("Net Worth,${escapeCsv(Money.format(netWorthMinor))}\n")
                writer.append("Total Income,${escapeCsv(Money.format(incomeMinor))}\n")
                writer.append("Total Expense,${escapeCsv(Money.format(expenseMinor))}\n")
                writer.append("Net Savings,${escapeCsv(Money.format(savingsMinor))}\n")
                writer.append("\n")

                // 2. Transactions Table Header (EXACTLY 4 COLUMNS: Date, Merchant, Money Spent, Type)
                writer.append("Date,Merchant,Money Spent,Type\n")

                // 3. Transactions Rows
                for (tx in transactions) {
                    val amountRupees = Money.format(tx.amountMinor, showSymbol = true)
                    val dateEsc = escapeCsv(tx.date)
                    val merchantEsc = escapeCsv(tx.merchant)
                    val amountEsc = escapeCsv(amountRupees)
                    val typeEsc = escapeCsv(tx.type.name)

                    writer.append("$dateEsc,$merchantEsc,$amountEsc,$typeEsc\n")
                }
            }

            // 4. FileProvider Uri & Share Intent
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "MoneyLens Summary & Transactions ($filterName)")
                putExtra(Intent.EXTRA_TEXT, "Attached is your MoneyLens financial summary and transactions CSV report for $filterName.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Summary & Transactions CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<Transaction>,
        categories: List<Category> = emptyList(),
        accounts: List<Account> = emptyList()
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(exportsDir, "MoneyLens_Transactions_$timeStamp.csv")

        FileWriter(file).use { writer ->
            writer.append("Date,Merchant,Money Spent,Type\n")
            for (tx in transactions) {
                val amountRupees = Money.format(tx.amountMinor, showSymbol = true)
                writer.append("${escapeCsv(tx.date)},${escapeCsv(tx.merchant)},${escapeCsv(amountRupees)},${escapeCsv(tx.type.name)}\n")
            }
        }
        return file
    }

    fun exportRawSmsToCsv(context: Context): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(exportsDir, "MoneyLens_RawSms_$timeStamp.csv")

        FileWriter(file).use { writer ->
            writer.append("Timestamp,Sender,Body\n")
            val cursor = try {
                context.contentResolver.query(
                    android.net.Uri.parse("content://sms/inbox"),
                    arrayOf("address", "body", "date"),
                    null, null, "date DESC"
                )
            } catch (e: Exception) { null }

            cursor?.use {
                val addressIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                while (it.moveToNext()) {
                    val address = if (addressIdx >= 0) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                    val dateLong = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(dateLong))
                    writer.append("${escapeCsv(dateStr)},${escapeCsv(address)},${escapeCsv(body)}\n")
                }
            }
        }
        return file
    }

    fun shareCsvFile(context: Context, file: File, subject: String = "MoneyLens CSV Export") {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Attached CSV file from MoneyLens.")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, subject)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        if (value.isBlank()) return "\"\""
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
