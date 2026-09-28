package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.domain.model.BreakRecord
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftEarnings
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.model.ShiftTemplate
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
    val isTemplatePickerVisible: Boolean = false,
    val isBreakSheetVisible: Boolean = false,
    val availableTemplates: List<ShiftTemplate> = ShiftTemplate.PRESETS,
    val selectedTemplateId: String? = null,
    val selectedHistoryRecord: CompletedShiftRecord? = null,
    val errorMessage: String? = null
) {
    companion object {
        private val turkishSymbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
        private val moneyFormat = DecimalFormat("#,##0", turkishSymbols)
    }

    val selectedTemplate: ShiftTemplate?
        get() {
            val id = selectedTemplateId ?: currentShift?.templateId
            return availableTemplates.firstOrNull { it.id == id }
        }

    val currentShiftBreaks: List<BreakRecord>
        get() = currentShift?.activeBreaks ?: emptyList()

    val ongoingBreak: BreakRecord?
        get() = currentShift?.activeBreaks?.firstOrNull { it.isOngoing }

    val isBreakActive: Boolean
        get() = currentShift?.activeBreaks?.any { it.isOngoing } == true

    val activeBreakDurationMs: Long
        get() {
            val ongoing = currentShift?.activeBreaks?.firstOrNull { it.isOngoing } ?: return 0L
            return ongoing.getDurationMs()
        }

    val activeBreakFormattedDuration: String
        get() {
            val totalSeconds = (activeBreakDurationMs / 1000).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }

    val isOvertimeActive: Boolean
        get() = earnings.isOvertimeActive

    val isNightShiftActive: Boolean
        get() = earnings.isNightShiftActive

    val stateBadgeText: String
        get() = when {
            shiftState == ShiftState.RUNNING && isBreakActive -> "MOLADA"
            shiftState == ShiftState.RUNNING && isOvertimeActive -> "FAZLA MESAİDE"
            shiftState == ShiftState.NOT_STARTED -> "VARDİYAYA HAZIR"
            shiftState == ShiftState.RUNNING -> "VARDİYA AKTİF"
            shiftState == ShiftState.PAUSED -> "VARDİYA DURAKLATILDI"
            shiftState == ShiftState.FINISHED -> "VARDİYA TAMAMLANDI"
            else -> "VARDİYA"
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

    val progress: Float
        get() = when (shiftState) {
            ShiftState.NOT_STARTED -> 0f
            ShiftState.FINISHED -> 1f
            ShiftState.RUNNING, ShiftState.PAUSED -> {
                val targetHours = salaryConfig.dailyPaidHours
                val targetMs = targetHours.multiply(java.math.BigDecimal(3_600_000)).toLong().coerceAtLeast(1L)
                (earnings.activeDurationMs.toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)
            }
        }
}
