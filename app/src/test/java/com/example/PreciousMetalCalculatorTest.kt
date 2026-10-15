package com.example

import com.example.calculator.PreciousMetalCalculator
import com.example.model.CalculationMode
import com.example.model.GoldCalculationInput
import com.example.model.SilverCalculationInput
import com.example.model.TaxBase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class PreciousMetalCalculatorTest {

    @Test
    fun testGoldSimpleCalculation() {
        // 10 grams of 18K gold at 4,000,000 Toman per gram of 18K
        val input = GoldCalculationInput(
            weightGrams = BigDecimal("10.0"),
            karat = 18.0,
            referencePricePerGram = BigDecimal("4000000"),
            referenceKarat = 18.0,
            mode = CalculationMode.SIMPLE
        )

        val result = PreciousMetalCalculator.calculateGold(input)

        // Raw metal value should be 40,000,000
        assertEquals(BigDecimal("40000000"), result.totalAmount)
        assertEquals(BigDecimal("40000000.0000"), result.rawMetalValue)
        // 10g of 18K equals 7.5g of pure 24K gold (10 * 18 / 24)
        assertEquals(BigDecimal("7.5000"), result.pureGoldWeight24k)
    }

    @Test
    fun testGoldKaratConversion() {
        // 10 grams of 24K gold with reference price given for 18K
        // 4,000,000 * (24 / 18) = 5,333,333.3333 per gram -> 10g = 53,333,333
        val input = GoldCalculationInput(
            weightGrams = BigDecimal("10.0"),
            karat = 24.0,
            referencePricePerGram = BigDecimal("4000000"),
            referenceKarat = 18.0,
            mode = CalculationMode.SIMPLE
        )

        val result = PreciousMetalCalculator.calculateGold(input)
        assertEquals(BigDecimal("53333333"), result.totalAmount)
        assertEquals(BigDecimal("10.0000"), result.pureGoldWeight24k)
    }

    @Test
    fun testGoldAdvancedIranianFormula() {
        // Raw metal value = 10,000,000
        // Ojrat = 10% -> 1,000,000
        // Base for profit = 11,000,000 -> 7% profit = 770,000
        // Iranian tax law: VAT only on (Ojrat + Profit) = 1,770,000 -> 9% tax = 159,300
        // Total = 10,000,000 + 1,000,000 + 770,000 + 159,300 = 11,929,300
        val input = GoldCalculationInput(
            weightGrams = BigDecimal("2.5"),
            karat = 18.0,
            referencePricePerGram = BigDecimal("4000000"), // 2.5 * 4,000,000 = 10,000,000
            referenceKarat = 18.0,
            mode = CalculationMode.ADVANCED,
            isOjratPercentage = true,
            ojratValue = BigDecimal("10.0"), // 10%
            profitPercent = BigDecimal("7.0"), // 7%
            taxPercent = BigDecimal("9.0"), // 9%
            taxBase = TaxBase.PROFIT_AND_OJRAT,
            additionalCosts = BigDecimal.ZERO,
            discount = BigDecimal.ZERO
        )

        val result = PreciousMetalCalculator.calculateGold(input)

        assertEquals(BigDecimal("10000000.0000"), result.rawMetalValue)
        assertEquals(BigDecimal("1000000.0000"), result.ojratAmount)
        assertEquals(BigDecimal("770000.0000"), result.profitAmount)
        assertEquals(BigDecimal("159300.0000"), result.taxAmount)
        assertEquals(BigDecimal("11929300"), result.totalAmount)
    }

    @Test
    fun testSilverCalculationGrams() {
        // 100 grams of 925 sterling silver at 70,000 per gram of 999
        // 100 * 70,000 * (925 / 999) = 6,481,481.48
        val input = SilverCalculationInput(
            weight = BigDecimal("100.0"),
            isTroyOunce = false,
            fineness = 925,
            referencePrice = BigDecimal("70000"),
            referenceIsTroyOunce = false,
            mode = CalculationMode.SIMPLE
        )

        val result = PreciousMetalCalculator.calculateSilver(input)
        assertEquals(BigDecimal("100.0"), result.weightInGrams)
        assertEquals(BigDecimal("6481481"), result.totalAmount)
    }
}
