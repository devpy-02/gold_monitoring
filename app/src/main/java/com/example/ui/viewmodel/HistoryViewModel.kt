package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.entity.CalculationRecordEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<CalculationRecordEntity> = emptyList(),
    val isToman: Boolean = true,
    val usePersianDigits: Boolean = true,
    val showClearAllDialog: Boolean = false
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.calculationRecordDao()
    private val prefsRepo = AppPreferencesRepository(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

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
            dao.getAllRecords().collectLatest { list ->
                _uiState.value = _uiState.value.copy(records = list)
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
            _snackbarEvent.emit("محاسبه حذف شد")
        }
    }

    fun showClearConfirmDialog() {
        _uiState.value = _uiState.value.copy(showClearAllDialog = true)
    }

    fun dismissClearConfirmDialog() {
        _uiState.value = _uiState.value.copy(showClearAllDialog = false)
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            dao.deleteAll()
            dismissClearConfirmDialog()
            _snackbarEvent.emit("تمام سوابق پاک شدند")
        }
    }
}
