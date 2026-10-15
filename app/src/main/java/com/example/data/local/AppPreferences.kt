package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zarnegar_prefs")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isToman: Boolean = true,
    val usePersianDigits: Boolean = true,
    val isAdvancedModeDefault: Boolean = false,
    val defaultGoldKarat: Double = 18.0,
    val defaultSilverFineness: Int = 999,
    val defaultProfitPercent: Double = 7.0,
    val defaultTaxPercent: Double = 9.0,
    val refreshIntervalMinutes: Int = 5
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_TOMAN = booleanPreferencesKey("is_toman")
        val USE_PERSIAN_DIGITS = booleanPreferencesKey("use_persian_digits")
        val DEFAULT_ADVANCED_MODE = booleanPreferencesKey("default_advanced_mode")
        val DEFAULT_GOLD_KARAT = doublePreferencesKey("default_gold_karat")
        val DEFAULT_SILVER_FINENESS = intPreferencesKey("default_silver_fineness")
        val DEFAULT_PROFIT_PERCENT = doublePreferencesKey("default_profit_percent")
        val DEFAULT_TAX_PERCENT = doublePreferencesKey("default_tax_percent")
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeString = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeString)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        UserPreferences(
            themeMode = themeMode,
            isToman = prefs[Keys.IS_TOMAN] ?: true,
            usePersianDigits = prefs[Keys.USE_PERSIAN_DIGITS] ?: true,
            isAdvancedModeDefault = prefs[Keys.DEFAULT_ADVANCED_MODE] ?: false,
            defaultGoldKarat = prefs[Keys.DEFAULT_GOLD_KARAT] ?: 18.0,
            defaultSilverFineness = prefs[Keys.DEFAULT_SILVER_FINENESS] ?: 999,
            defaultProfitPercent = prefs[Keys.DEFAULT_PROFIT_PERCENT] ?: 7.0,
            defaultTaxPercent = prefs[Keys.DEFAULT_TAX_PERCENT] ?: 9.0,
            refreshIntervalMinutes = prefs[Keys.REFRESH_INTERVAL] ?: 5
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setIsToman(isToman: Boolean) {
        context.dataStore.edit { it[Keys.IS_TOMAN] = isToman }
    }

    suspend fun setUsePersianDigits(usePersian: Boolean) {
        context.dataStore.edit { it[Keys.USE_PERSIAN_DIGITS] = usePersian }
    }

    suspend fun setDefaultAdvancedMode(isAdvanced: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_ADVANCED_MODE] = isAdvanced }
    }

    suspend fun setDefaultGoldKarat(karat: Double) {
        context.dataStore.edit { it[Keys.DEFAULT_GOLD_KARAT] = karat }
    }

    suspend fun setDefaultSilverFineness(fineness: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SILVER_FINENESS] = fineness }
    }

    suspend fun setDefaultProfitPercent(profit: Double) {
        context.dataStore.edit { it[Keys.DEFAULT_PROFIT_PERCENT] = profit }
    }

    suspend fun setDefaultTaxPercent(tax: Double) {
        context.dataStore.edit { it[Keys.DEFAULT_TAX_PERCENT] = tax }
    }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REFRESH_INTERVAL] = minutes }
    }
}
