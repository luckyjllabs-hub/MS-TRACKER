package com.example.mstrackerapp.data.parser

import com.example.mstrackerapp.data.database.dao.SMSInboxDao
import com.example.mstrackerapp.data.database.entities.SMSInboxEntity
import com.example.mstrackerapp.parser.pipeline.MessageTypeClassifier
import java.security.MessageDigest

/** Persists every received SMS before finance parsing, enabling safe future reprocessing. */
class SmsInboxRecorder(private val dao: SMSInboxDao) {
    suspend fun record(sender: String, body: String, timestamp: Long): String {
        val hash = sha256("$sender\u0000$timestamp\u0000$body")
        dao.insertSms(SMSInboxEntity(
            id = hash,
            sender = sender,
            body = body,
            timestamp = timestamp,
            messageType = MessageTypeClassifier.classify(sender, body).name,
            rawHash = hash
        ))
        return hash
    }

    suspend fun markProcessed(hash: String, status: String) = dao.markProcessed(hash, status)

    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
