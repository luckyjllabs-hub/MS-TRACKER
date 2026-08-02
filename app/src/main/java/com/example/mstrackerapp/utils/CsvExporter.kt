package com.example.mstrackerapp.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.mstrackerapp.domain.models.Account
import com.example.mstrackerapp.domain.models.Category
import com.example.mstrackerapp.domain.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): File {
        val fileName = "mstracker_export_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val csvFile = File(exportDir, fileName)
        val writer = FileWriter(csvFile)

        // Write Header
        writer.append("ID,Type,Amount (INR),Date,Time,Merchant,Category,Account,Note,Source\n")

        // Write Rows
        transactions.forEach { tx ->
            val cat = categories.find { it.id == tx.categoryId }?.name ?: "Category"
            val acc = accounts.find { it.id == tx.accountId }?.name ?: "Account"
            val amountRupees = tx.amountMinor / 100.0

            val row = listOf(
                escapeCsv(tx.id),
                escapeCsv(tx.type.name),
                "%.2f".format(amountRupees),
                escapeCsv(tx.date),
                escapeCsv(tx.time),
                escapeCsv(tx.merchant),
                escapeCsv(cat),
                escapeCsv(acc),
                escapeCsv(tx.note),
                escapeCsv(tx.source)
            ).joinToString(",")

            writer.append(row).append("\n")
        }

        writer.flush()
        writer.close()

        return csvFile
    }

    /**
     * Reads ALL SMS messages from the device inbox (no cap) and exports them as CSV.
     * Includes every message — bank transactional, OTP, promotional — for full audit.
     */
    suspend fun exportRawSmsToCsv(context: Context): File = withContext(Dispatchers.IO) {
        val fileName = "mstracker_raw_sms_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val csvFile = File(exportDir, fileName)
        val writer = FileWriter(csvFile)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        // CSV Header
        writer.append("Sender,Date,Time,DateTime_Raw,Body\n")

        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            writer.append("ERROR,No SMS permission granted,,,\n")
            writer.flush()
            writer.close()
            return@withContext csvFile
        }

        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC"  // No LIMIT — read all SMS
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

                while (c.moveToNext()) {
                    val sender = c.getString(addressIdx) ?: "Unknown"
                    val body = c.getString(bodyIdx) ?: ""
                    val dateMs = c.getLong(dateIdx)
                    val dateObj = Date(dateMs)
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateObj)
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dateObj)
                    val fullDateTime = sdf.format(dateObj)

                    val row = listOf(
                        escapeCsv(sender),
                        escapeCsv(dateStr),
                        escapeCsv(timeStr),
                        escapeCsv(fullDateTime),
                        escapeCsv(body)
                    ).joinToString(",")

                    writer.append(row).append("\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            writer.append("ERROR,${escapeCsv(e.message ?: "unknown error")},,,\n")
        }

        writer.flush()
        writer.close()

        return@withContext csvFile
    }

    fun shareCsvFile(context: Context, csvFile: File, subject: String = "MS Tracker Financial Export") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
