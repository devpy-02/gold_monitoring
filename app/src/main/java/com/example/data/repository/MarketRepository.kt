package com.example.data.repository

import android.content.Context
import com.example.model.MarketCategory
import com.example.model.MarketPriceItem
import com.example.model.PriceHistoryPoint
import com.example.model.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

sealed class MarketDataState {
    data object Loading : MarketDataState()
    data class Success(
        val items: List<MarketPriceItem>,
        val lastUpdated: Long,
        val isStale: Boolean,
        val isOffline: Boolean,
        val hasManualOverrides: Boolean,
        val sourceTitle: String = "اتحادیه طلا و ارز (TGJU)"
    ) : MarketDataState()
    data class Error(val message: String, val cachedItems: List<MarketPriceItem>) : MarketDataState()
}

class MarketRepository(context: Context) {

    private val sharedPrefs = context.getSharedPreferences("market_cache", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .build()

    // Real Iranian market current reference rates (in Tomans)
    private val baselineItems = listOf(
        MarketPriceItem(
            id = "gold_18k",
            titleFa = "طلای ۱۸ عیار",
            titleEn = "Gold 18K",
            category = MarketCategory.GOLD,
            currentPriceToman = BigDecimal("26045000"),
            changePercent24h = +0.65,
            unit = "تومان / گرم",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "gold_24k",
            titleFa = "طلای ۲۴ عیار (آبشده)",
            titleEn = "Gold 24K",
            category = MarketCategory.GOLD,
            currentPriceToman = BigDecimal("34718000"),
            changePercent24h = +0.72,
            unit = "تومان / گرم",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "gold_mesghal",
            titleFa = "مظنه (مثقال طلا)",
            titleEn = "Mesghal Gold",
            category = MarketCategory.GOLD,
            currentPriceToman = BigDecimal("112797000"),
            changePercent24h = +0.58,
            unit = "تومان / ۴.۶۰۸ گرم",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "coin_emami",
            titleFa = "سکه تمام بهار (امامی)",
            titleEn = "Emami Gold Coin",
            category = MarketCategory.GOLD,
            currentPriceToman = BigDecimal("267090000"),
            changePercent24h = +0.95,
            unit = "تومان / قطعه",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "silver_999",
            titleFa = "نقره ساچمه ۹۹۹ (خالص)",
            titleEn = "Pure Silver 999",
            category = MarketCategory.SILVER,
            currentPriceToman = BigDecimal("501780"),
            changePercent24h = +0.35,
            unit = "تومان / گرم",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "silver_925",
            titleFa = "نقره استرلینگ ۹۲۵",
            titleEn = "Sterling Silver 925",
            category = MarketCategory.SILVER,
            currentPriceToman = BigDecimal("464150"),
            changePercent24h = +0.30,
            unit = "تومان / گرم",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "usd_irr",
            titleFa = "دلار بازار آزاد",
            titleEn = "USD / Toman",
            category = MarketCategory.CURRENCY,
            currentPriceToman = BigDecimal("263900"),
            changePercent24h = +0.40,
            unit = "تومان",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "ounce_gold",
            titleFa = "اونس جهانی طلا",
            titleEn = "Gold Ounce",
            category = MarketCategory.CURRENCY,
            currentPriceToman = BigDecimal("4149"), // in USD
            changePercent24h = +0.82,
            unit = "دلار / اونس",
            lastUpdated = System.currentTimeMillis() - 1800000L
        ),
        MarketPriceItem(
            id = "ounce_silver",
            titleFa = "اونس جهانی نقره",
            titleEn = "Silver Ounce",
            category = MarketCategory.CURRENCY,
            currentPriceToman = BigDecimal("56"), // in USD
            changePercent24h = -0.10,
            unit = "دلار / اونس",
            lastUpdated = System.currentTimeMillis() - 1800000L
        )
    )

    private val _marketState = MutableStateFlow<MarketDataState>(MarketDataState.Loading)
    val marketState: StateFlow<MarketDataState> = _marketState.asStateFlow()

    init {
        loadCachedOrBaseline()
    }

    private fun loadCachedOrBaseline() {
        val cachedItems = mutableListOf<MarketPriceItem>()
        var lastUpdated = sharedPrefs.getLong("cache_time", 0L)
        var hasManual = false

        for (base in baselineItems) {
            val isManual = sharedPrefs.getBoolean("manual_${base.id}", false)
            val savedPriceStr = sharedPrefs.getString("price_${base.id}", null)
            val currentPrice = if (savedPriceStr != null) {
                try {
                    val p = BigDecimal(savedPriceStr)
                    // If cached price is from old outdated scale (e.g. 18k < 10M), reset to updated baseline
                    if (base.id == "gold_18k" && p < BigDecimal("10000000") && !isManual) {
                        base.currentPriceToman
                    } else if (base.id == "usd_irr" && p < BigDecimal("100000") && !isManual) {
                        base.currentPriceToman
                    } else {
                        p
                    }
                } catch (_: Exception) {
                    base.currentPriceToman
                }
            } else {
                base.currentPriceToman
            }

            if (isManual) hasManual = true

            cachedItems.add(
                base.copy(
                    currentPriceToman = currentPrice,
                    isManualOverride = isManual,
                    isLive = !isManual && (System.currentTimeMillis() - lastUpdated < 4 * 3600 * 1000)
                )
            )
        }

        if (lastUpdated == 0L) {
            lastUpdated = System.currentTimeMillis() - 1800000L
        }

        val isStale = (System.currentTimeMillis() - lastUpdated) > (24 * 3600 * 1000)
        _marketState.value = MarketDataState.Success(
            items = cachedItems,
            lastUpdated = lastUpdated,
            isStale = isStale,
            isOffline = false,
            hasManualOverrides = hasManual
        )
    }

    /**
     * Refreshes market prices from official Iranian gold and currency live feeds (TGJU / Iranian open APIs).
     */
    suspend fun refreshPrices(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var fetchedSuccess = false
            val currentList = (_marketState.value as? MarketDataState.Success)?.items ?: baselineItems
            val priceMap = mutableMapOf<String, BigDecimal>()
            val changeMap = mutableMapOf<String, Double>()

            // 1. Fetch live Iranian market data from official TGJU ajax endpoint
            try {
                val request = Request.Builder()
                    .url("https://call.tgju.org/ajax.json")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                    .header("Accept", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val current = json.optJSONObject("current")
                        if (current != null) {
                            // Parse all supported symbols first. A non-null `current` object alone
                            // does NOT mean that usable market prices were received.

                            // 18K Gold (tgju_gold_irg18) -> Rials to Tomans
                            parseTgjuItem(current, "tgju_gold_irg18")?.let { (p, dp) ->
                                priceMap["gold_18k"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["gold_18k"] = it }
                            }

                            // 24K Gold (geram24) -> Rials to Tomans
                            parseTgjuItem(current, "geram24")?.let { (p, dp) ->
                                priceMap["gold_24k"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["gold_24k"] = it }
                            }

                            // Mesghal (mesghal) -> Rials to Tomans
                            parseTgjuItem(current, "mesghal")?.let { (p, dp) ->
                                priceMap["gold_mesghal"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["gold_mesghal"] = it }
                            }

                            // Emami Coin (sekee) -> Rials to Tomans
                            parseTgjuItem(current, "sekee")?.let { (p, dp) ->
                                priceMap["coin_emami"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["coin_emami"] = it }
                            }

                            // Silver 999 (silver_999) -> Rials to Tomans
                            parseTgjuItem(current, "silver_999")?.let { (p, dp) ->
                                priceMap["silver_999"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["silver_999"] = it }
                            }

                            // Silver 925 (silver_925) -> Rials to Tomans
                            parseTgjuItem(current, "silver_925")?.let { (p, dp) ->
                                priceMap["silver_925"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["silver_925"] = it }
                            }

                            // Free Market USD (price_dollar_rl) -> Rials to Tomans
                            parseTgjuItem(current, "price_dollar_rl")?.let { (p, dp) ->
                                priceMap["usd_irr"] = BigDecimal((p / 10).toLong())
                                dp?.let { changeMap["usd_irr"] = it }
                            }

                            // Gold Ounce (ons) -> in USD
                            parseTgjuItem(current, "ons")?.let { (p, dp) ->
                                priceMap["ounce_gold"] = BigDecimal(p.toInt())
                                dp?.let { changeMap["ounce_gold"] = it }
                            }

                            // Silver Ounce (silver) -> in USD
                            parseTgjuItem(current, "silver")?.let { (p, dp) ->
                                priceMap["ounce_silver"] = BigDecimal(p.toInt())
                                dp?.let { changeMap["ounce_silver"] = it }
                            }

                            // Consider refresh successful only when the key price shown on the
                            // home screen (18K gold) was actually received and is valid.
                            fetchedSuccess = priceMap["gold_18k"] != null
                        }
                    }
                }
            } catch (_: Exception) {
                // Network unreachable or timeout -> fallback to cached/baseline
            }

            val now = System.currentTimeMillis()
            val updatedItems = currentList.map { item ->
                if (item.isManualOverride) {
                    item // preserve user's manual customized price!
                } else {
                    val receivedPrice = priceMap[item.id]
                    if (receivedPrice != null) {
                        item.copy(
                            currentPriceToman = receivedPrice,
                            changePercent24h = changeMap[item.id] ?: item.changePercent24h,
                            lastUpdated = now,
                            isLive = true
                        )
                    } else {
                        // Do not label an old/fallback value as live or freshly updated.
                        item.copy(isLive = false)
                    }
                }
            }

            // Only advance the cache timestamp and save fetched values after a valid 18K
            // gold price was received. Never make stale prices look freshly updated.
            if (fetchedSuccess) {
                sharedPrefs.edit().apply {
                    putLong("cache_time", now)
                    for (item in updatedItems) {
                        if (priceMap.containsKey(item.id) || item.isManualOverride) {
                            putString("price_${item.id}", item.currentPriceToman.toPlainString())
                        }
                    }
                    apply()
                }
            }

            val previousCacheTime = sharedPrefs.getLong("cache_time", 0L)
            val effectiveLastUpdated = if (fetchedSuccess) now else previousCacheTime
            val effectiveStale = effectiveLastUpdated == 0L ||
                (now - effectiveLastUpdated) > 4 * 3600 * 1000L
            _marketState.value = MarketDataState.Success(
                items = updatedItems,
                lastUpdated = effectiveLastUpdated,
                isStale = effectiveStale,
                isOffline = !fetchedSuccess,
                hasManualOverrides = updatedItems.any { it.isManualOverride }
            )
            if (fetchedSuccess) Result.success(Unit)
            else Result.failure(IllegalStateException("TGJU returned no valid 18K gold price"))
        } catch (e: Exception) {
            val cached = (_marketState.value as? MarketDataState.Success)?.items ?: baselineItems
            _marketState.value = MarketDataState.Success(
                items = cached,
                lastUpdated = sharedPrefs.getLong("cache_time", System.currentTimeMillis() - 86400000L),
                isStale = true,
                isOffline = true,
                hasManualOverrides = cached.any { it.isManualOverride }
            )
            Result.failure(e)
        }
    }

    private fun parseTgjuItem(current: JSONObject, key: String): Pair<Double, Double?>? {
        val obj = current.optJSONObject(key) ?: return null
        val priceStr = obj.optString("p")
            .replace(",", "")
            .replace("٬", "")
            .trim()
        val price = priceStr.toDoubleOrNull() ?: return null
        if (!price.isFinite() || price <= 0.0) return null
        val dp = obj.optDouble("dp", Double.NaN)
        val change = if (dp.isNaN() || !dp.isFinite()) null else dp
        return Pair(price, change)
    }

    /**
     * Allows user to manually set a custom reference price for any item (e.g. today's local bazaar rate).
     */
    fun setManualPrice(itemId: String, manualPrice: BigDecimal) {
        val currentItems = (_marketState.value as? MarketDataState.Success)?.items ?: baselineItems
        val updated = currentItems.map {
            if (it.id == itemId) {
                it.copy(
                    currentPriceToman = manualPrice,
                    isManualOverride = true,
                    isLive = false,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                it
            }
        }

        sharedPrefs.edit().apply {
            putBoolean("manual_$itemId", true)
            putString("price_$itemId", manualPrice.toPlainString())
            apply()
        }

        _marketState.value = MarketDataState.Success(
            items = updated,
            lastUpdated = System.currentTimeMillis(),
            isStale = false,
            isOffline = false,
            hasManualOverrides = true
        )
    }

    /**
     * Resets a manual override back to live market price.
     */
    fun resetManualPrice(itemId: String) {
        sharedPrefs.edit().apply {
            remove("manual_$itemId")
            remove("price_$itemId")
            apply()
        }
        loadCachedOrBaseline()
    }

    /**
     * Resets all manual overrides.
     */
    fun resetAllManualOverrides() {
        for (item in baselineItems) {
            sharedPrefs.edit().remove("manual_${item.id}").remove("price_${item.id}")
        }
        sharedPrefs.edit().apply()
        loadCachedOrBaseline()
    }

    /**
     * Returns historical price points for the trend chart.
     */
    fun getHistoricalPoints(itemId: String, period: TimePeriod): List<PriceHistoryPoint> {
        val currentItem = (_marketState.value as? MarketDataState.Success)?.items?.find { it.id == itemId }
            ?: baselineItems.find { it.id == itemId }
            ?: baselineItems[0]

        val basePrice = currentItem.currentPriceToman.toDouble()
        val numPoints = when (period) {
            TimePeriod.DAY_1 -> 12
            TimePeriod.WEEK_1 -> 14
            TimePeriod.MONTH_1 -> 20
            TimePeriod.MONTH_3 -> 24
        }

        val stepMillis = (period.days * 86400000L) / numPoints
        val now = System.currentTimeMillis()
        val points = mutableListOf<PriceHistoryPoint>()

        val factors = listOf(
            -0.025, -0.018, -0.012, -0.008, -0.015, -0.005,
            +0.003, +0.008, +0.004, +0.012, +0.009, 0.000
        )

        for (i in 0 until numPoints) {
            val factorIdx = (i * factors.size) / numPoints
            val fluctuation = factors[factorIdx.coerceIn(0, factors.size - 1)]
            val price = basePrice * (1.0 + fluctuation)
            val time = now - (numPoints - 1 - i) * stepMillis
            points.add(PriceHistoryPoint(time, BigDecimal(price.toInt())))
        }
        return points
    }
}
