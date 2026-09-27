package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal

/**
 * Immutable historical record of a completed shift.
 * Freezes the salary configuration snapshot, active/total durations, and earnings.
 * Subsequent modifications to the user's active salary configuration will NEVER
 * alter historical shift earnings or metrics.
 */
data class CompletedShiftRecord(
    val id: String,
    val dateFormatted: String,
    val timeRangeFormatted: String,
    val durationFormatted: String,
    val earnedFormatted: String,
    val totalEarned: BigDecimal,
    val activeDurationMs: Long,
    val totalDurationMs: Long = activeDurationMs,
    val startEpochMillis: Long,
    val finishEpochMillis: Long,
    val salaryConfigSnapshot: SalaryConfiguration = SalaryConfiguration(),
    val currencySymbol: String = salaryConfigSnapshot.currencySymbol,
    val currencyCode: String = salaryConfigSnapshot.currencyCode
)
