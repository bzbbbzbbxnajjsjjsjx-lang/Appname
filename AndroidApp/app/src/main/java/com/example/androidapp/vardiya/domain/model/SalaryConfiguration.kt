package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Domain model representing a user's salary and working schedule configuration.
 */
data class SalaryConfiguration(
    val monthlySalary: BigDecimal = BigDecimal("28000"),
    val monthlyWorkDays: Int = 22,
    val dailyWorkHours: BigDecimal = BigDecimal("8"),
    val breakMinutes: Int = 60,
    val deductBreakFromSalary: Boolean = false,
    val currencySymbol: String = "₺",
    val currencyCode: String = "TRY"
) {
    init {
        require(monthlySalary > BigDecimal.ZERO) { "Monthly salary must be positive" }
        require(monthlyWorkDays > 0) { "Monthly work days must be greater than 0" }
        require(dailyWorkHours > BigDecimal.ZERO) { "Daily work hours must be positive" }
        require(breakMinutes >= 0) { "Break minutes cannot be negative" }
    }

    /**
     * Effective daily paid hours after optional unpaid break deduction.
     */
    val dailyPaidHours: BigDecimal
        get() {
            return if (deductBreakFromSalary && breakMinutes > 0) {
                val breakHours = BigDecimal(breakMinutes).divide(BigDecimal(60), 6, RoundingMode.HALF_UP)
                val remaining = dailyWorkHours.subtract(breakHours)
                if (remaining > BigDecimal.ZERO) remaining else BigDecimal("0.1")
            } else {
                dailyWorkHours
            }
        }

    /**
     * Hourly earning rate in BigDecimal (Scale 10).
     */
    val hourlyRate: BigDecimal
        get() {
            val totalMonthlyHours = BigDecimal(monthlyWorkDays).multiply(dailyPaidHours)
            return monthlySalary.divide(totalMonthlyHours, 10, RoundingMode.HALF_UP)
        }

    /**
     * Per-minute earning rate in BigDecimal.
     */
    val minuteRate: BigDecimal
        get() = hourlyRate.divide(BigDecimal(60), 10, RoundingMode.HALF_UP)

    /**
     * Per-second earning rate in BigDecimal.
     */
    val secondRate: BigDecimal
        get() = hourlyRate.divide(BigDecimal(3600), 10, RoundingMode.HALF_UP)
}
