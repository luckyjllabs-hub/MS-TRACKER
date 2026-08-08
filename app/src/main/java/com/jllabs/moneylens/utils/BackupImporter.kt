package com.jllabs.moneylens.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.jllabs.moneylens.domain.models.Account
import com.jllabs.moneylens.domain.models.AccountType
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.domain.models.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MoneyLensBackupPayload(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val accounts: List<Account>
)

object BackupImporter {

    fun exportJson(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): String {
        val root = JSONObject()
        root.put("format", "MoneyLensBackup")
        root.put("version", 1)
        root.put(
            "exportedAt",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        )
        root.put("transactions", JSONArray().also { arr ->
            transactions.forEach { tx -> arr.put(txToJson(tx)) }
        })
        root.put("categories", JSONArray().also { arr ->
            categories.forEach { c ->
                arr.put(
                    JSONObject()
                        .put("id", c.id)
                        .put("name", c.name)
                        .put("icon", c.icon)
                        .put("color", c.color)
                        .put("monthlyLimitMinor", c.monthlyLimitMinor)
                        .put("order", c.order)
                        .put("isArchived", c.isArchived)
                )
            }
        })
        root.put("accounts", JSONArray().also { arr ->
            accounts.forEach { a ->
                arr.put(
                    JSONObject()
                        .put("id", a.id)
                        .put("name", a.name)
                        .put("type", a.type.name)
                        .put("institution", a.institution)
                        .put("startingBalanceMinor", a.startingBalanceMinor)
                        .put("icon", a.icon)
                        .put("includeInNetWorth", a.includeInNetWorth)
                        .put("isArchived", a.isArchived)
                        .put("order", a.order)
                )
            }
        })
        return root.toString(2)
    }

    fun writeBackupFile(
        context: Context,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "MoneyLens_Backup_$stamp.json")
        FileWriter(file).use { it.write(exportJson(transactions, categories, accounts)) }
        return file
    }

    fun shareBackupFile(context: Context, file: File) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "MoneyLens Backup")
            putExtra(Intent.EXTRA_TEXT, "MoneyLens full backup (accounts, categories, transactions).")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share MoneyLens backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun parse(text: String): MoneyLensBackupPayload {
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("{") -> parseJson(trimmed)
            else -> parseLegacyCsv(trimmed)
        }
    }

    fun readUri(context: Context, uri: Uri): MoneyLensBackupPayload {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: error("Could not read file")
        return parse(text)
    }

    private fun parseJson(text: String): MoneyLensBackupPayload {
        val root = JSONObject(text)
        val txs = mutableListOf<Transaction>()
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            txs += jsonToTx(txArr.getJSONObject(i))
        }
        val cats = mutableListOf<Category>()
        val catArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            cats += Category(
                id = o.optString("id"),
                name = o.optString("name"),
                icon = o.optString("icon"),
                color = o.optString("color", "#8F9C8A"),
                monthlyLimitMinor = o.optLong("monthlyLimitMinor"),
                order = o.optInt("order"),
                isArchived = o.optBoolean("isArchived")
            )
        }
        val accounts = mutableListOf<Account>()
        val accArr = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArr.length()) {
            val o = accArr.getJSONObject(i)
            accounts += Account(
                id = o.optString("id"),
                name = o.optString("name"),
                type = try {
                    AccountType.valueOf(o.optString("type", "BANK"))
                } catch (_: Exception) {
                    AccountType.BANK
                },
                institution = o.optString("institution"),
                startingBalanceMinor = o.optLong("startingBalanceMinor"),
                icon = o.optString("icon"),
                includeInNetWorth = o.optBoolean("includeInNetWorth", true),
                isArchived = o.optBoolean("isArchived"),
                order = o.optInt("order")
            )
        }
        return MoneyLensBackupPayload(txs, cats, accounts)
    }

    /** Best-effort import of the old 4-column CSV (Date,Merchant,Money Spent,Type). */
    private fun parseLegacyCsv(text: String): MoneyLensBackupPayload {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("===") }
            .toList()
        if (lines.isEmpty()) return MoneyLensBackupPayload(emptyList(), emptyList(), emptyList())
        val start = if (lines.first().contains("Date", ignoreCase = true) &&
            lines.first().contains("Merchant", ignoreCase = true)
        ) 1 else 0
        val txs = mutableListOf<Transaction>()
        for (i in start until lines.size) {
            val cols = splitCsvLine(lines[i])
            if (cols.size < 4) continue
            val date = cols[0].trim().trim('"')
            val merchant = cols[1].trim().trim('"')
            val amountMinor = parseMoneyToMinor(cols[2])
            val type = try {
                TransactionType.valueOf(cols[3].trim().trim('"').uppercase(Locale.US))
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }
            if (date.isBlank() || amountMinor <= 0L) continue
            txs += Transaction(
                id = "import-${date}-${merchant.hashCode()}-$amountMinor-$i",
                type = type,
                amountMinor = amountMinor,
                accountId = "acc-1",
                categoryId = "cat-14",
                merchant = merchant.ifBlank { "Imported" },
                date = normalizeDate(date),
                time = "12:00",
                note = "Imported from CSV",
                source = "IMPORT",
                isManual = true
            )
        }
        return MoneyLensBackupPayload(txs, emptyList(), emptyList())
    }

    private fun txToJson(tx: Transaction): JSONObject =
        JSONObject()
            .put("id", tx.id)
            .put("type", tx.type.name)
            .put("amountMinor", tx.amountMinor)
            .put("accountId", tx.accountId)
            .put("toAccountId", tx.toAccountId)
            .put("categoryId", tx.categoryId)
            .put("merchant", tx.merchant)
            .put("date", tx.date)
            .put("time", tx.time)
            .put("note", tx.note)
            .put("source", tx.source)
            .put("bankName", tx.bankName)
            .put("accountLast4", tx.accountLast4)
            .put("referenceNumber", tx.referenceNumber)
            .put("status", tx.status)
            .put("confidence", tx.confidence)
            .put("isManual", tx.isManual)
            .put("isReviewed", tx.isReviewed)
            .put("rawSms", tx.rawSms)
            .put("upiId", tx.upiId)
            .put("availableBalance", tx.availableBalance)
            .put("messageType", tx.messageType)
            .put("smsTransactionSubType", tx.smsTransactionSubType)
            .put("smsSender", tx.smsSender)
            .put("createdAt", tx.createdAt)

    private fun jsonToTx(o: JSONObject): Transaction =
        Transaction(
            id = o.optString("id").ifBlank { "import-${System.nanoTime()}" },
            type = try {
                TransactionType.valueOf(o.optString("type", "EXPENSE"))
            } catch (_: Exception) {
                TransactionType.EXPENSE
            },
            amountMinor = o.optLong("amountMinor"),
            accountId = o.optString("accountId", "acc-1"),
            toAccountId = o.optString("toAccountId").ifBlank { null },
            categoryId = o.optString("categoryId", "cat-14"),
            merchant = o.optString("merchant"),
            date = o.optString("date"),
            time = o.optString("time", "12:00"),
            note = o.optString("note"),
            source = o.optString("source", "IMPORT"),
            bankName = o.optString("bankName"),
            accountLast4 = o.optString("accountLast4"),
            referenceNumber = o.optString("referenceNumber"),
            status = o.optString("status", "CONFIRMED"),
            confidence = o.optString("confidence", "HIGH"),
            isManual = o.optBoolean("isManual", true),
            isReviewed = o.optBoolean("isReviewed", true),
            rawSms = o.optString("rawSms"),
            upiId = o.optString("upiId"),
            availableBalance = if (o.has("availableBalance") && !o.isNull("availableBalance")) {
                o.optLong("availableBalance")
            } else null,
            messageType = o.optString("messageType"),
            smsTransactionSubType = o.optString("smsTransactionSubType"),
            smsSender = o.optString("smsSender"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )

    private fun parseMoneyToMinor(raw: String): Long {
        val cleaned = raw.trim().trim('"')
            .replace("₹", "")
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("INR", "", ignoreCase = true)
            .replace(",", "")
            .replace(" ", "")
            .trim()
        val value = cleaned.toDoubleOrNull() ?: return 0L
        return (value * 100).toLong()
    }

    private fun normalizeDate(raw: String): String {
        val formats = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "dd-MMM-yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                val d = sdf.parse(raw) ?: continue
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
            } catch (_: Exception) {
            }
        }
        return raw.take(10)
    }

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    out += cur.toString()
                    cur.clear()
                }
                else -> cur.append(ch)
            }
        }
        out += cur.toString()
        return out
    }
}
