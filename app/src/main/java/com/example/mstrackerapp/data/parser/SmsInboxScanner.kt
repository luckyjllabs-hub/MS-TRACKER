package com.example.mstrackerapp.data.parser

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.mstrackerapp.data.database.MSTrackerDatabase
import com.example.mstrackerapp.parser.regex.BankSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RawSmsMessage(
    val sender: String,
    val body: String,
    val date: Long,
    val threadId: Long
)

object SmsInboxScanner {

    private val TRANSACTION_KEYWORDS = listOf(
        "debited", "credited", "spent", "sent", "paid", "received", "withdrawn", "txn", "transfer", "inr", "rs.", "rs ", "₹"
    )

    suspend fun readHistoricalInbox(context: Context): List<RawSmsMessage> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<RawSmsMessage>()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }

        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE,
            Telephony.Sms.Inbox.THREAD_ID
        )

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)
                val threadIdIdx = c.getColumnIndexOrThrow(Telephony.Sms.Inbox.THREAD_ID)

                while (c.moveToNext()) {
                    val sender = c.getString(addressIdx) ?: "Unknown"
                    val body = c.getString(bodyIdx) ?: ""
                    val date = c.getLong(dateIdx)
                    val threadId = c.getLong(threadIdIdx)

                    val bodyLower = body.lowercase()
                    if (TRANSACTION_KEYWORDS.any { bodyLower.contains(it) }) {
                        smsList.add(
                            RawSmsMessage(
                                sender = sender,
                                body = body,
                                date = date,
                                threadId = threadId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext smsList
    }

    fun scanExistingInbox(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MSTrackerDatabase.getDatabase(context)
            val recorder = SmsInboxRecorder(db.smsInboxDao())
            val learningManager = com.example.mstrackerapp.parser.classifier.CategoryLearningManager(db.userLearnedMappingDao())
            val processor = InboxQueueProcessor(db.smsQueueDao(), db.transactionDao(), learningManager)

            val messages = readHistoricalInbox(context)
            if (messages.isEmpty()) return@launch

            for (msg in messages) {
                val rawHash = recorder.record(msg.sender, msg.body, msg.date)
                val status = processor.processParsedSms(
                    rawSmsText = msg.body,
                    senderBank = msg.sender,
                    timestamp = msg.date
                )
                recorder.markProcessed(rawHash, status.name)
            }
        }
    }
}
