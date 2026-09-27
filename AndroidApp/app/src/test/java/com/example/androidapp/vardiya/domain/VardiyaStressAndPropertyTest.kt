package com.example.androidapp.vardiya.domain

import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

class VardiyaStressAndPropertyTest {

    private lateinit var calculator: ShiftEarningsCalculator

    @Before
    fun setUp() {
        calculator = ShiftEarningsCalculator()
    }

    @Test
    fun test1000SalaryConfigurationsStressAndProperties() {
        val random = Random(42)

        for (i in 1..1000) {
            val salaryAmount = (1000 + random.nextInt(500000)).toString()
            val workDays = 1 + random.nextInt(31)
            val workHours = (4 + random.nextInt(12)).toString()
            val breakMinutes = random.nextInt(120)
            val deductBreak = random.nextBoolean()

            // Break cannot exceed total work minutes when deducted
            val safeBreak = if (deductBreak) {
                breakMinutes.coerceAtMost((workHours.toInt() * 60) - 30).coerceAtLeast(0)
            } else {
                breakMinutes
            }

            val config = SalaryConfiguration(
                monthlySalary = BigDecimal(salaryAmount),
                monthlyWorkDays = workDays,
                dailyWorkHours = BigDecimal(workHours),
                breakMinutes = safeBreak,
                deductBreakFromSalary = deductBreak
            )

            // Invariant: Hourly rate > 0
            assertTrue("Hourly rate must be positive", config.hourlyRate > BigDecimal.ZERO)
            // Invariant: Minute rate > 0
            assertTrue("Minute rate must be positive", config.minuteRate > BigDecimal.ZERO)
            // Invariant: Second rate > 0
            assertTrue("Second rate must be positive", config.secondRate > BigDecimal.ZERO)

            // Invariant: hourlyRate = minuteRate * 60 (within scale 10)
            val hourlyFromMinute = config.minuteRate.multiply(BigDecimal(60)).setScale(8, RoundingMode.HALF_UP)
            val actualHourly = config.hourlyRate.setScale(8, RoundingMode.HALF_UP)
            assertEquals("hourly = minute * 60", actualHourly, hourlyFromMinute)

            // Invariant: minuteRate = secondRate * 60 (within scale 8)
            val minuteFromSecond = config.secondRate.multiply(BigDecimal(60)).setScale(8, RoundingMode.HALF_UP)
            val actualMinute = config.minuteRate.setScale(8, RoundingMode.HALF_UP)
            assertEquals("minute = second * 60", actualMinute, minuteFromSecond)
        }
    }

    @Test
    fun test1000ShiftsElapsedCalculationsStress() {
        val random = Random(1337)
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("30000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8")
        )

        for (i in 1..1000) {
            val startEpoch = 1700000000000L + random.nextLong(1000000000L)
            val startElapsed = 100000L + random.nextLong(500000000L)
            val shift = Shift(
                startEpochMillis = startEpoch,
                startElapsedRealtime = startElapsed,
                lastResumeElapsedRealtime = startElapsed,
                lastResumeEpochMillis = startEpoch,
                state = ShiftState.RUNNING,
                salaryConfig = config
            )

            // Simulate elapsed between 0 and 72 hours
            val simulatedElapsedMs = random.nextLong(72 * 3600 * 1000L)
            val nowElapsed = startElapsed + simulatedElapsedMs
            val nowEpoch = startEpoch + simulatedElapsedMs

            val calculatedDuration = calculator.calculateActiveDurationMs(shift, nowElapsed, nowEpoch)
            val earnings = calculator.calculateEarnings(shift, calculatedDuration)

            // Invariant: Duration >= 0
            assertTrue("Duration >= 0", calculatedDuration >= 0L)
            // Invariant: Earned >= 0
            assertTrue("Earned >= 0", earnings.earnedAmount >= BigDecimal.ZERO)
            // Invariant: Duration equals simulated duration
            assertEquals(simulatedElapsedMs, calculatedDuration)
        }
    }

    @Test
    fun test1000StateTransitionsStress() {
        val random = Random(9999)
        val allStates = ShiftState.values()

        for (i in 1..1000) {
            val fromState = allStates[random.nextInt(allStates.size)]
            val toState = allStates[random.nextInt(allStates.size)]

            val canTransition = fromState.canTransitionTo(toState)

            // Verify against ground truth specification:
            val expected = when (fromState) {
                ShiftState.NOT_STARTED -> toState == ShiftState.RUNNING
                ShiftState.RUNNING -> toState == ShiftState.PAUSED || toState == ShiftState.FINISHED
                ShiftState.PAUSED -> toState == ShiftState.RUNNING || toState == ShiftState.FINISHED
                ShiftState.FINISHED -> toState == ShiftState.NOT_STARTED
            }

            assertEquals("Transition from $fromState to $toState validity", expected, canTransition)
        }
    }

    @Test
    fun testPropertyInvariants() {
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("35000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8")
        )

        val shift = Shift(
            startEpochMillis = 1000000L,
            startElapsedRealtime = 500000L,
            lastResumeElapsedRealtime = 500000L,
            lastResumeEpochMillis = 1000000L,
            state = ShiftState.RUNNING,
            salaryConfig = config
        )

        // Invariant 1: Increasing elapsed CANNOT decrease earnings (monotonicity)
        var previousEarned = BigDecimal.ZERO
        for (sec in 0..3600 step 10) {
            val durationMs = sec * 1000L
            val earnings = calculator.calculateEarnings(shift, durationMs)
            assertTrue(
                "Earnings must be monotonically non-decreasing",
                earnings.earnedAmount >= previousEarned
            )
            previousEarned = earnings.earnedAmount
        }

        // Invariant 2: Same inputs => exactly same earnings (determinism)
        val earningsA = calculator.calculateEarnings(shift, 12345678L)
        val earningsB = calculator.calculateEarnings(shift, 12345678L)
        assertEquals(earningsA.earnedAmount, earningsB.earnedAmount)
        assertEquals(earningsA.formattedEarned, earningsB.formattedEarned)

        // Invariant 3: Formatted display does not mutate or degrade raw BigDecimal precision
        val rawPrecision = earningsA.earnedAmount.scale()
        assertTrue("Internal scale must be high-precision (16)", rawPrecision >= 16)
        // Formatting is accessed
        val display = earningsA.formattedEarned
        assertFalse(display.isEmpty())
        assertEquals("Raw precision remains intact after formatting", rawPrecision, earningsA.earnedAmount.scale())
    }
}
