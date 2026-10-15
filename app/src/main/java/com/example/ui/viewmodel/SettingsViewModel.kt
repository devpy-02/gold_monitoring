package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = AppPreferencesRepository(application)

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            prefsRepo.userPreferencesFlow.collectLatest { prefs ->
                _preferences.value = prefs
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
    }

    fun setIsToman(isToman: Boolean) {
        viewModelScope.launch {
            prefsRepo.setIsToman(isToman)
            _snackbarEvent.emit(if (isToman) "واحد پول به تومان تغییر یافت" else "واحد پول به ریال تغییر یافت")
        }
    }

    fun setUsePersianDigits(usePersian: Boolean) {
        viewModelScope.launch {
            prefsRepo.setUsePersianDigits(usePersian)
        }
    }

    fun setDefaultAdvancedMode(isAdvanced: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDefaultAdvancedMode(isAdvanced)
        }
    }

    fun setDefaultProfitPercent(profit: Double) {
        viewModelScope.launch {
            prefsRepo.setDefaultProfitPercent(profit)
        }
    }

    fun setDefaultTaxPercent(tax: Double) {
        viewModelScope.launch {
            prefsRepo.setDefaultTaxPercent(tax)
        }
    }

    fun setRefreshInterval(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.setRefreshIntervalMinutes(minutes)
        }
    }
}
