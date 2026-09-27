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
    val currencyCode: String = "TRY"
) {
    companion object {
        private val turkishSymbols = DecimalFormatSymbols(Locale("tr", "TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }

        private val standardMoneyFormat = DecimalFormat("#,##0.00", turkishSymbols)
        private val highPrecisionRateFormat = DecimalFormat("0.00000", turkishSymbols)
        private val fourDecimalRateFormat = DecimalFormat("0.0000", turkishSymbols)
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
        get() {
            val totalSeconds = (activeDurationMs / 1000).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }

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
