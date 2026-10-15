package com.example

import com.example.util.PersianUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PersianUtilsTest {

    @Test
    fun testToEnglishDigits() {
        assertEquals("1234567890", PersianUtils.toEnglishDigits("۱۲۳۴۵۶۷۸۹۰"))
        assertEquals("1234.56", PersianUtils.toEnglishDigits("۱۲۳۴٫۵۶"))
        assertEquals("987654.32", PersianUtils.toEnglishDigits("۹۸۷۶۵۴/۳۲"))
    }

    @Test
    fun testToPersianDigits() {
        assertEquals("۱۲۳۴۵۶۷۸۹۰", PersianUtils.toPersianDigits("1234567890"))
        assertEquals("۱٬۲۳۴٬۵۶۷", PersianUtils.toPersianDigits("1,234,567"))
    }

    @Test
    fun testCleanNumericInput() {
        assertEquals("1250000", PersianUtils.cleanNumericInput("۱,۲۵۰,۰۰۰"))
        assertEquals("12.5", PersianUtils.cleanNumericInput("۱۲.۵"))
        assertEquals("12.5", PersianUtils.cleanNumericInput("۱۲/۵"))
        assertEquals("12.5", PersianUtils.cleanNumericInput("۱۲٫۵"))
    }

    @Test
    fun testFormatMoney() {
        val amount = BigDecimal("2500000")
        val formatted = PersianUtils.formatMoney(amount, isToman = true, usePersianDigits = true, includeUnit = true)
        assertEquals("۲٬۵۰۰٬۰۰۰ تومان", formatted)

        val formattedEnglish = PersianUtils.formatMoney(amount, isToman = true, usePersianDigits = false, includeUnit = true)
        assertEquals("2,500,000 تومان", formattedEnglish)
    }

    @Test
    fun testFormatWeight() {
        val weight = BigDecimal("14.65")
        val formatted = PersianUtils.formatWeight(weight, usePersianDigits = true, includeUnit = true)
        assertEquals("۱۴.۶۵ گرم", formatted)
    }
}
