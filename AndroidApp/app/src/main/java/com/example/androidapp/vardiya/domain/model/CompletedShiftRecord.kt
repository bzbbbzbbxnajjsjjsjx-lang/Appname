package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal

/**
 * Historical record of a completed shift.
 */
data class CompletedShiftRecord(
    val id: String,
    val dateFormatted: String,
    val timeRangeFormatted: String,
    val durationFormatted: String,
    val earnedFormatted: String,
    val totalEarned: BigDecimal,
    val activeDurationMs: Long,
    val startEpochMillis: Long,
    val finishEpochMillis: Long
)
