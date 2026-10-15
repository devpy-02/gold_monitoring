package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.PreciousMetalCalculator
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.entity.CalculationRecordEntity
import com.example.data.repository.MarketDataState
import com.example.data.repository.MarketRepository
import com.example.model.CalculationMode
import com.example.model.GoldCalculationInput
import com.example.model.GoldCalculationResult
import com.example.model.GoldPurity
import com.example.model.MetalType
import com.example.model.SilverCalculationInput
import com.example.model.SilverCalculationResult
import com.example.model.SilverPurity
import com.example.model.TaxBase
import com.example.util.PersianUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CalculatorUiState(
    val activeMetal: MetalType = MetalType.GOLD,
    val calculationMode: CalculationMode = CalculationMode.SIMPLE,
    // Currency & formatting preference
    val isToman: Boolean = true,
    val usePersianDigits: Boolean = true,

    // Gold inputs
    val goldWeightInput: String = "4.25",
    val goldPuritySelection: GoldPurity = GoldPurity.K18,
    val customGoldKaratInput: String = "18",
    val goldRefPriceInput: String = "26045000",
    val goldRefBasisKarat: Double = 18.0,
    val goldIsOjratPercent: Boolean = true,
    val goldOjratInput: String = "12", // 12%
    val goldProfitInput: String = "7", // 7%
    val goldTaxInput: String = "9", // 9%
    val goldTaxBase: TaxBase = TaxBase.PROFIT_AND_OJRAT,
    val goldAdditionalCostsInput: String = "",
    val goldDiscountInput: String = "",
    val goldResult: GoldCalculationResult? = null,

    // Silver inputs
    val silverWeightInput: String = "25.0",
    val silverIsTroyOunce: Boolean = false,
    val silverPuritySelection: SilverPurity = SilverPurity.FINENESS_925,
    val customSilverFinenessInput: String = "925",
    val silverRefPriceInput: String = "501780", // 999 per gram
    val silverRefIsTroyOunce: Boolean = false,
    val silverIsOjratPercent: Boolean = true,
    val silverOjratInput: String = "15",
    val silverProfitInput: String = "7",
    val silverTaxInput: String = "9",
    val silverTaxBase: TaxBase = TaxBase.PROFIT_AND_OJRAT,
    val silverAdditionalCostsInput: String = "",
    val silverDiscountInput: String = "",
    val silverResult: SilverCalculationResult? = null
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val recordDao = db.calculationRecordDao()
    private val prefsRepo = AppPreferencesRepository(application)
    private val marketRepo = MarketRepository(application)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.userPreferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                isToman = prefs.isToman,
                usePersianDigits = prefs.usePersianDigits,
                calculationMode = if (prefs.isAdvancedModeDefault) CalculationMode.ADVANCED else CalculationMode.SIMPLE,
                goldProfitInput = prefs.defaultProfitPercent.toString(),
                goldTaxInput = prefs.defaultTaxPercent.toString()
            )
            recalculateAll()
        }
    }

    fun setMetal(metal: MetalType) {
        _uiState.value = _uiState.value.copy(activeMetal = metal)
        recalculateAll()
    }

    fun setCalculationMode(mode: CalculationMode) {
        _uiState.value = _uiState.value.copy(calculationMode = mode)
        recalculateAll()
    }

    // Gold setters
    fun setGoldWeight(value: String) {
        _uiState.value = _uiState.value.copy(goldWeightInput = value)
        recalculateGold()
    }

    fun setGoldPurity(purity: GoldPurity) {
        _uiState.value = _uiState.value.copy(goldPuritySelection = purity)
        recalculateGold()
    }

    fun setCustomGoldKarat(karat: String) {
        _uiState.value = _uiState.value.copy(customGoldKaratInput = karat)
        recalculateGold()
    }

    fun setGoldRefPrice(price: String) {
        _uiState.value = _uiState.value.copy(goldRefPriceInput = price)
        recalculateGold()
    }

    fun setGoldRefBasis(basisKarat: Double) {
        _uiState.value = _uiState.value.copy(goldRefBasisKarat = basisKarat)
        recalculateGold()
    }

    fun setGoldIsOjratPercent(isPercent: Boolean) {
        _uiState.value = _uiState.value.copy(goldIsOjratPercent = isPercent)
        recalculateGold()
    }

    fun setGoldOjrat(value: String) {
        _uiState.value = _uiState.value.copy(goldOjratInput = value)
        recalculateGold()
    }

    fun setGoldProfit(value: String) {
        _uiState.value = _uiState.value.copy(goldProfitInput = value)
        recalculateGold()
    }

    fun setGoldTax(value: String) {
        _uiState.value = _uiState.value.copy(goldTaxInput = value)
        recalculateGold()
    }

    fun setGoldTaxBase(base: TaxBase) {
        _uiState.value = _uiState.value.copy(goldTaxBase = base)
        recalculateGold()
    }

    fun setGoldAdditionalCosts(value: String) {
        _uiState.value = _uiState.value.copy(goldAdditionalCostsInput = value)
        recalculateGold()
    }

    fun setGoldDiscount(value: String) {
        _uiState.value = _uiState.value.copy(goldDiscountInput = value)
        recalculateGold()
    }

    // Silver setters
    fun setSilverWeight(value: String) {
        _uiState.value = _uiState.value.copy(silverWeightInput = value)
        recalculateSilver()
    }

    fun setSilverIsTroyOunce(isTroyOz: Boolean) {
        _uiState.value = _uiState.value.copy(silverIsTroyOunce = isTroyOz)
        recalculateSilver()
    }

    fun setSilverPurity(purity: SilverPurity) {
        _uiState.value = _uiState.value.copy(silverPuritySelection = purity)
        recalculateSilver()
    }

    fun setCustomSilverFineness(fineness: String) {
        _uiState.value = _uiState.value.copy(customSilverFinenessInput = fineness)
        recalculateSilver()
    }

    fun setSilverRefPrice(price: String) {
        _uiState.value = _uiState.value.copy(silverRefPriceInput = price)
        recalculateSilver()
    }

    fun setSilverIsOjratPercent(isPercent: Boolean) {
        _uiState.value = _uiState.value.copy(silverIsOjratPercent = isPercent)
        recalculateSilver()
    }

    fun setSilverOjrat(value: String) {
        _uiState.value = _uiState.value.copy(silverOjratInput = value)
        recalculateSilver()
    }

    fun setSilverProfit(value: String) {
        _uiState.value = _uiState.value.copy(silverProfitInput = value)
        recalculateSilver()
    }

    fun setSilverTax(value: String) {
        _uiState.value = _uiState.value.copy(silverTaxInput = value)
        recalculateSilver()
    }

    fun setSilverTaxBase(base: TaxBase) {
        _uiState.value = _uiState.value.copy(silverTaxBase = base)
        recalculateSilver()
    }

    fun setSilverAdditionalCosts(value: String) {
        _uiState.value = _uiState.value.copy(silverAdditionalCostsInput = value)
        recalculateSilver()
    }

    fun setSilverDiscount(value: String) {
        _uiState.value = _uiState.value.copy(silverDiscountInput = value)
        recalculateSilver()
    }

    /**
     * Copies the live reference price from market state to calculator inputs.
     */
    fun applyLiveMarketPrice() {
        val marketState = marketRepo.marketState.value
        if (marketState is MarketDataState.Success) {
            when (_uiState.value.activeMetal) {
                MetalType.GOLD -> {
                    val targetId = if (_uiState.value.goldRefBasisKarat == 24.0) "gold_24k" else "gold_18k"
                    val item = marketState.items.find { it.id == targetId }
                    if (item != null) {
                        _uiState.value = _uiState.value.copy(
                            goldRefPriceInput = item.currentPriceToman.toPlainString()
                        )
                        recalculateGold()
                        viewModelScope.launch {
                            _snackbarEvent.emit("قیمت روز ${item.titleFa} اعمال شد")
                        }
                    }
                }
                MetalType.SILVER -> {
                    val item = marketState.items.find { it.id == "silver_999" }
                    if (item != null) {
                        _uiState.value = _uiState.value.copy(
                            silverRefPriceInput = item.currentPriceToman.toPlainString()
                        )
                        recalculateSilver()
                        viewModelScope.launch {
                            _snackbarEvent.emit("قیمت روز نقره اعمال شد")
                        }
                    }
                }
            }
        }
    }

    fun resetForm() {
        if (_uiState.value.activeMetal == MetalType.GOLD) {
            _uiState.value = _uiState.value.copy(
                goldWeightInput = "",
                goldAdditionalCostsInput = "",
                goldDiscountInput = "",
                goldOjratInput = "0"
            )
            recalculateGold()
        } else {
            _uiState.value = _uiState.value.copy(
                silverWeightInput = "",
                silverAdditionalCostsInput = "",
                silverDiscountInput = "",
                silverOjratInput = "0"
            )
            recalculateSilver()
        }
    }

    fun saveCurrentCalculationToHistory() {
        viewModelScope.launch {
            val state = _uiState.value
            val record = if (state.activeMetal == MetalType.GOLD) {
                val res = state.goldResult ?: return@launch
                val karat = if (state.goldPuritySelection == GoldPurity.CUSTOM) {
                    "${state.customGoldKaratInput} عیار"
                } else {
                    state.goldPuritySelection.label
                }
                CalculationRecordEntity(
                    metalType = "GOLD",
                    weight = PersianUtils.parseBigDecimal(state.goldWeightInput).toDouble(),
                    unit = "گرم",
                    purityLabel = karat,
                    refPrice = PersianUtils.parseBigDecimal(state.goldRefPriceInput).toLong(),
                    rawMetalValue = res.rawMetalValue.toLong(),
                    ojratAmount = res.ojratAmount.toLong(),
                    profitAmount = res.profitAmount.toLong(),
                    taxAmount = res.taxAmount.toLong(),
                    additionalCosts = res.additionalCosts.toLong(),
                    discount = res.discount.toLong(),
                    totalAmount = res.totalAmount.toLong()
                )
            } else {
                val res = state.silverResult ?: return@launch
                val fineness = if (state.silverPuritySelection == SilverPurity.CUSTOM) {
                    "${state.customSilverFinenessInput} عیار"
                } else {
                    state.silverPuritySelection.label
                }
                CalculationRecordEntity(
                    metalType = "SILVER",
                    weight = PersianUtils.parseBigDecimal(state.silverWeightInput).toDouble(),
                    unit = if (state.silverIsTroyOunce) "اونس" else "گرم",
                    purityLabel = fineness,
                    refPrice = PersianUtils.parseBigDecimal(state.silverRefPriceInput).toLong(),
                    rawMetalValue = res.rawMetalValue.toLong(),
                    ojratAmount = res.ojratAmount.toLong(),
                    profitAmount = res.profitAmount.toLong(),
                    taxAmount = res.taxAmount.toLong(),
                    additionalCosts = res.additionalCosts.toLong(),
                    discount = res.discount.toLong(),
                    totalAmount = res.totalAmount.toLong()
                )
            }
            recordDao.insert(record)
            _snackbarEvent.emit("محاسبه در سوابق ذخیره شد")
        }
    }

    fun loadFromHistoryRecord(record: CalculationRecordEntity) {
        if (record.metalType == "GOLD") {
            _uiState.value = _uiState.value.copy(
                activeMetal = MetalType.GOLD,
                goldWeightInput = record.weight.toString(),
                goldRefPriceInput = record.refPrice.toString(),
                goldAdditionalCostsInput = record.additionalCosts.toString(),
                goldDiscountInput = record.discount.toString()
            )
            recalculateGold()
        } else {
            _uiState.value = _uiState.value.copy(
                activeMetal = MetalType.SILVER,
                silverWeightInput = record.weight.toString(),
                silverIsTroyOunce = record.unit == "اونس",
                silverRefPriceInput = record.refPrice.toString(),
                silverAdditionalCostsInput = record.additionalCosts.toString(),
                silverDiscountInput = record.discount.toString()
            )
            recalculateSilver()
        }
    }

    private fun recalculateAll() {
        recalculateGold()
        recalculateSilver()
    }

    private fun recalculateGold() {
        val state = _uiState.value
        val karatValue = if (state.goldPuritySelection == GoldPurity.CUSTOM) {
            state.customGoldKaratInput.toDoubleOrNull() ?: 18.0
        } else {
            state.goldPuritySelection.karat
        }

        val input = GoldCalculationInput(
            weightGrams = PersianUtils.parseBigDecimal(state.goldWeightInput),
            karat = karatValue,
            referencePricePerGram = PersianUtils.parseBigDecimal(state.goldRefPriceInput),
            referenceKarat = state.goldRefBasisKarat,
            mode = state.calculationMode,
            isOjratPercentage = state.goldIsOjratPercent,
            ojratValue = PersianUtils.parseBigDecimal(state.goldOjratInput),
            profitPercent = PersianUtils.parseBigDecimal(state.goldProfitInput),
            taxPercent = PersianUtils.parseBigDecimal(state.goldTaxInput),
            taxBase = state.goldTaxBase,
            additionalCosts = PersianUtils.parseBigDecimal(state.goldAdditionalCostsInput),
            discount = PersianUtils.parseBigDecimal(state.goldDiscountInput)
        )
        val result = PreciousMetalCalculator.calculateGold(input)
        _uiState.value = _uiState.value.copy(goldResult = result)
    }

    private fun recalculateSilver() {
        val state = _uiState.value
        val finenessValue = if (state.silverPuritySelection == SilverPurity.CUSTOM) {
            state.customSilverFinenessInput.toIntOrNull() ?: 925
        } else {
            state.silverPuritySelection.fineness
        }

        val input = SilverCalculationInput(
            weight = PersianUtils.parseBigDecimal(state.silverWeightInput),
            isTroyOunce = state.silverIsTroyOunce,
            fineness = finenessValue,
            referencePrice = PersianUtils.parseBigDecimal(state.silverRefPriceInput),
            referenceIsTroyOunce = state.silverRefIsTroyOunce,
            mode = state.calculationMode,
            isOjratPercentage = state.silverIsOjratPercent,
            ojratValue = PersianUtils.parseBigDecimal(state.silverOjratInput),
            profitPercent = PersianUtils.parseBigDecimal(state.silverProfitInput),
            taxPercent = PersianUtils.parseBigDecimal(state.silverTaxInput),
            taxBase = state.silverTaxBase,
            additionalCosts = PersianUtils.parseBigDecimal(state.silverAdditionalCostsInput),
            discount = PersianUtils.parseBigDecimal(state.silverDiscountInput)
        )
        val result = PreciousMetalCalculator.calculateSilver(input)
        _uiState.value = _uiState.value.copy(silverResult = result)
    }
}
