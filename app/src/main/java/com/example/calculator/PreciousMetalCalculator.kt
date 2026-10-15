package com.example.calculator

import com.example.model.CalculationMode
import com.example.model.GoldCalculationInput
import com.example.model.GoldCalculationResult
import com.example.model.SilverCalculationInput
import com.example.model.SilverCalculationResult
import com.example.model.TaxBase
import java.math.BigDecimal
import java.math.RoundingMode

object PreciousMetalCalculator {

    val TROY_OUNCE_TO_GRAMS: BigDecimal = BigDecimal("31.1034768")
    private val HUNDRED: BigDecimal = BigDecimal("100")
    private val SCALE: Int = 4

    /**
     * Calculates gold values with high precision BigDecimal arithmetic.
     */
    fun calculateGold(input: GoldCalculationInput): GoldCalculationResult {
        if (input.weightGrams <= BigDecimal.ZERO || input.referencePricePerGram <= BigDecimal.ZERO) {
            return GoldCalculationResult(
                pureGoldWeight24k = BigDecimal.ZERO,
                rawMetalValue = BigDecimal.ZERO,
                ojratAmount = BigDecimal.ZERO,
                profitAmount = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO,
                additionalCosts = BigDecimal.ZERO,
                discount = BigDecimal.ZERO,
                totalAmount = BigDecimal.ZERO
            )
        }

        val karat = BigDecimal.valueOf(input.karat.coerceIn(1.0, 24.0))
        val refKarat = BigDecimal.valueOf(input.referenceKarat.coerceIn(1.0, 24.0))

        // Pure 24K gold weight: weight * (karat / 24)
        val pureGoldWeight = input.weightGrams
            .multiply(karat)
            .divide(BigDecimal("24"), SCALE, RoundingMode.HALF_UP)

        // Raw metal value: weight * refPrice * karat / refKarat
        val rawMetalValue = input.weightGrams
            .multiply(input.referencePricePerGram)
            .multiply(karat)
            .divide(refKarat, SCALE, RoundingMode.HALF_UP)

        if (input.mode == CalculationMode.SIMPLE) {
            return GoldCalculationResult(
                pureGoldWeight24k = pureGoldWeight,
                rawMetalValue = rawMetalValue,
                ojratAmount = BigDecimal.ZERO,
                profitAmount = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO,
                additionalCosts = BigDecimal.ZERO,
                discount = BigDecimal.ZERO,
                totalAmount = rawMetalValue.setScale(0, RoundingMode.HALF_UP)
            )
        }

        // Advanced Mode Calculations:
        // 1. Making Charge (Ojrat)
        val ojratAmount = if (input.isOjratPercentage) {
            rawMetalValue.multiply(input.ojratValue).divide(HUNDRED, SCALE, RoundingMode.HALF_UP)
        } else {
            input.weightGrams.multiply(input.ojratValue).setScale(SCALE, RoundingMode.HALF_UP)
        }

        // 2. Seller Profit: (Raw Metal + Ojrat) * profitPercent / 100
        val profitBase = rawMetalValue.add(ojratAmount)
        val profitAmount = profitBase.multiply(input.profitPercent)
            .divide(HUNDRED, SCALE, RoundingMode.HALF_UP)

        // 3. Tax (VAT): based on taxBase
        val taxBaseAmount = when (input.taxBase) {
            TaxBase.PROFIT_AND_OJRAT -> ojratAmount.add(profitAmount)
            TaxBase.TOTAL_AMOUNT -> rawMetalValue.add(ojratAmount).add(profitAmount)
            TaxBase.NONE -> BigDecimal.ZERO
        }
        val taxAmount = taxBaseAmount.multiply(input.taxPercent)
            .divide(HUNDRED, SCALE, RoundingMode.HALF_UP)

        // 4. Additional costs & Discounts
        val additionalCosts = input.additionalCosts.max(BigDecimal.ZERO)
        val discount = input.discount.max(BigDecimal.ZERO)

        // Final total: Raw + Ojrat + Profit + Tax + Costs - Discount
        val subtotal = rawMetalValue
            .add(ojratAmount)
            .add(profitAmount)
            .add(taxAmount)
            .add(additionalCosts)

        val totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO)

        return GoldCalculationResult(
            pureGoldWeight24k = pureGoldWeight,
            rawMetalValue = rawMetalValue,
            ojratAmount = ojratAmount,
            profitAmount = profitAmount,
            taxAmount = taxAmount,
            additionalCosts = additionalCosts,
            discount = discount,
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP)
        )
    }

    /**
     * Calculates silver values with unit conversion (grams vs troy ounces).
     */
    fun calculateSilver(input: SilverCalculationInput): SilverCalculationResult {
        if (input.weight <= BigDecimal.ZERO || input.referencePrice <= BigDecimal.ZERO) {
            return SilverCalculationResult(
                weightInGrams = BigDecimal.ZERO,
                pureSilverWeight999 = BigDecimal.ZERO,
                rawMetalValue = BigDecimal.ZERO,
                ojratAmount = BigDecimal.ZERO,
                profitAmount = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO,
                additionalCosts = BigDecimal.ZERO,
                discount = BigDecimal.ZERO,
                totalAmount = BigDecimal.ZERO
            )
        }

        // Standardize weight to grams
        val weightInGrams = if (input.isTroyOunce) {
            input.weight.multiply(TROY_OUNCE_TO_GRAMS).setScale(SCALE, RoundingMode.HALF_UP)
        } else {
            input.weight
        }

        // Standardize reference price to price per gram of 999
        val refPricePerGram = if (input.referenceIsTroyOunce) {
            input.referencePrice.divide(TROY_OUNCE_TO_GRAMS, SCALE, RoundingMode.HALF_UP)
        } else {
            input.referencePrice
        }

        val fineness = BigDecimal.valueOf(input.fineness.coerceIn(1, 1000).toLong())
        val pureFineness = BigDecimal("999")

        // Pure 999 weight
        val pureWeight = weightInGrams.multiply(fineness)
            .divide(pureFineness, SCALE, RoundingMode.HALF_UP)

        // Raw metal value: weightInGrams * refPricePerGram * fineness / 999
        val rawMetalValue = weightInGrams
            .multiply(refPricePerGram)
            .multiply(fineness)
            .divide(pureFineness, SCALE, RoundingMode.HALF_UP)

        if (input.mode == CalculationMode.SIMPLE) {
            return SilverCalculationResult(
                weightInGrams = weightInGrams,
                pureSilverWeight999 = pureWeight,
                rawMetalValue = rawMetalValue,
                ojratAmount = BigDecimal.ZERO,
                profitAmount = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO,
                additionalCosts = BigDecimal.ZERO,
                discount = BigDecimal.ZERO,
                totalAmount = rawMetalValue.setScale(0, RoundingMode.HALF_UP)
            )
        }

        // Advanced Mode
        val ojratAmount = if (input.isOjratPercentage) {
            rawMetalValue.multiply(input.ojratValue).divide(HUNDRED, SCALE, RoundingMode.HALF_UP)
        } else {
            weightInGrams.multiply(input.ojratValue).setScale(SCALE, RoundingMode.HALF_UP)
        }

        val profitBase = rawMetalValue.add(ojratAmount)
        val profitAmount = profitBase.multiply(input.profitPercent)
            .divide(HUNDRED, SCALE, RoundingMode.HALF_UP)

        val taxBaseAmount = when (input.taxBase) {
            TaxBase.PROFIT_AND_OJRAT -> ojratAmount.add(profitAmount)
            TaxBase.TOTAL_AMOUNT -> rawMetalValue.add(ojratAmount).add(profitAmount)
            TaxBase.NONE -> BigDecimal.ZERO
        }
        val taxAmount = taxBaseAmount.multiply(input.taxPercent)
            .divide(HUNDRED, SCALE, RoundingMode.HALF_UP)

        val additionalCosts = input.additionalCosts.max(BigDecimal.ZERO)
        val discount = input.discount.max(BigDecimal.ZERO)

        val subtotal = rawMetalValue
            .add(ojratAmount)
            .add(profitAmount)
            .add(taxAmount)
            .add(additionalCosts)

        val totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO)

        return SilverCalculationResult(
            weightInGrams = weightInGrams,
            pureSilverWeight999 = pureWeight,
            rawMetalValue = rawMetalValue,
            ojratAmount = ojratAmount,
            profitAmount = profitAmount,
            taxAmount = taxAmount,
            additionalCosts = additionalCosts,
            discount = discount,
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP)
        )
    }
}
