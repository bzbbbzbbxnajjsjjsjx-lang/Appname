package com.example.androidapp.vardiya.domain.calculator

import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ShiftEarningsCalculatorTest {

    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var standardConfig: SalaryConfiguration

    @Before
    fun setUp() {
        calculator = ShiftEarningsCalculator()
        // 176 hours, 159.090909... TL/hr
        standardConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8")
        )
    }

    @Test
    fun testExactOneHourElapsedYieldsHourlyRate() {
        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 500000L,
            lastResumeElapsedRealtime = 500000L,
            accumulatedActiveElapsedMs = 0L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        // 1 hour = 3,600,000 ms
        val nowElapsed = 500000L + 3600000L
        val nowEpoch = 1000000L + 3600000L

        val duration = calculator.calculateActiveDurationMs(shift, nowElapsed, nowEpoch)
        assertEquals(3600000L, duration)

        val earnings = calculator.calculateEarnings(shift, duration)
        val expected = standardConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testHalfHourElapsedYieldsHalfHourlyRate() {
        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 500000L,
            lastResumeElapsedRealtime = 500000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        // 30 mins = 1,800,000 ms
        val nowElapsed = 500000L + 1800000L
        val duration = calculator.calculateActiveDurationMs(shift, nowElapsed, 1000000L + 1800000L)
        assertEquals(1800000L, duration)

        val earnings = calculator.calculateEarnings(shift, duration)
        val expected = standardConfig.hourlyRate.multiply(BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testClockChangeInvarianceUsingMonotonicClock() {
        // User changes system wall clock forward by 10 hours, but actual elapsed realtime is only 15 minutes
        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 500000L,
            lastResumeElapsedRealtime = 500000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        val actualElapsedMs = 15 * 60 * 1000L // 15 mins
        val tamperedWallClockEpoch = 1000000L + (10 * 3600 * 1000L) // +10 hours wall clock
        val actualMonotonicNow = 500000L + actualElapsedMs

        val duration = calculator.calculateActiveDurationMs(shift, actualMonotonicNow, tamperedWallClockEpoch)
        assertEquals(actualElapsedMs, duration)

        val earnings = calculator.calculateEarnings(shift, duration)
        val expected = standardConfig.hourlyRate.multiply(BigDecimal(15).divide(BigDecimal(60), 10, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testMidnightCrossingCorrectlyCalculated() {
        // Shift starts at 23:00 and finishes at 03:00 next day (4 hours)
        val shift = Shift(
            startEpochMillis = 1700000000000L,
            startElapsedRealtime = 10000000L,
            lastResumeElapsedRealtime = 10000000L,
            state = ShiftState.RUNNING,
            salaryConfig = standardConfig
        )

        val fourHoursMs = 4 * 3600 * 1000L
        val duration = calculator.calculateActiveDurationMs(
            shift,
            shift.lastResumeElapsedRealtime + fourHoursMs,
            shift.startEpochMillis + fourHoursMs
        )
        assertEquals(fourHoursMs, duration)

        val earnings = calculator.calculateEarnings(shift, duration)
        val expected = standardConfig.hourlyRate.multiply(BigDecimal(4)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testFinishedShiftPreservesEarningsInvariantly() {
        val fixedEarned = BigDecimal("543.21")
        val finishedShift = Shift(
            state = ShiftState.FINISHED,
            accumulatedActiveElapsedMs = 12345000L,
            totalEarnedWhenFinished = fixedEarned,
            salaryConfig = standardConfig
        )

        val earnings = calculator.calculateEarnings(finishedShift, 12345000L)
        assertEquals(fixedEarned, earnings.earnedAmount)
    }
}
