package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_records")
data class CalculationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val metalType: String, // "GOLD" or "SILVER"
    val weight: Double,
    val unit: String, // "گرم" or "اونس"
    val purityLabel: String,
    val refPrice: Long,
    val rawMetalValue: Long,
    val ojratAmount: Long,
    val profitAmount: Long,
    val taxAmount: Long,
    val additionalCosts: Long,
    val discount: Long,
    val totalAmount: Long,
    val note: String = ""
)
