package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Domain entity representing an active or completed work shift.
 */
data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val startEpochMillis: Long = 0L,
    val startElapsedRealtime: Long = 0L,
    val accumulatedActiveElapsedMs: Long = 0L,
    val lastResumeElapsedRealtime: Long = 0L,
    val pauseEpochMillis: Long? = null,
    val finishEpochMillis: Long? = null,
    val state: ShiftState = ShiftState.NOT_STARTED,
    val salaryConfig: SalaryConfiguration = SalaryConfiguration(),
    val totalEarnedWhenFinished: BigDecimal? = null
)
