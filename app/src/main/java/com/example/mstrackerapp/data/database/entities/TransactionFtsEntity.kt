package com.example.mstrackerapp.data.database.entities

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "transactions_fts")
@Fts4(contentEntity = TransactionEntity::class)
data class TransactionFtsEntity(
    val merchant: String,
    val note: String,
    val date: String,
    val source: String
)
