package com.example.androidapp.vardiya.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class SalaryConfigurationTest {

    @Test
    fun testDefaultSalaryRateCalculations() {
        // 28,000 ₺, 22 days, 8 hours/day
        // Total hours = 176
        // Hourly rate = 28,000 / 176 = 159.0909090909...
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 60,
            deductBreakFromSalary = false
        )

        val hourly = config.hourlyRate.setScale(4, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("159.0909"), hourly)

        val minute = config.minuteRate.setScale(4, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("2.6515"), minute)

        val second = config.secondRate.setScale(5, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("0.04419"), second)
    }

    @Test
    fun testDeductBreakFromSalaryCalculations() {
        // 28,000 ₺, 22 days, 8 hours - 60 min break = 7 paid hours/day
        // Total monthly paid hours = 22 * 7 = 154
        // Hourly rate = 28,000 / 154 = 181.8181818182...
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 60,
            deductBreakFromSalary = true
        )

        assertEquals(BigDecimal("7.000000"), config.dailyPaidHours)

        val hourly = config.hourlyRate.setScale(4, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("181.8182"), hourly)
    }

    @Test
    fun testPrecisionAvoidsFloatingPointDrift() {
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("33333.33"),
            monthlyWorkDays = 21,
            dailyWorkHours = BigDecimal("7.5")
        )

        // Must not throw and maintain high precision scale
        assertTrue(config.hourlyRate.scale() >= 10)
        assertTrue(config.secondRate.scale() >= 10)
    }
}
