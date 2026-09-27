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
        currencyCode: String = "TRY"
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

        return SalaryValidationResult.Valid
    }

    fun parseOrNull(
        salaryText: String,
        workDaysText: String,
        workHoursText: String,
        breakMinutesText: String,
        deductBreak: Boolean,
        currencySymbol: String = "₺",
        currencyCode: String = "TRY"
    ): SalaryConfiguration? {
        val validation = validate(
            salaryText = salaryText,
            workDaysText = workDaysText,
            workHoursText = workHoursText,
            breakMinutesText = breakMinutesText,
            deductBreak = deductBreak,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode
        )
        if (validation !is SalaryValidationResult.Valid) return null

        return SalaryConfiguration(
            monthlySalary = BigDecimal(salaryText.replace(",", ".").trim()),
            monthlyWorkDays = workDaysText.trim().toInt(),
            dailyWorkHours = BigDecimal(workHoursText.replace(",", ".").trim()),
            breakMinutes = breakMinutesText.trim().toInt(),
            deductBreakFromSalary = deductBreak,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode
        )
    }
}
