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

    @Test
    fun testOvertimeMultiplierValidation() {
        // Multiplier less than 1.0 rejected
        var result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            overtimeMultiplierText = "0.95"
        )
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.OVERTIME_MULTIPLIER, (result as SalaryValidationResult.Invalid).field)

        // Negative multiplier rejected
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            overtimeMultiplierText = "-1.50"
        )
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.OVERTIME_MULTIPLIER, (result as SalaryValidationResult.Invalid).field)

        // Blank multiplier rejected
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            overtimeMultiplierText = "  "
        )
        assertTrue(result is SalaryValidationResult.Invalid)

        // Non-numeric multiplier rejected
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            overtimeMultiplierText = "abc"
        )
        assertTrue(result is SalaryValidationResult.Invalid)

        // Multiplier >= 1.0 accepted (e.g. 1.0, 1.25, 1.50, 2.0)
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            overtimeMultiplierText = "1.0"
        )
        assertTrue(result is SalaryValidationResult.Valid)

        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            overtimeMultiplierText = "1,75"
        )
        assertTrue(result is SalaryValidationResult.Valid)
    }

    @Test
    fun testNightDifferentialRateValidation() {
        // Negative rate rejected
        var result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightDifferentialRateText = "-0.10"
        )
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.NIGHT_DIFFERENTIAL_RATE, (result as SalaryValidationResult.Invalid).field)

        // Blank rate rejected
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightDifferentialRateText = ""
        )
        assertTrue(result is SalaryValidationResult.Invalid)

        // Non-numeric rate rejected
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightDifferentialRateText = "xyz"
        )
        assertTrue(result is SalaryValidationResult.Invalid)

        // Zero rate accepted
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightDifferentialRateText = "0.0"
        )
        assertTrue(result is SalaryValidationResult.Valid)

        // Decimal and percent rates accepted
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightDifferentialRateText = "0,15"
        )
        assertTrue(result is SalaryValidationResult.Valid)

        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightDifferentialRateText = "15%"
        )
        assertTrue(result is SalaryValidationResult.Valid)
    }

    @Test
    fun testNightShiftHoursValidation() {
        // Out of bounds start hour
        var result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightShiftStartHour = -1
        )
        assertTrue(result is SalaryValidationResult.Invalid)
        assertEquals(SalaryValidationResult.Field.NIGHT_SHIFT_HOURS, (result as SalaryValidationResult.Invalid).field)

        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightShiftStartHour = 24
        )
        assertTrue(result is SalaryValidationResult.Invalid)

        // Out of bounds end hour
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightShiftEndHour = 25
        )
        assertTrue(result is SalaryValidationResult.Invalid)

        // Boundary values (0 and 23)
        result = SalaryConfigValidator.validate(
            "28000", "22", "8", "60", false,
            nightShiftStartHour = 0,
            nightShiftEndHour = 23
        )
        assertTrue(result is SalaryValidationResult.Valid)
    }

    @Test
    fun testParseOrNullWithPhaseHFeatures() {
        val parsed = SalaryConfigValidator.parseOrNull(
            salaryText = "35000",
            workDaysText = "20",
            workHoursText = "7.5",
            breakMinutesText = "45",
            deductBreak = true,
            overtimeMultiplierText = "1.75",
            isOvertimeEnabled = true,
            nightDifferentialRateText = "20%",
            isNightDifferentialEnabled = true,
            nightShiftStartHour = 21,
            nightShiftEndHour = 7
        )
        assertNotNull(parsed)
        assertEquals(BigDecimal("35000"), parsed?.monthlySalary)
        assertEquals(20, parsed?.monthlyWorkDays)
        assertEquals(BigDecimal("7.5"), parsed?.dailyWorkHours)
        assertEquals(45, parsed?.breakMinutes)
        assertEquals(true, parsed?.deductBreakFromSalary)
        assertEquals(BigDecimal("1.75"), parsed?.overtimeMultiplier)
        assertEquals(true, parsed?.isOvertimeEnabled)
        assertEquals(BigDecimal("0.2000"), parsed?.nightDifferentialRate)
        assertEquals(true, parsed?.isNightDifferentialEnabled)
        assertEquals(21, parsed?.nightShiftStartHour)
        assertEquals(7, parsed?.nightShiftEndHour)
    }
}
