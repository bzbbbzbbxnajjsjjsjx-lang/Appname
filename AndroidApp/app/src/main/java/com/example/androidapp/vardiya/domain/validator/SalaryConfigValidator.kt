package com.example.androidapp.vardiya.domain.validator

import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import java.math.BigDecimal

/**
 * Result of validating salary configuration inputs.
 */
sealed class SalaryValidationResult {
    data object Valid : SalaryValidationResult()
    data class Invalid(val field: Field, val errorMessage: String) : SalaryValidationResult()

    enum class Field {
        SALARY,
        WORK_DAYS,
        WORK_HOURS,
        BREAK_MINUTES,
        OVERTIME_MULTIPLIER,
        NIGHT_DIFFERENTIAL_RATE,
        NIGHT_SHIFT_HOURS,
        GENERAL
    }
}

/**
 * Strict validator rejecting zero, negative, NaN, Infinity, and malformed inputs.
 */
object SalaryConfigValidator {

    fun validate(
        salaryText: String,
        workDaysText: String,
        workHoursText: String,
        breakMinutesText: String,
        deductBreak: Boolean,
        currencySymbol: String = "₺",
        currencyCode: String = "TRY",
        overtimeMultiplierText: String = "1.50",
        nightDifferentialRateText: String = "0.15",
        nightShiftStartHour: Int = 20,
        nightShiftEndHour: Int = 6
    ): SalaryValidationResult {
        // 1. Validate Monthly Salary
        val cleanSalary = salaryText.replace(",", ".").trim()
        if (cleanSalary.isBlank()) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.SALARY,
                "Aylık maaş boş bırakılamaz."
            )
        }
        val salary = try {
            BigDecimal(cleanSalary)
        } catch (e: Exception) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.SALARY,
                "Geçerli bir maaş tutarı giriniz."
            )
        }
        if (salary <= BigDecimal.ZERO) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.SALARY,
                "Maaş 0 veya negatif olamaz."
            )
        }

        // 2. Validate Monthly Work Days
        val cleanWorkDays = workDaysText.trim()
        val workDays = cleanWorkDays.toIntOrNull()
            ?: return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.WORK_DAYS,
                "Aylık çalışma gün sayısı tam sayı olmalıdır."
            )
        if (workDays <= 0) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.WORK_DAYS,
                "Çalışma gün sayısı en az 1 olmalıdır."
            )
        }
        if (workDays > 31) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.WORK_DAYS,
                "Bir ayda en fazla 31 gün olabilir."
            )
        }

        // 3. Validate Daily Work Hours
        val cleanWorkHours = workHoursText.replace(",", ".").trim()
        val dailyHours = try {
            BigDecimal(cleanWorkHours)
        } catch (e: Exception) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.WORK_HOURS,
                "Geçerli bir günlük çalışma süresi giriniz."
            )
        }
        if (dailyHours <= BigDecimal.ZERO) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.WORK_HOURS,
                "Günlük çalışma saati 0 veya negatif olamaz."
            )
        }
        if (dailyHours > BigDecimal("24")) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.WORK_HOURS,
                "Günlük çalışma saati 24 saati aşamaz."
            )
        }

        // 4. Validate Break Minutes
        val cleanBreak = breakMinutesText.trim()
        val breakMinutes = cleanBreak.toIntOrNull()
            ?: return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.BREAK_MINUTES,
                "Mola süresi tam sayı dakika olmalıdır."
            )
        if (breakMinutes < 0) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.BREAK_MINUTES,
                "Mola süresi negatif olamaz."
            )
        }

        val totalDailyMinutes = dailyHours.multiply(BigDecimal("60")).toInt()
        if (breakMinutes >= totalDailyMinutes) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.BREAK_MINUTES,
                "Mola süresi günlük çalışma süresine eşit veya daha uzun olamaz."
            )
        }

        // 5. Validate Overtime Multiplier
        val cleanOt = overtimeMultiplierText.replace(",", ".").trim()
        if (cleanOt.isBlank()) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.OVERTIME_MULTIPLIER,
                "Fazla mesai çarpanı boş bırakılamaz."
            )
        }
        val otMultiplier = try {
            BigDecimal(cleanOt)
        } catch (e: Exception) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.OVERTIME_MULTIPLIER,
                "Geçerli bir fazla mesai çarpanı giriniz."
            )
        }
        if (otMultiplier < BigDecimal.ONE) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.OVERTIME_MULTIPLIER,
                "Fazla mesai çarpanı en az 1.0 olmalıdır."
            )
        }

        // 6. Validate Night Differential Rate
        val cleanNight = nightDifferentialRateText.replace(",", ".").trim().removeSuffix("%")
        if (cleanNight.isBlank()) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.NIGHT_DIFFERENTIAL_RATE,
                "Gece farkı oranı boş bırakılamaz."
            )
        }
        val nightRate = try {
            BigDecimal(cleanNight)
        } catch (e: Exception) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.NIGHT_DIFFERENTIAL_RATE,
                "Gece farkı oranı sayısal olmalıdır."
            )
        }
        if (nightRate < BigDecimal.ZERO) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.NIGHT_DIFFERENTIAL_RATE,
                "Gece farkı oranı negatif olamaz."
            )
        }

        // 7. Validate Night Shift Hours
        if (nightShiftStartHour !in 0..23 || nightShiftEndHour !in 0..23) {
            return SalaryValidationResult.Invalid(
                SalaryValidationResult.Field.NIGHT_SHIFT_HOURS,
                "Gece vardiyası saatleri 0 ile 23 arasında olmalıdır."
            )
        }

        return SalaryValidationResult.Valid
    }

    fun parseOrNull(
        salaryText: String,
        workDaysText: String,
        workHoursText: String,
        breakMinutesText: String,
        deductBreak: Boolean,
        currencySymbol: String = "₺",
        currencyCode: String = "TRY",
        overtimeMultiplierText: String = "1.50",
        isOvertimeEnabled: Boolean = false,
        nightDifferentialRateText: String = "0.15",
        isNightDifferentialEnabled: Boolean = false,
        nightShiftStartHour: Int = 20,
        nightShiftEndHour: Int = 6
    ): SalaryConfiguration? {
        val validation = validate(
            salaryText = salaryText,
            workDaysText = workDaysText,
            workHoursText = workHoursText,
            breakMinutesText = breakMinutesText,
            deductBreak = deductBreak,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            overtimeMultiplierText = overtimeMultiplierText,
            nightDifferentialRateText = nightDifferentialRateText,
            nightShiftStartHour = nightShiftStartHour,
            nightShiftEndHour = nightShiftEndHour
        )
        if (validation !is SalaryValidationResult.Valid) return null

        val cleanNight = nightDifferentialRateText.replace(",", ".").trim().removeSuffix("%")
        val parsedNightRate = BigDecimal(cleanNight)
        val finalNightRate = if (parsedNightRate > BigDecimal.ONE) {
            parsedNightRate.divide(BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
        } else {
            parsedNightRate
        }

        return SalaryConfiguration(
            monthlySalary = BigDecimal(salaryText.replace(",", ".").trim()),
            monthlyWorkDays = workDaysText.trim().toInt(),
            dailyWorkHours = BigDecimal(workHoursText.replace(",", ".").trim()),
            breakMinutes = breakMinutesText.trim().toInt(),
            deductBreakFromSalary = deductBreak,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            overtimeMultiplier = BigDecimal(overtimeMultiplierText.replace(",", ".").trim()),
            isOvertimeEnabled = isOvertimeEnabled,
            nightDifferentialRate = finalNightRate,
            isNightDifferentialEnabled = isNightDifferentialEnabled,
            nightShiftStartHour = nightShiftStartHour,
            nightShiftEndHour = nightShiftEndHour
        )
    }
}
