package com.example.androidapp.vardiya.domain.calculator

import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ShiftEarningsCalculatorComprehensiveTest {

    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var standardConfig: SalaryConfiguration

    @Before
    fun setUp() {
        calculator = ShiftEarningsCalculator()
        // 28,000 / 22 / 8 = 159.0909090909090909... TL/hr
        standardConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 60,
            deductBreakFromSalary = false
        )
    }

    @Test
    fun testExactTimeMilestones() {
        // Milestone durations in milliseconds
        val milestones = listOf(
            0L to BigDecimal.ZERO,
            1_000L to standardConfig.secondRate, // 1 sec
            10_000L to standardConfig.secondRate.multiply(BigDecimal(10)), // 10 sec
            30_000L to standardConfig.secondRate.multiply(BigDecimal(30)), // 30 sec
            60_000L to standardConfig.minuteRate, // 1 min
            600_000L to standardConfig.minuteRate.multiply(BigDecimal(10)), // 10 min
            1_800_000L to standardConfig.minuteRate.multiply(BigDecimal(30)), // 30 min
            3_600_000L to standardConfig.hourlyRate, // 1 hour
            8 * 3_600_000L to standardConfig.hourlyRate.multiply(BigDecimal(8)), // 8 hours
            24 * 3_600_000L to standardConfig.hourlyRate.multiply(BigDecimal(24)) // 24 hours
        )

        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 500000L,
            lastResumeElapsedRealtime = 500000L,
            lastResumeEpochMillis = 1000000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        for ((durationMs, expectedEarned) in milestones) {
            val calculatedMs = calculator.calculateActiveDurationMs(
                shift,
                nowElapsed = 500000L + durationMs,
                nowEpoch = 1000000L + durationMs
            )
            assertEquals(durationMs, calculatedMs)

            val earnings = calculator.calculateEarnings(shift, calculatedMs)
            val expectedRounded = expectedEarned.setScale(2, RoundingMode.HALF_UP)
            val actualRounded = earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP)
            assertEquals(
                "Mismatch at milestone ${durationMs}ms",
                expectedRounded,
                actualRounded
            )
        }
    }

    @Test
    fun testWallClockTamperingForwardAndBackward() {
        val shift = Shift(
            startEpochMillis = 10000000L,
            startElapsedRealtime = 2000000L,
            lastResumeElapsedRealtime = 2000000L,
            lastResumeEpochMillis = 10000000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        val realActiveMs = 10 * 60 * 1000L // Exactly 10 minutes pass in monotonic time
        val actualMonotonicNow = shift.lastResumeElapsedRealtime + realActiveMs

        // Case A: User jumps phone forward by +2 hours
        val forwardClockEpoch = shift.startEpochMillis + realActiveMs + (2 * 3600 * 1000L)
        val elapsedForward = calculator.calculateActiveDurationMs(shift, actualMonotonicNow, forwardClockEpoch)
        assertEquals(realActiveMs, elapsedForward)

        // Case B: User rewinds phone by -2 hours
        val backwardClockEpoch = shift.startEpochMillis + realActiveMs - (2 * 3600 * 1000L)
        val elapsedBackward = calculator.calculateActiveDurationMs(shift, actualMonotonicNow, backwardClockEpoch)
        assertEquals(realActiveMs, elapsedBackward)
    }

    @Test
    fun testMidnightCrossingInvariance() {
        // Shift starts at 23:59:00 and crosses to 00:01:00 next day (2 minutes = 120,000 ms)
        val startEpoch = 1700000000000L // 23:59
        val startMonotonic = 10000000L
        val twoMinutesMs = 2 * 60 * 1000L

        val shift = Shift(
            startEpochMillis = startEpoch,
            startElapsedRealtime = startMonotonic,
            lastResumeElapsedRealtime = startMonotonic,
            lastResumeEpochMillis = startEpoch,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        val activeMs = calculator.calculateActiveDurationMs(
            shift,
            nowElapsed = startMonotonic + twoMinutesMs,
            nowEpoch = startEpoch + twoMinutesMs
        )
        assertEquals(twoMinutesMs, activeMs)

        val earnings = calculator.calculateEarnings(shift, activeMs)
        val expected = standardConfig.minuteRate.multiply(BigDecimal(2)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testLongShiftSimulations() {
        val longShifts = listOf(
            1L * 3600 * 1000L,
            8L * 3600 * 1000L,
            12L * 3600 * 1000L,
            24L * 3600 * 1000L,
            48L * 3600 * 1000L
        )

        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 500000L,
            lastResumeElapsedRealtime = 500000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        for (durationMs in longShifts) {
            val earnings = calculator.calculateEarnings(shift, durationMs)
            assertTrue(earnings.earnedAmount > BigDecimal.ZERO)
            assertTrue(earnings.activeDurationMs == durationMs)
            // 48 hours * 159.09 = ~7636.36 TL
            if (durationMs == 48L * 3600 * 1000L) {
                val expected = standardConfig.hourlyRate.multiply(BigDecimal(48)).setScale(2, RoundingMode.HALF_UP)
                assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
            }
        }
    }

    @Test
    fun testPauseSemanticsFreezeDuration() {
        // RUNNING 10 min -> PAUSED 20 min -> RUNNING 10 min => Paid elapsed: 20 min
        val tenMinutesMs = 10 * 60 * 1000L
        val twentyMinutesMs = 20 * 60 * 1000L

        // Stage 1: Running for 10 min
        var shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 100000L,
            lastResumeElapsedRealtime = 100000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        // Pause after 10 min
        val activeAtPause = calculator.calculateActiveDurationMs(
            shift,
            nowElapsed = 100000L + tenMinutesMs,
            nowEpoch = 1000000L + tenMinutesMs
        )
        assertEquals(tenMinutesMs, activeAtPause)

        shift = shift.copy(
            accumulatedActiveElapsedMs = activeAtPause,
            pauseEpochMillis = 1000000L + tenMinutesMs,
            state = ShiftState.PAUSED
        )

        // Stage 2: In pause for 20 min, duration must be frozen
        val activeDuringPause = calculator.calculateActiveDurationMs(
            shift,
            nowElapsed = 100000L + tenMinutesMs + twentyMinutesMs,
            nowEpoch = 1000000L + tenMinutesMs + twentyMinutesMs
        )
        assertEquals(tenMinutesMs, activeDuringPause)

        // Stage 3: Resume after 20 min pause
        val resumeElapsed = 100000L + tenMinutesMs + twentyMinutesMs
        val resumeEpoch = 1000000L + tenMinutesMs + twentyMinutesMs
        shift = shift.copy(
            lastResumeElapsedRealtime = resumeElapsed,
            lastResumeEpochMillis = resumeEpoch,
            pauseEpochMillis = null,
            state = ShiftState.RUNNING
        )

        // Running another 10 min
        val finalActive = calculator.calculateActiveDurationMs(
            shift,
            nowElapsed = resumeElapsed + tenMinutesMs,
            nowEpoch = resumeEpoch + tenMinutesMs
        )
        // Total active: 10m + 10m = 20m (1,200,000 ms)
        assertEquals(20 * 60 * 1000L, finalActive)

        val earnings = calculator.calculateEarnings(shift, finalActive)
        val expected = standardConfig.hourlyRate.multiply(BigDecimal(20).divide(BigDecimal(60), 10, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testBreakPaidVsUnpaidCalculations() {
        // 8h shift, 30m unpaid break -> paid hours = 7.5h
        val unpaidBreakConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 30,
            deductBreakFromSalary = true
        )
        assertEquals(BigDecimal("7.500000"), unpaidBreakConfig.dailyPaidHours)

        // 8h shift, 30m paid break -> paid hours = 8.0h
        val paidBreakConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 30,
            deductBreakFromSalary = false
        )
        assertEquals(BigDecimal("8"), paidBreakConfig.dailyPaidHours)
    }

    @Test
    fun testRebootRecoveryModel() {
        // Shift was running, uptime was 100,000,000 ms.
        // Device reboots. After reboot, nowElapsed is only 15,000 ms (15 sec since boot).
        // Wall clock difference between now and last resume is 25 minutes (1,500,000 ms).
        val shift = Shift(
            startEpochMillis = 1700000000000L,
            startElapsedRealtime = 100000000L,
            lastResumeElapsedRealtime = 100000000L,
            lastResumeEpochMillis = 1700000000000L,
            accumulatedActiveElapsedMs = 0L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        val nowElapsedAfterReboot = 15000L // Uptime reset to 15s
        val nowEpochAfterReboot = 1700000000000L + (25 * 60 * 1000L) // 25 min later

        val duration = calculator.calculateActiveDurationMs(
            shift,
            nowElapsed = nowElapsedAfterReboot,
            nowEpoch = nowEpochAfterReboot
        )

        // Must recover 25 minutes from epoch anchor, and never negative or huge corrupted values!
        assertEquals(25 * 60 * 1000L, duration)
    }
}
