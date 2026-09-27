package com.example.androidapp.vardiya.domain.calculator

import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftEarnings
import com.example.androidapp.vardiya.domain.model.ShiftState
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure, deterministic calculator deriving active shift duration and real-time earnings
 * without accumulator drift.
 *
 * Implements dual-clock time architecture:
 * - Monotonic elapsed realtime for in-session wall-clock tampering immunity.
 * - Epoch wall-clock anchoring for device reboot recovery.
 */
class ShiftEarningsCalculator {

    companion object {
        const val MAX_REASONABLE_SHIFT_MS = 7 * 24 * 3600 * 1000L // 7 days ceiling
        private val MS_PER_HOUR = BigDecimal(3_600_000)
    }

    /**
     * Calculates the exact active duration of the shift in milliseconds.
     */
    fun calculateActiveDurationMs(shift: Shift, nowElapsed: Long, nowEpoch: Long): Long {
        return when (shift.state) {
            ShiftState.NOT_STARTED -> 0L
            ShiftState.PAUSED, ShiftState.FINISHED -> shift.accumulatedActiveElapsedMs
            ShiftState.RUNNING -> {
                val currentSegment = if (nowElapsed >= shift.lastResumeElapsedRealtime) {
                    // Normal execution on same boot cycle: monotonic difference is 100% immune to clock changes
                    nowElapsed - shift.lastResumeElapsedRealtime
                } else {
                    // System reboot detected: uptime reset (nowElapsed < lastResumeElapsedRealtime).
                    // Fall back to wall-clock epoch difference from the last resume point.
                    val anchor = if (shift.lastResumeEpochMillis > 0L) {
                        shift.lastResumeEpochMillis
                    } else {
                        shift.startEpochMillis
                    }
                    val wallClockSegment = nowEpoch - anchor
                    wallClockSegment.coerceAtLeast(0L)
                }

                val totalActive = shift.accumulatedActiveElapsedMs + currentSegment.coerceAtLeast(0L)
                totalActive.coerceIn(0L, MAX_REASONABLE_SHIFT_MS)
            }
        }
    }

    /**
     * Derives real-time earnings from hourly rate and active duration.
     * Multiplies first to retain 16 fractional digits of precision before division.
     */
    fun calculateEarnings(shift: Shift, activeDurationMs: Long): ShiftEarnings {
        val config = shift.salaryConfig
        val hourlyRate = config.hourlyRate

        val earned = if (shift.state == ShiftState.FINISHED && shift.totalEarnedWhenFinished != null) {
            shift.totalEarnedWhenFinished
        } else {
            val safeDurationMs = activeDurationMs.coerceAtLeast(0L)
            val msBigDecimal = BigDecimal(safeDurationMs)
            hourlyRate.multiply(msBigDecimal).divide(MS_PER_HOUR, 16, RoundingMode.HALF_UP)
        }

        return ShiftEarnings(
            earnedAmount = earned,
            activeDurationMs = activeDurationMs,
            hourlyRate = hourlyRate,
            minuteRate = config.minuteRate,
            secondRate = config.secondRate,
            currencySymbol = config.currencySymbol,
            currencyCode = config.currencyCode
        )
    }
}
