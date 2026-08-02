package com.example.mstrackerapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mstrackerapp.data.database.entities.UserLearnedMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLearnedMappingDao {

    @Query("SELECT * FROM user_learned_mappings")
    fun getAllLearnedMappings(): Flow<List<UserLearnedMappingEntity>>

    @Query("SELECT * FROM user_learned_mappings WHERE merchant = :merchant LIMIT 1")
    suspend fun getMappingForMerchant(merchant: String): UserLearnedMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMapping(mapping: UserLearnedMappingEntity)
}
