package com.example.androidapp.vardiya.domain.calculator

import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftEarnings
import com.example.androidapp.vardiya.domain.model.ShiftState
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure calculator deriving active shift duration and real-time earnings without accumulator drift.
 */
class ShiftEarningsCalculator {

    /**
     * Calculates the exact active duration of the shift in milliseconds.
     */
    fun calculateActiveDurationMs(shift: Shift, nowElapsed: Long, nowEpoch: Long): Long {
        return when (shift.state) {
            ShiftState.NOT_STARTED -> 0L
            ShiftState.PAUSED, ShiftState.FINISHED -> shift.accumulatedActiveElapsedMs
            ShiftState.RUNNING -> {
                val currentSegment = if (nowElapsed >= shift.lastResumeElapsedRealtime) {
                    nowElapsed - shift.lastResumeElapsedRealtime
                } else {
                    // System reboot detected while running; fallback to wall-clock difference
                    val wallClockElapsed = nowEpoch - shift.startEpochMillis
                    if (wallClockElapsed > 0) wallClockElapsed - shift.accumulatedActiveElapsedMs else 0L
                }
                shift.accumulatedActiveElapsedMs + currentSegment.coerceAtLeast(0L)
            }
        }
    }

    /**
     * Derives real-time earnings from hourly rate and active duration.
     */
    fun calculateEarnings(shift: Shift, activeDurationMs: Long): ShiftEarnings {
        val config = shift.salaryConfig
        val hourlyRate = config.hourlyRate

        val earned = if (shift.state == ShiftState.FINISHED && shift.totalEarnedWhenFinished != null) {
            shift.totalEarnedWhenFinished
        } else {
            val elapsedHours = BigDecimal(activeDurationMs).divide(BigDecimal(3_600_000), 10, RoundingMode.HALF_UP)
            hourlyRate.multiply(elapsedHours).setScale(10, RoundingMode.HALF_UP)
        }

        return ShiftEarnings(
            earnedAmount = earned,
            activeDurationMs = activeDurationMs,
            hourlyRate = hourlyRate,
            minuteRate = config.minuteRate,
            secondRate = config.secondRate,
            currencySymbol = config.currencySymbol
        )
    }
}
