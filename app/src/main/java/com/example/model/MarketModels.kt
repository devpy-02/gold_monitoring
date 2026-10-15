package com.example.model

import java.math.BigDecimal

enum class MarketCategory(val labelPersian: String) {
    GOLD("طلا و مسکوکات"),
    SILVER("نقره"),
    CURRENCY("ارز و اونس جهانی")
}

data class MarketPriceItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val category: MarketCategory,
    val currentPriceToman: BigDecimal,
    val changePercent24h: Double,
    val unit: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isLive: Boolean = true,
    val isManualOverride: Boolean = false
)

data class PriceHistoryPoint(
    val timestamp: Long,
    val priceToman: BigDecimal
)

enum class TimePeriod(val labelPersian: String, val days: Int) {
    DAY_1("۱ روز", 1),
    WEEK_1("۱ هفته", 7),
    MONTH_1("۱ ماه", 30),
    MONTH_3("۳ ماه", 90)
}
