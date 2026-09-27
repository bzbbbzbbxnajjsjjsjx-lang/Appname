package com.example.androidapp.vardiya.domain.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SalaryConfigValidatorTest {

    @Test
    fun testValidConfigurationPasses() {
        val result = SalaryConfigValidator.validate(
            salaryText = "28000",
            workDaysText = "22",
            workHoursText = "8",
            breakMinutesText = "60",
            deductBreak = false
        )
        assertTrue(result is SalaryValidationResult.Valid)

        val config = SalaryConfigValidator.parseOrNull(
            salaryText = "28000",
            workDaysText = "22",
            workHoursText = "8",
            breakMinutesText = "60",
            deductBreak = false
        )
        assertNotNull(config)
        assertEquals(BigDecimal("28000"), config?.monthlySalary)
        assertEquals(22, config?.monthlyWorkDays)
        assertEquals(BigDecimal("8"), config?.dailyWorkHours)
        assertEquals(60, config?.breakMinutes)
    }

    @Test
    fun testZeroAndNegativeSalaryRejected() {
        var result = SalaryConfigValidator.validate("0", "22", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.SALARY, (result as SalaryValidationResult.Invalid).field)

        result = SalaryConfigValidator.validate("-5000", "22", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.SALARY, (result as SalaryValidationResult.Invalid).field)

        result = SalaryConfigValidator.validate("", "22", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)

        assertNull(SalaryConfigValidator.parseOrNull("0", "22", "8", "60", false))
    }

    @Test
    fun testMalformedSalaryStringsRejected() {
        val result = SalaryConfigValidator.validate("abc", "22", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.SALARY, (result as SalaryValidationResult.Invalid).field)

        val resultNaN = SalaryConfigValidator.validate("NaN", "22", "8", "60", false)
        assertTrue(resultNaN is SalaryValidationResult.Invalid)

        val resultInf = SalaryConfigValidator.validate("Infinity", "22", "8", "60", false)
        assertTrue(resultInf is SalaryValidationResult.Invalid)
    }

    @Test
    fun testWorkDaysValidation() {
        // Zero work days
        var result = SalaryConfigValidator.validate("28000", "0", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.WORK_DAYS, (result as SalaryValidationResult.Invalid).field)

        // Negative work days
        result = SalaryConfigValidator.validate("28000", "-5", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)

        // Work days > 31
        result = SalaryConfigValidator.validate("28000", "32", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)

        // Non-integer work days
        result = SalaryConfigValidator.validate("28000", "22.5", "8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)
    }

    @Test
    fun testDailyWorkHoursValidation() {
        // Zero hours
        var result = SalaryConfigValidator.validate("28000", "22", "0", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.WORK_HOURS, (result as SalaryValidationResult.Invalid).field)

        // Negative hours
        result = SalaryConfigValidator.validate("28000", "22", "-8", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)

        // Over 24 hours
        result = SalaryConfigValidator.validate("28000", "22", "25", "60", false)
        assertTrue(result is SalaryValidationResult.Invalid)

        // Valid decimal hours (e.g. 7.5 or 7,5)
        result = SalaryConfigValidator.validate("28000", "22", "7,5", "45", false)
        assertTrue(result is SalaryValidationResult.Valid)
    }

    @Test
    fun testBreakMinutesValidation() {
        // Negative break
        var result = SalaryConfigValidator.validate("28000", "22", "8", "-10", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.BREAK_MINUTES, (result as SalaryValidationResult.Invalid).field)

        // Break equals daily work hours (8 hours = 480 minutes)
        result = SalaryConfigValidator.validate("28000", "22", "8", "480", false)
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.BREAK_MINUTES, (result as SalaryValidationResult.Invalid).field)

        // Break exceeds daily work hours
        result = SalaryConfigValidator.validate("28000", "22", "8", "500", false)
        assertTrue(result is SalaryValidationResult.Invalid)

        // Valid break
        result = SalaryConfigValidator.validate("28000", "22", "8", "45", false)
        assertTrue(result is SalaryValidationResult.Valid)
    }
}
