package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftEarnings
import com.example.androidapp.vardiya.domain.model.ShiftState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Immutable UI state for the Vardiya screen.
 */
data class VardiyaUiState(
    val shiftState: ShiftState = ShiftState.NOT_STARTED,
    val salaryConfig: SalaryConfiguration = SalaryConfiguration(),
    val earnings: ShiftEarnings = ShiftEarnings(),
    val currentShift: Shift? = null,
    val history: List<CompletedShiftRecord> = emptyList(),
    val isSetupVisible: Boolean = false,
    val isHistoryVisible: Boolean = false,
    val selectedHistoryRecord: CompletedShiftRecord? = null,
    val errorMessage: String? = null
) {
    companion object {
        private val turkishSymbols = DecimalFormatSymbols(Locale("tr", "TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
        private val moneyFormat = DecimalFormat("#,##0", turkishSymbols)
    }

    val stateBadgeText: String
        get() = when (shiftState) {
            ShiftState.NOT_STARTED -> "VARDİYAYA HAZIR"
            ShiftState.RUNNING -> "VARDİYA AKTİF"
            ShiftState.PAUSED -> "VARDİYA DURAKLATILDI"
            ShiftState.FINISHED -> "VARDİYA TAMAMLANDI"
        }

    val heroAmountText: String
        get() = when (shiftState) {
            ShiftState.NOT_STARTED -> "${salaryConfig.currencySymbol}${moneyFormat.format(salaryConfig.monthlySalary.setScale(0, RoundingMode.HALF_UP))}"
            ShiftState.RUNNING, ShiftState.PAUSED, ShiftState.FINISHED -> earnings.formattedEarned
        }

    val heroSubtitleText: String
        get() = when (shiftState) {
            ShiftState.NOT_STARTED -> "AYLIK MAAŞ"
            ShiftState.RUNNING, ShiftState.PAUSED -> "BU VARDİYADA KAZANILAN"
            ShiftState.FINISHED -> "BU VARDİYADA KAZANILDI"
        }
}
