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

class ShiftEarningsCalculatorBoundaryTest {

    private lateinit var calculator: ShiftEarningsCalculator

    @Before
    fun setUp() {
        calculator = ShiftEarningsCalculator()
    }

    @Test
    fun testZeroAndNegativeDurationSafety() {
        val config = SalaryConfiguration(monthlySalary = BigDecimal("30000"))
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        val zeroEarnings = calculator.calculateEarnings(shift, 0L)
        assertEquals(0, BigDecimal.ZERO.compareTo(zeroEarnings.earnedAmount))
        assertEquals(0L, zeroEarnings.activeDurationMs)

        val negativeEarnings = calculator.calculateEarnings(shift, -5000L)
        assertEquals(0, BigDecimal.ZERO.compareTo(negativeEarnings.earnedAmount))
        assertEquals(-5000L, negativeEarnings.activeDurationMs)
    }

    @Test
    fun testMicroDurationsOneMsAndFiveHundredMs() {
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8")
        )
        // Hourly rate = 28000 / (22 * 8) = 159.090909... TL/hr
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        val oneMsEarnings = calculator.calculateEarnings(shift, 1L)
        assertTrue(oneMsEarnings.earnedAmount > BigDecimal.ZERO)
        assertTrue(oneMsEarnings.earnedAmount < BigDecimal("0.001"))

        val fiveHundredMsEarnings = calculator.calculateEarnings(shift, 500L)
        assertTrue(fiveHundredMsEarnings.earnedAmount > oneMsEarnings.earnedAmount)
        // 500 ms = 0.5 sec. Second rate is ~0.044 TL/sec, so 500ms is ~0.022 TL
        assertEquals(BigDecimal("0.02"), fiveHundredMsEarnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testSevenDayCapEnforcement() {
        val config = SalaryConfiguration()
        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 1000000L,
            lastResumeEpochMillis = 1000000L,
            lastResumeElapsedRealtime = 1000000L,
            state = ShiftState.RUNNING,
            salaryConfig = config
        )

        val eightDaysMs = 8 * 24 * 3600 * 1000L
        val activeMs = calculator.calculateActiveDurationMs(
            shift = shift,
            nowElapsed = 1000000L + eightDaysMs,
            nowEpoch = 1000000L + eightDaysMs
        )

        // Must be capped strictly at 7 days = ShiftEarningsCalculator.MAX_REASONABLE_SHIFT_MS
        assertEquals(ShiftEarningsCalculator.MAX_REASONABLE_SHIFT_MS, activeMs)
        assertEquals(7 * 24 * 3600 * 1000L, activeMs)
    }

    @Test
    fun testRebootDetectionAndWallClockFallback() {
        val shift = Shift(
            startEpochMillis = 1700000000000L,
            startElapsedRealtime = 50000000L, // Phone had 50,000s uptime
            lastResumeEpochMillis = 1700000000000L,
            lastResumeElapsedRealtime = 50000000L,
            state = ShiftState.RUNNING
        )

        // Reboot occurred: nowElapsed reset to 5,000s (< 50,000s)
        // Wall clock advanced by 3 hours (10,800,000 ms)
        val nowElapsedAfterReboot = 5000000L
        val nowEpochAfterReboot = 1700000000000L + 10800000L

        val activeDuration = calculator.calculateActiveDurationMs(shift, nowElapsedAfterReboot, nowEpochAfterReboot)
        assertEquals(10800000L, activeDuration)
    }

    @Test
    fun testRebootWithBackwardsWallClockTampering() {
        val shift = Shift(
            startEpochMillis = 1700000000000L,
            startElapsedRealtime = 50000000L,
            lastResumeEpochMillis = 1700000000000L,
            lastResumeElapsedRealtime = 50000000L,
            state = ShiftState.RUNNING
        )

        // Reboot occurred, and user turned clock backwards before startEpoch
        val nowElapsedAfterReboot = 1000L
        val nowEpochTampered = 1699999000000L // earlier than start

        val activeDuration = calculator.calculateActiveDurationMs(shift, nowElapsedAfterReboot, nowEpochTampered)
        assertEquals(0L, activeDuration) // Coerced safely to 0, no negative crash
    }

    @Test
    fun testCrossMidnightShiftBoundary() {
        // Shift started at 22:00 (10 PM) and ended at 06:00 (6 AM) next day = 8 hours
        val startEpoch = 1700082000000L // 22:00
        val endEpoch = startEpoch + (8 * 3600 * 1000L) // 06:00 next day
        val elapsedStart = 1000000L
        val elapsedEnd = elapsedStart + (8 * 3600 * 1000L)

        val shift = Shift(
            startEpochMillis = startEpoch,
            startElapsedRealtime = elapsedStart,
            lastResumeEpochMillis = startEpoch,
            lastResumeElapsedRealtime = elapsedStart,
            accumulatedActiveElapsedMs = 0L,
            state = ShiftState.RUNNING,
            salaryConfig = SalaryConfiguration(
                monthlySalary = BigDecimal("35200"),
                monthlyWorkDays = 22,
                dailyWorkHours = BigDecimal("8")
            ) // Hourly rate = 35200 / 176 = 200 TL/hr
        )

        val activeDuration = calculator.calculateActiveDurationMs(shift, elapsedEnd, endEpoch)
        assertEquals(8 * 3600 * 1000L, activeDuration)

        val earnings = calculator.calculateEarnings(shift, activeDuration)
        // 8 hours * 200 TL/hr = 1600.00 TL
        assertEquals(0, BigDecimal("1600.00").compareTo(earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP)))
        assertEquals("₺1.600,00", earnings.formattedEarned)
    }

    @Test
    fun testExtremeSalaryValues() {
        // Micro salary: 0.01 TL / month
        val microConfig = SalaryConfiguration(monthlySalary = BigDecimal("0.01"))
        val microShift = Shift(salaryConfig = microConfig, state = ShiftState.RUNNING)
        val microEarnings = calculator.calculateEarnings(microShift, 3600000L) // 1 hour
        assertTrue(microEarnings.earnedAmount > BigDecimal.ZERO)
        assertEquals("₺0,00", microEarnings.formattedEarned) // Renders 0,00 safely

        // Mega salary: 100,000,000 TL / month
        val megaConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("100000000"),
            monthlyWorkDays = 20,
            dailyWorkHours = BigDecimal("8")
        ) // Hourly rate = 100,000,000 / 160 = 625,000 TL/hr
        val megaShift = Shift(salaryConfig = megaConfig, state = ShiftState.RUNNING)
        val megaEarnings = calculator.calculateEarnings(megaShift, 3600000L) // 1 hour
        assertEquals(0, BigDecimal("625000.00").compareTo(megaEarnings.earnedAmount.setScale(2, RoundingMode.HALF_UP)))
        assertEquals("₺625.000,00", megaEarnings.formattedEarned)
    }

    @Test
    fun testMathematicalDeterminismAndIdempotency() {
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("45678.90"),
            monthlyWorkDays = 21,
            dailyWorkHours = BigDecimal("7.5")
        )
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)
        val durationMs = 12345678L // ~3.43 hours

        val firstResult = calculator.calculateEarnings(shift, durationMs)
        for (i in 0 until 500) {
            val repeated = calculator.calculateEarnings(shift, durationMs)
            assertEquals(firstResult.earnedAmount, repeated.earnedAmount)
            assertEquals(firstResult.formattedEarned, repeated.formattedEarned)
            assertEquals(firstResult.formattedDuration, repeated.formattedDuration)
        }
    }
}
