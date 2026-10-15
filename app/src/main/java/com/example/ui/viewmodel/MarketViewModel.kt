package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferencesRepository
import com.example.data.repository.MarketDataState
import com.example.data.repository.MarketRepository
import com.example.model.PriceHistoryPoint
import com.example.model.TimePeriod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class MarketUiState(
    val marketDataState: MarketDataState = MarketDataState.Loading,
    val isRefreshing: Boolean = false,
    val selectedChartItemId: String = "gold_18k",
    val selectedChartPeriod: TimePeriod = TimePeriod.WEEK_1,
    val chartPoints: List<PriceHistoryPoint> = emptyList(),
    val editingItemId: String? = null,
    val isToman: Boolean = true,
    val usePersianDigits: Boolean = true
)

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MarketRepository(application)
    private val prefsRepo = AppPreferencesRepository(application)

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            prefsRepo.userPreferencesFlow.collectLatest { prefs ->
                _uiState.value = _uiState.value.copy(
                    isToman = prefs.isToman,
                    usePersianDigits = prefs.usePersianDigits
                )
            }
        }

        viewModelScope.launch {
            repository.marketState.collectLatest { mState ->
                _uiState.value = _uiState.value.copy(marketDataState = mState)
                updateChartPoints()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val result = repository.refreshPrices()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
            if (result.isSuccess) {
                _snackbarEvent.emit("قیمت‌ها به‌روزرسانی شدند")
            } else {
                _snackbarEvent.emit("عدم دسترسی به اینترنت؛ نرخ‌های حافظه نمایش داده می‌شوند")
            }
        }
    }

    fun selectChartItem(itemId: String) {
        _uiState.value = _uiState.value.copy(selectedChartItemId = itemId)
        updateChartPoints()
    }

    fun selectChartPeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedChartPeriod = period)
        updateChartPoints()
    }

    private fun updateChartPoints() {
        val points = repository.getHistoricalPoints(
            _uiState.value.selectedChartItemId,
            _uiState.value.selectedChartPeriod
        )
        _uiState.value = _uiState.value.copy(chartPoints = points)
    }

    fun openEditPriceDialog(itemId: String) {
        _uiState.value = _uiState.value.copy(editingItemId = itemId)
    }

    fun dismissEditPriceDialog() {
        _uiState.value = _uiState.value.copy(editingItemId = null)
    }

    fun saveManualPrice(itemId: String, newPrice: BigDecimal) {
        repository.setManualPrice(itemId, newPrice)
        dismissEditPriceDialog()
        updateChartPoints()
        viewModelScope.launch {
            _snackbarEvent.emit("نرخ دستی با موفقیت ثبت شد")
        }
    }

    fun resetToLive(itemId: String) {
        repository.resetManualPrice(itemId)
        dismissEditPriceDialog()
        updateChartPoints()
        viewModelScope.launch {
            _snackbarEvent.emit("نرخ به قیمت زنده بازار بازگردانده شد")
        }
    }
}
