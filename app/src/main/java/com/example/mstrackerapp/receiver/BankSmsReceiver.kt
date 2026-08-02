package com.example.mstrackerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.mstrackerapp.data.database.MSTrackerDatabase
import com.example.mstrackerapp.data.parser.InboxQueueProcessor
import com.example.mstrackerapp.parser.regex.BankSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val db = MSTrackerDatabase.getDatabase(context)
            val recorder = com.example.mstrackerapp.data.parser.SmsInboxRecorder(db.smsInboxDao())
            val learningManager = com.example.mstrackerapp.parser.classifier.CategoryLearningManager(
                db.userLearnedMappingDao(),
                db.merchantDao(),
                db.merchantAliasDao()
            )
            val processor = InboxQueueProcessor(db.smsQueueDao(), db.transactionDao(), learningManager)

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (sms in messages) {
                        val body = sms.messageBody ?: continue
                        val sender = sms.originatingAddress ?: "Unknown"
                        val timestamp = sms.timestampMillis
                        val rawHash = recorder.record(sender, body, timestamp)

                        val metadata = BankSmsParser.parseMetadata(body)
                        val bankName = metadata.bankName ?: sender

                        val userMappings = com.example.mstrackerapp.data.parser.ClassificationMappingLoader.loadUserMappings(db)
                        val status = processor.processParsedSms(
                            rawSmsText = body,
                            senderBank = bankName,
                            timestamp = timestamp,
                            customMappings = userMappings
                        )
                        recorder.markProcessed(rawHash, status.name)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
