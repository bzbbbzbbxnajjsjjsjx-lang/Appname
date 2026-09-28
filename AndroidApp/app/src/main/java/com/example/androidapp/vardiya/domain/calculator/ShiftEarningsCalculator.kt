package com.example.androidapp.vardiya.domain.calculator

import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftEarnings
import com.example.androidapp.vardiya.domain.model.ShiftState
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure, deterministic calculator deriving active shift duration and real-time earnings
 * without accumulator drift.
 *
 * Implements dual-clock time architecture:
 * - Monotonic elapsed realtime for in-session wall-clock tampering immunity.
 * - Epoch wall-clock anchoring for device reboot recovery.
 *
 * Phase C expands calculation engine to support:
 * - Overtime multiplier for active hours exceeding daily schedule.
 * - Night shift differential supplement for hours worked during night windows.
 * - Granular break tracking and optional salary deduction.
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
     * Calculates the overlap in milliseconds between a time interval [startEpochMs, endEpochMs]
     * and the recurring daily night window [nightStartHour, nightEndHour] (in local time).
     */
    fun calculateNightShiftDurationMs(
        startEpochMs: Long,
        endEpochMs: Long,
        nightStartHour: Int,
        nightEndHour: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        if (startEpochMs >= endEpochMs) return 0L
        if (nightStartHour == nightEndHour) return 0L

        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = startEpochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var totalNightMs = 0L

        while (calendar.timeInMillis < endEpochMs) {
            val dayStart = calendar.timeInMillis

            if (nightStartHour > nightEndHour) {
                // Window crosses midnight (e.g. 20:00 to 06:00):
                // Morning segment: [dayStart, dayStart + nightEndHour * 3600000]
                val seg1Start = dayStart
                val seg1End = dayStart + (nightEndHour * 3_600_000L)
                val overlap1 = (minOf(endEpochMs, seg1End) - maxOf(startEpochMs, seg1Start)).coerceAtLeast(0L)
                totalNightMs += overlap1

                // Evening segment: [dayStart + nightStartHour * 3600000, dayStart + 24 * 3600000]
                val seg2Start = dayStart + (nightStartHour * 3_600_000L)
                val seg2End = dayStart + (24 * 3_600_000L)
                val overlap2 = (minOf(endEpochMs, seg2End) - maxOf(startEpochMs, seg2Start)).coerceAtLeast(0L)
                totalNightMs += overlap2
            } else {
                // Window within same calendar day (e.g. 01:00 to 05:00):
                val segStart = dayStart + (nightStartHour * 3_600_000L)
                val segEnd = dayStart + (nightEndHour * 3_600_000L)
                val overlap = (minOf(endEpochMs, segEnd) - maxOf(startEpochMs, segStart)).coerceAtLeast(0L)
                totalNightMs += overlap
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return totalNightMs
    }

    /**
     * Derives real-time earnings from hourly rate, active duration, overtime,
     * night shift differential, and break deductions.
     * Multiplies first to retain 16 fractional digits of precision before division.
     */
    fun calculateEarnings(
        shift: Shift,
        activeDurationMs: Long,
        nowEpoch: Long = 0L
    ): ShiftEarnings {
        val config = shift.salaryConfig
        val hourlyRate = config.hourlyRate
        val safeDurationMs = activeDurationMs.coerceAtLeast(0L)

        // 1. Regular vs Overtime Durations
        val regularThresholdMs = (config.dailyPaidHours.multiply(MS_PER_HOUR)).toLong().coerceAtLeast(0L)
        val (regularDurationMs, overtimeDurationMs) = if (config.isOvertimeEnabled && safeDurationMs > regularThresholdMs) {
            regularThresholdMs to (safeDurationMs - regularThresholdMs)
        } else {
            safeDurationMs to 0L
        }

        // 2. Base and Overtime Calculations
        val baseEarned = hourlyRate.multiply(BigDecimal(regularDurationMs))
            .divide(MS_PER_HOUR, 16, RoundingMode.HALF_UP)

        val overtimeEarned = if (overtimeDurationMs > 0L) {
            config.overtimeHourlyRate.multiply(BigDecimal(overtimeDurationMs))
                .divide(MS_PER_HOUR, 16, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // 3. Night Shift Calculation
        val resolvedNowEpoch = when {
            nowEpoch > 0L -> nowEpoch
            shift.state == ShiftState.FINISHED && shift.finishEpochMillis != null -> shift.finishEpochMillis
            shift.startEpochMillis > 0L -> shift.startEpochMillis + safeDurationMs
            else -> System.currentTimeMillis()
        }

        val rawNightDurationMs = if (config.isNightDifferentialEnabled && shift.startEpochMillis > 0L) {
            calculateNightShiftDurationMs(
                startEpochMs = shift.startEpochMillis,
                endEpochMs = resolvedNowEpoch,
                nightStartHour = config.nightShiftStartHour,
                nightEndHour = config.nightShiftEndHour
            )
        } else {
            0L
        }
        val nightShiftDurationMs = rawNightDurationMs.coerceAtMost(safeDurationMs)

        val nightDifferentialEarned = if (config.isNightDifferentialEnabled && nightShiftDurationMs > 0L) {
            config.nightDifferentialHourlyRate.multiply(BigDecimal(nightShiftDurationMs))
                .divide(MS_PER_HOUR, 16, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // 4. Break Deductions
        var deductedBreakMs = 0L
        for (brk in shift.activeBreaks) {
            if (brk.isDeductedFromSalary) {
                deductedBreakMs += brk.getDurationMs(resolvedNowEpoch)
            }
        }
        val unpaidBreakDeduction = if (deductedBreakMs > 0L) {
            hourlyRate.multiply(BigDecimal(deductedBreakMs))
                .divide(MS_PER_HOUR, 16, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // 5. Total Earned Amount
        val totalCalculated = (baseEarned + overtimeEarned + nightDifferentialEarned - unpaidBreakDeduction)
            .coerceAtLeast(BigDecimal.ZERO)

        val earned = if (shift.state == ShiftState.FINISHED && shift.totalEarnedWhenFinished != null) {
            shift.totalEarnedWhenFinished
        } else {
            totalCalculated
        }

        return ShiftEarnings(
            earnedAmount = earned,
            activeDurationMs = activeDurationMs,
            hourlyRate = hourlyRate,
            minuteRate = config.minuteRate,
            secondRate = config.secondRate,
            currencySymbol = config.currencySymbol,
            currencyCode = config.currencyCode,
            baseEarned = baseEarned,
            overtimeEarned = overtimeEarned,
            nightDifferentialEarned = nightDifferentialEarned,
            unpaidBreakDeduction = unpaidBreakDeduction,
            regularDurationMs = regularDurationMs,
            overtimeDurationMs = overtimeDurationMs,
            nightShiftDurationMs = nightShiftDurationMs,
            deductedBreakDurationMs = deductedBreakMs
        )
    }
}
