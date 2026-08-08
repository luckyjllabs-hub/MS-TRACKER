package com.jllabs.moneylens.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.jllabs.moneylens.data.database.MoneyLensDatabase
import com.jllabs.moneylens.data.parser.InboxQueueProcessor
import com.jllabs.moneylens.parser.regex.BankSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val db = MoneyLensDatabase.getDatabase(context)
            val recorder = com.jllabs.moneylens.data.parser.SmsInboxRecorder(db.smsInboxDao())
            val learningManager = com.jllabs.moneylens.parser.classifier.CategoryLearningManager(
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

                        val userMappings = com.jllabs.moneylens.data.parser.ClassificationMappingLoader.loadUserMappings(db)
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
