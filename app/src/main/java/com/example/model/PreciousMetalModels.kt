package com.example.model

import java.math.BigDecimal

enum class MetalType {
    GOLD,
    SILVER
}

enum class CalculationMode {
    SIMPLE,
    ADVANCED
}

enum class TaxBase(val titlePersian: String) {
    PROFIT_AND_OJRAT("فقط سود و اجرت (قانون جدید)"),
    TOTAL_AMOUNT("کل مبلغ (قانون قدیم)"),
    NONE("بدون مالیات")
}

enum class GoldPurity(val karat: Double, val finenessPerThousand: Int, val label: String) {
    K24(24.0, 1000, "۲۴ عیار (۱۰۰۰)"),
    K22(22.0, 916, "۲۲ عیار (۹۱۶)"),
    K21(21.0, 875, "۲۱ عیار (۸۷۵)"),
    K18(18.0, 750, "۱۸ عیار (۷۵۰)"),
    K14(14.0, 585, "۱۴ عیار (۵۸۵)"),
    K9(9.0, 375, "۹ عیار (۳۷۵)"),
    CUSTOM(-1.0, -1, "عیار دلخواه")
}

enum class SilverPurity(val fineness: Int, val label: String) {
    FINENESS_999(999, "۹۹۹ (خالص ساچمه)"),
    FINENESS_925(925, "۹۲۵ (استرلینگ)"),
    FINENESS_800(800, "۸۰۰ (ظروف و سنتی)"),
    CUSTOM(-1, "خلوص دلخواه")
}

data class GoldCalculationInput(
    val weightGrams: BigDecimal = BigDecimal.ZERO,
    val karat: Double = 18.0,
    val referencePricePerGram: BigDecimal = BigDecimal.ZERO,
    val referenceKarat: Double = 18.0, // usually 18 or 24
    val mode: CalculationMode = CalculationMode.SIMPLE,
    val isOjratPercentage: Boolean = true,
    val ojratValue: BigDecimal = BigDecimal.ZERO, // either % or fixed per gram
    val profitPercent: BigDecimal = BigDecimal("7.0"), // standard 7%
    val taxPercent: BigDecimal = BigDecimal("9.0"), // standard 9%
    val taxBase: TaxBase = TaxBase.PROFIT_AND_OJRAT,
    val additionalCosts: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO
)

data class GoldCalculationResult(
    val pureGoldWeight24k: BigDecimal,
    val rawMetalValue: BigDecimal,
    val ojratAmount: BigDecimal,
    val profitAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val additionalCosts: BigDecimal,
    val discount: BigDecimal,
    val totalAmount: BigDecimal
)

data class SilverCalculationInput(
    val weight: BigDecimal = BigDecimal.ZERO,
    val isTroyOunce: Boolean = false, // false = grams, true = oz
    val fineness: Int = 999,
    val referencePrice: BigDecimal = BigDecimal.ZERO, // per gram or per oz of 999
    val referenceIsTroyOunce: Boolean = false,
    val mode: CalculationMode = CalculationMode.SIMPLE,
    val isOjratPercentage: Boolean = true,
    val ojratValue: BigDecimal = BigDecimal.ZERO,
    val profitPercent: BigDecimal = BigDecimal("7.0"),
    val taxPercent: BigDecimal = BigDecimal("9.0"),
    val taxBase: TaxBase = TaxBase.PROFIT_AND_OJRAT,
    val additionalCosts: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO
)

data class SilverCalculationResult(
    val weightInGrams: BigDecimal,
    val pureSilverWeight999: BigDecimal,
    val rawMetalValue: BigDecimal,
    val ojratAmount: BigDecimal,
    val profitAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val additionalCosts: BigDecimal,
    val discount: BigDecimal,
    val totalAmount: BigDecimal
)
