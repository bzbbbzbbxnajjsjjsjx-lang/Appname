package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Calculated real-time snapshot of shift earnings and rates.
 * Separates high-precision domain arithmetic from UI display formatting.
 */
data class ShiftEarnings(
    val earnedAmount: BigDecimal = BigDecimal.ZERO,
    val activeDurationMs: Long = 0L,
    val hourlyRate: BigDecimal = BigDecimal.ZERO,
    val minuteRate: BigDecimal = BigDecimal.ZERO,
    val secondRate: BigDecimal = BigDecimal.ZERO,
    val currencySymbol: String = "₺",
    val currencyCode: String = "TRY",
    val baseEarned: BigDecimal = earnedAmount,
    val overtimeEarned: BigDecimal = BigDecimal.ZERO,
    val nightDifferentialEarned: BigDecimal = BigDecimal.ZERO,
    val unpaidBreakDeduction: BigDecimal = BigDecimal.ZERO,
    val regularDurationMs: Long = activeDurationMs,
    val overtimeDurationMs: Long = 0L,
    val nightShiftDurationMs: Long = 0L,
    val deductedBreakDurationMs: Long = 0L
) {
    val isOvertimeActive: Boolean
        get() = overtimeDurationMs > 0L

    val isNightShiftActive: Boolean
        get() = nightShiftDurationMs > 0L

    companion object {
        private val turkishSymbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }

        private val standardMoneyFormat = DecimalFormat("#,##0.00", turkishSymbols)
        private val highPrecisionRateFormat = DecimalFormat("0.00000", turkishSymbols)
        private val fourDecimalRateFormat = DecimalFormat("0.0000", turkishSymbols)

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    /**
     * Primary hero representation (e.g. "₺12,47").
     */
    val formattedEarned: String
        get() = "$currencySymbol${standardMoneyFormat.format(earnedAmount.setScale(2, RoundingMode.HALF_UP))}"

    /**
     * Formatted active duration (e.g. "02:56:21").
     */
    val formattedDuration: String
        get() = formatDuration(activeDurationMs)

    /**
     * Formatted regular duration (e.g. "08:00:00").
     */
    val formattedRegularDuration: String
        get() = formatDuration(regularDurationMs)

    /**
     * Formatted overtime duration (e.g. "01:30:00").
     */
    val formattedOvertimeDuration: String
        get() = formatDuration(overtimeDurationMs)

    /**
     * Formatted night shift duration (e.g. "06:00:00").
     */
    val formattedNightShiftDuration: String
        get() = formatDuration(nightShiftDurationMs)

    /**
     * Formatted base earned amount.
     */
    val formattedBaseEarned: String
        get() = "$currencySymbol${standardMoneyFormat.format(baseEarned.setScale(2, RoundingMode.HALF_UP))}"

    /**
     * Formatted overtime earned amount.
     */
    val formattedOvertimeEarned: String
        get() = "$currencySymbol${standardMoneyFormat.format(overtimeEarned.setScale(2, RoundingMode.HALF_UP))}"

    /**
     * Formatted night differential earned amount.
     */
    val formattedNightDifferentialEarned: String
        get() = "$currencySymbol${standardMoneyFormat.format(nightDifferentialEarned.setScale(2, RoundingMode.HALF_UP))}"

    /**
     * Formatted unpaid break deduction amount.
     */
    val formattedUnpaidBreakDeduction: String
        get() = "$currencySymbol${standardMoneyFormat.format(unpaidBreakDeduction.setScale(2, RoundingMode.HALF_UP))}"

    /**
     * Formatted hourly rate (e.g. "₺159,09 / SAAT").
     */
    val formattedHourlyRate: String
        get() = "$currencySymbol${standardMoneyFormat.format(hourlyRate.setScale(2, RoundingMode.HALF_UP))} / SAAT"

    /**
     * Formatted minute rate (e.g. "₺2,6515 / DAKİKA").
     */
    val formattedMinuteRate: String
        get() = "$currencySymbol${fourDecimalRateFormat.format(minuteRate.setScale(4, RoundingMode.HALF_UP))} / DAKİKA"

    /**
     * Formatted second rate (e.g. "₺0,04419 / SANİYE").
     */
    val formattedSecondRate: String
        get() = "$currencySymbol${highPrecisionRateFormat.format(secondRate.setScale(5, RoundingMode.HALF_UP))} / SANİYE"
}
