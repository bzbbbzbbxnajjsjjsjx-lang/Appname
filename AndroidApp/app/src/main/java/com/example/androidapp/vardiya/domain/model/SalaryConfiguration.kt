package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Domain model representing a user's salary and working schedule configuration.
 * All rate calculations maintain 16 fractional digits to completely eliminate floating-point drift.
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
    val breakIsPaid: Boolean
        get() = !deductBreakFromSalary

    init {
        require(monthlySalary > BigDecimal.ZERO) { "Monthly salary must be positive" }
        require(monthlyWorkDays in 1..31) { "Monthly work days must be between 1 and 31" }
        require(dailyWorkHours > BigDecimal.ZERO && dailyWorkHours <= BigDecimal("24")) {
            "Daily work hours must be positive and at most 24"
        }
        require(breakMinutes >= 0) { "Break minutes cannot be negative" }
        val breakHours = BigDecimal(breakMinutes).divide(BigDecimal(60), 6, RoundingMode.HALF_UP)
        if (deductBreakFromSalary) {
            require(breakHours < dailyWorkHours) {
                "Break duration cannot exceed or equal daily work hours when deducted"
            }
        }
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
     * Hourly earning rate in BigDecimal (Scale 16).
     * For 28,000 / 22 / 8 = 159.0909090909090909
     */
    val hourlyRate: BigDecimal
        get() {
            val totalMonthlyHours = BigDecimal(monthlyWorkDays).multiply(dailyPaidHours)
            return monthlySalary.divide(totalMonthlyHours, 16, RoundingMode.HALF_UP)
        }

    /**
     * Per-minute earning rate in BigDecimal (Scale 16).
     * For 159.090909... / 60 = 2.6515151515151515
     */
    val minuteRate: BigDecimal
        get() = hourlyRate.divide(BigDecimal(60), 16, RoundingMode.HALF_UP)

    /**
     * Per-second earning rate in BigDecimal (Scale 16).
     * For 159.090909... / 3600 = 0.0441919191919192
     */
    val secondRate: BigDecimal
        get() = hourlyRate.divide(BigDecimal(3600), 16, RoundingMode.HALF_UP)
}
