package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CalculationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CalculationRecordEntity): Long

    @Query("SELECT * FROM calculation_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CalculationRecordEntity>>

    @Query("DELETE FROM calculation_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calculation_records")
    suspend fun deleteAll()
}
