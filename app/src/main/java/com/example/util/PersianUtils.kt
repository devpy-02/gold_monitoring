package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PersianUtils {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Converts any Persian or Arabic digits in the input string to standard Latin/English digits.
     */
    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                ch in '۰'..'۹' -> sb.append((ch - '۰'))
                ch in '٠'..'٩' -> sb.append((ch - '٠'))
                ch == '٫' || ch == '/' -> sb.append('.')
                ch == '٬' -> sb.append(',')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Latin digits '0'..'9' in the input string to Persian digits '۰'..'۹'.
     */
    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(PERSIAN_DIGITS[ch - '0'])
            } else if (ch == ',') {
                sb.append('٬')
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Cleans an input string for numeric use:
     * - Converts Persian digits to English
     * - Removes grouping separators
     * - Allows only digits and at most one decimal point
     */
    fun cleanNumericInput(input: String, allowDecimal: Boolean = true): String {
        val english = toEnglishDigits(input)
        val sb = StringBuilder()
        var hasDecimal = false

        for (ch in english) {
            if (ch in '0'..'9') {
                sb.append(ch)
            } else if (allowDecimal && ch == '.' && !hasDecimal) {
                sb.append('.')
                hasDecimal = true
            }
        }
        return sb.toString()
    }

    /**
     * Formats a BigDecimal amount into formatted currency (with thousands separators).
     */
    fun formatMoney(
        amount: BigDecimal,
        isToman: Boolean = true,
        usePersianDigits: Boolean = true,
        includeUnit: Boolean = true
    ): String {
        // Round to nearest integer for currency display in Toman / Rial
        val rounded = amount.setScale(0, RoundingMode.HALF_UP)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val df = DecimalFormat("#,##0", symbols)
        val formatted = df.format(rounded)
        val withDigits = if (usePersianDigits) toPersianDigits(formatted) else formatted
        return if (includeUnit) {
            val unit = if (isToman) "تومان" else "ریال"
            "$withDigits $unit"
        } else {
            withDigits
        }
    }

    /**
     * Formats decimal weights (e.g. grams, ounces) cleanly up to 3 decimal places.
     */
    fun formatWeight(
        weight: BigDecimal,
        usePersianDigits: Boolean = true,
        includeUnit: Boolean = true
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.###", symbols)
        val formatted = df.format(weight)
        val withDigits = if (usePersianDigits) toPersianDigits(formatted) else formatted
        return if (includeUnit) {
            "$withDigits گرم"
        } else {
            withDigits
        }
    }

    /**
     * Safe conversion of string to BigDecimal with scale.
     */
    fun parseBigDecimal(input: String): BigDecimal {
        val cleaned = cleanNumericInput(input, allowDecimal = true)
        return try {
            if (cleaned.isBlank() || cleaned == ".") BigDecimal.ZERO else BigDecimal(cleaned)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }
}

/**
 * VisualTransformation for dynamic live formatting of numeric inputs in Jetpack Compose:
 * - Inserts thousands separators in real-time as the user types
 * - Displays Persian digits (or English digits based on settings)
 * - Accurately maps cursor offsets bi-directionally to preserve cursor position
 *   during middle-text edits, typing, and backspacing.
 */
class PersianNumberVisualTransformation(
    private val usePersianDigits: Boolean = true,
    private val allowDecimal: Boolean = true
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Clean english digits
        val rawEnglish = PersianUtils.toEnglishDigits(originalText)

        // Split integer and decimal parts
        val parts = rawEnglish.split('.', limit = 2)
        val integerPart = parts[0]
        val hasDecimal = parts.size > 1
        val decimalPart = if (hasDecimal) parts[1] else ""

        // Build formatted string and pre-calculate offset mappings
        val formattedIntegerBuilder = StringBuilder()
        val originalIntLen = integerPart.length

        // Array mapping original index in integerPart -> transformed index
        val intOrigToTrans = IntArray(originalIntLen + 1)

        var transIndex = 0
        for (i in 0 until originalIntLen) {
            intOrigToTrans[i] = transIndex
            val ch = integerPart[i]
            if (ch in '0'..'9') {
                formattedIntegerBuilder.append(
                    if (usePersianDigits) PersianUtils.toPersianDigits(ch.toString()) else ch
                )
                transIndex++
                val digitsRemaining = originalIntLen - 1 - i
                if (digitsRemaining > 0 && digitsRemaining % 3 == 0) {
                    formattedIntegerBuilder.append(if (usePersianDigits) '٬' else ',')
                    transIndex++
                }
            } else {
                formattedIntegerBuilder.append(ch)
                transIndex++
            }
        }
        intOrigToTrans[originalIntLen] = transIndex

        val fullTransformed = StringBuilder(formattedIntegerBuilder.toString())
        if (hasDecimal) {
            fullTransformed.append('.')
            fullTransformed.append(
                if (usePersianDigits) PersianUtils.toPersianDigits(decimalPart) else decimalPart
            )
        }

        val transformedString = fullTransformed.toString()

        // Create bi-directional offset mapping table
        val origToTrans = IntArray(originalText.length + 1)
        val transToOrig = IntArray(transformedString.length + 1)

        // Fill integer part offsets
        for (i in 0..originalIntLen) {
            origToTrans[i] = intOrigToTrans[i]
        }

        // Fill decimal part offsets if present
        if (hasDecimal) {
            val decimalSeparatorOrigIndex = originalIntLen
            val decimalSeparatorTransIndex = intOrigToTrans[originalIntLen]
            origToTrans[decimalSeparatorOrigIndex] = decimalSeparatorTransIndex

            for (d in 0..decimalPart.length) {
                val origIdx = originalIntLen + 1 + d
                val transIdx = decimalSeparatorTransIndex + 1 + d
                if (origIdx <= originalText.length) {
                    origToTrans[origIdx] = transIdx.coerceAtMost(transformedString.length)
                }
            }
        }

        // Compute reverse mapping
        var currentOrig = 0
        for (t in 0..transformedString.length) {
            while (currentOrig < originalText.length && origToTrans[currentOrig + 1] <= t) {
                currentOrig++
            }
            transToOrig[t] = currentOrig
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, originalText.length)
                return origToTrans[safeOffset].coerceIn(0, transformedString.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, transformedString.length)
                return transToOrig[safeOffset].coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }
}
