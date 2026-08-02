package com.example.mstrackerapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_learned_mappings")
data class UserLearnedMappingEntity(
    @PrimaryKey val merchant: String,
    val categoryId: String,
    val timesUsed: Int = 1,
    val confidence: String = "USER_CONFIRMED",
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
