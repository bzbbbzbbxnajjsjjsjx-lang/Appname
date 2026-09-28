package com.example.androidapp.vardiya.domain.calculator

import com.example.androidapp.vardiya.domain.model.BreakRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.model.ShiftTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.TimeZone

/**
 * Comprehensive test suite validating Vardiya 3.0 Phase C:
 * - Overtime calculation (threshold, 1.5x / 2.0x multipliers)
 * - Night shift differential calculation (20:00-06:00 window, midnight crossing, multi-day)
 * - Break tracking and optional salary deductions
 * - ShiftTemplate model validation and presets
 * - BreakRecord lifecycle and duration resolution
 * - Combined money engine arithmetic and extreme boundary conditions
 */
class ShiftEarningsCalculatorPhaseCTest {

    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var standardConfig: SalaryConfiguration
    private val gmtTimeZone = TimeZone.getTimeZone("GMT")

    @Before
    fun setUp() {
        calculator = ShiftEarningsCalculator()
        // 28,000 TL / (22 * 8) = 159.090909... TL/hr
        standardConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("28000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8")
        )
    }

    // =========================================================================
    // 1. OVERTIME CALCULATION TESTS
    // =========================================================================

    @Test
    fun testUnderDailyThresholdHasZeroOvertime() {
        val config = standardConfig.copy(
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50")
        )
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        // 6 hours active (< 8h threshold)
        val sixHoursMs = 6 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, sixHoursMs)

        assertEquals(sixHoursMs, earnings.regularDurationMs)
        assertEquals(0L, earnings.overtimeDurationMs)
        assertFalse(earnings.isOvertimeActive)
        assertEquals(0, BigDecimal.ZERO.compareTo(earnings.overtimeEarned))

        val expectedBase = config.hourlyRate.multiply(BigDecimal(6)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedBase, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testExactThresholdHasZeroOvertime() {
        val config = standardConfig.copy(
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50")
        )
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        // Exactly 8 hours
        val eightHoursMs = 8 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, eightHoursMs)

        assertEquals(eightHoursMs, earnings.regularDurationMs)
        assertEquals(0L, earnings.overtimeDurationMs)
        assertFalse(earnings.isOvertimeActive)
        assertEquals(0, BigDecimal.ZERO.compareTo(earnings.overtimeEarned))

        val expectedBase = config.hourlyRate.multiply(BigDecimal(8)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedBase, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testOvertimeCalculatesWithOneAndHalfMultiplier() {
        val config = standardConfig.copy(
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50")
        )
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        // 10 hours: 8h base + 2h overtime
        val tenHoursMs = 10 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, tenHoursMs)

        assertEquals(8 * 3_600_000L, earnings.regularDurationMs)
        assertEquals(2 * 3_600_000L, earnings.overtimeDurationMs)
        assertTrue(earnings.isOvertimeActive)

        // 8h base = 8 * hourlyRate
        val expectedBase = config.hourlyRate.multiply(BigDecimal(8)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedBase, earnings.baseEarned.setScale(2, RoundingMode.HALF_UP))

        // 2h overtime = 2 * (hourlyRate * 1.5) = 3 * hourlyRate
        val expectedOvertime = config.hourlyRate.multiply(BigDecimal("1.50")).multiply(BigDecimal(2)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedOvertime, earnings.overtimeEarned.setScale(2, RoundingMode.HALF_UP))

        // Total = 8 * rate + 3 * rate = 11 * rate
        val expectedTotal = config.hourlyRate.multiply(BigDecimal(11)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedTotal, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testDoubleTimeOvertimeMultiplier() {
        val config = standardConfig.copy(
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("2.00")
        )
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        // 12 hours: 8h base + 4h at 2.0x
        val twelveHoursMs = 12 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, twelveHoursMs)

        assertEquals(4 * 3_600_000L, earnings.overtimeDurationMs)
        // 4h * 2.0 = 8h equivalent overtime pay
        val expectedOvertime = config.hourlyRate.multiply(BigDecimal(8)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedOvertime, earnings.overtimeEarned.setScale(2, RoundingMode.HALF_UP))

        // Total = 8h base + 8h overtime = 16h equivalent pay
        val expectedTotal = config.hourlyRate.multiply(BigDecimal(16)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedTotal, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testOvertimeWithDeductedBreakSchedule() {
        // Daily hours: 8h, break: 60min deducted -> dailyPaidHours = 7h
        val config = standardConfig.copy(
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 60,
            deductBreakFromSalary = true,
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50")
        )
        val shift = Shift(salaryConfig = config, state = ShiftState.RUNNING)

        // Working 9 hours -> 7h regular threshold, 2h overtime
        val nineHoursMs = 9 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, nineHoursMs)

        assertEquals(7 * 3_600_000L, earnings.regularDurationMs)
        assertEquals(2 * 3_600_000L, earnings.overtimeDurationMs)
    }

    // =========================================================================
    // 2. NIGHT SHIFT DIFFERENTIAL CALCULATION TESTS
    // =========================================================================

    @Test
    fun testCalculateNightShiftDurationWindowCrossingMidnight() {
        // Night window: 20:00 to 06:00
        val cal = Calendar.getInstance(gmtTimeZone).apply {
            set(2026, Calendar.SEPTEMBER, 28, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        // 1. Fully within night window: 22:00 to 04:00 (6 hours)
        val start1 = dayStart + (22 * 3_600_000L)
        val end1 = dayStart + ((24 + 4) * 3_600_000L)
        val nightMs1 = calculator.calculateNightShiftDurationMs(start1, end1, 20, 6, gmtTimeZone)
        assertEquals(6 * 3_600_000L, nightMs1)

        // 2. Exact night window: 20:00 to 06:00 next day (10 hours)
        val start2 = dayStart + (20 * 3_600_000L)
        val end2 = dayStart + ((24 + 6) * 3_600_000L)
        val nightMs2 = calculator.calculateNightShiftDurationMs(start2, end2, 20, 6, gmtTimeZone)
        assertEquals(10 * 3_600_000L, nightMs2)

        // 3. Shift from 18:00 to 22:00 (4 hours total, 20:00-22:00 = 2 hours night)
        val start3 = dayStart + (18 * 3_600_000L)
        val end3 = dayStart + (22 * 3_600_000L)
        val nightMs3 = calculator.calculateNightShiftDurationMs(start3, end3, 20, 6, gmtTimeZone)
        assertEquals(2 * 3_600_000L, nightMs3)

        // 4. Pure daytime shift: 09:00 to 17:00 (0 hours night)
        val start4 = dayStart + (9 * 3_600_000L)
        val end4 = dayStart + (17 * 3_600_000L)
        val nightMs4 = calculator.calculateNightShiftDurationMs(start4, end4, 20, 6, gmtTimeZone)
        assertEquals(0L, nightMs4)
    }

    @Test
    fun testCalculateNightShiftDurationWindowWithinSameDay() {
        // Window: 01:00 to 05:00 (does not cross midnight)
        val cal = Calendar.getInstance(gmtTimeZone).apply {
            set(2026, Calendar.SEPTEMBER, 28, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        // Shift 00:00 to 06:00 (6 hours)
        val start = dayStart
        val end = dayStart + (6 * 3_600_000L)
        val nightMs = calculator.calculateNightShiftDurationMs(start, end, 1, 5, gmtTimeZone)
        assertEquals(4 * 3_600_000L, nightMs)
    }

    @Test
    fun testCalculateNightShiftMultiDayShift() {
        // Continuous 48-hour shift starting at 00:00 Day 1
        val cal = Calendar.getInstance(gmtTimeZone).apply {
            set(2026, Calendar.SEPTEMBER, 28, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val end = dayStart + (48 * 3_600_000L)

        // Night window: 20:00 to 06:00 (10 hours per night)
        // Day 1: 00:00-06:00 (6h) + 20:00-24:00 (4h) = 10h
        // Day 2: 00:00-06:00 (6h) + 20:00-24:00 (4h) = 10h
        // Total = 20 hours
        val nightMs = calculator.calculateNightShiftDurationMs(dayStart, end, 20, 6, gmtTimeZone)
        assertEquals(20 * 3_600_000L, nightMs)
    }

    @Test
    fun testNightDifferentialEarningsApplied() {
        val config = standardConfig.copy(
            isNightDifferentialEnabled = true,
            nightDifferentialRate = BigDecimal("0.15"),
            nightShiftStartHour = 20,
            nightShiftEndHour = 6
        )

        // Shift starting at 20:00 and running 8 hours to 04:00 next day
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startEpoch = cal.timeInMillis
        val eightHoursMs = 8 * 3_600_000L

        val shift = Shift(
            startEpochMillis = startEpoch,
            startElapsedRealtime = 1000000L,
            lastResumeElapsedRealtime = 1000000L,
            state = ShiftState.RUNNING,
            salaryConfig = config
        )

        val earnings = calculator.calculateEarnings(
            shift = shift,
            activeDurationMs = eightHoursMs,
            nowEpoch = startEpoch + eightHoursMs
        )

        assertEquals(eightHoursMs, earnings.nightShiftDurationMs)
        assertTrue(earnings.isNightShiftActive)

        // Regular base = 8 * hourlyRate
        val expectedBase = config.hourlyRate.multiply(BigDecimal(8)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedBase, earnings.baseEarned.setScale(2, RoundingMode.HALF_UP))

        // Night diff = 8 * hourlyRate * 0.15 = 1.2 * hourlyRate
        val expectedNight = config.hourlyRate.multiply(BigDecimal("0.15")).multiply(BigDecimal(8)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedNight, earnings.nightDifferentialEarned.setScale(2, RoundingMode.HALF_UP))

        // Total = 8 * rate + 1.2 * rate = 9.2 * rate
        val expectedTotal = config.hourlyRate.multiply(BigDecimal("9.20")).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedTotal, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    // =========================================================================
    // 3. BREAK RECORD & DEDUCTION TESTS
    // =========================================================================

    @Test
    fun testBreakRecordLifecycleAndDuration() {
        val startEpoch = 1700000000000L

        // Ongoing break
        val ongoingBreak = BreakRecord(
            startEpochMillis = startEpoch,
            isDeductedFromSalary = true,
            note = "Öğle Yemeği"
        )
        assertTrue(ongoingBreak.isOngoing)
        assertNull(ongoingBreak.endEpochMillis)
        assertEquals("Öğle Yemeği", ongoingBreak.note)

        // Duration after 30 minutes
        val thirtyMinLater = startEpoch + (30 * 60 * 1000L)
        assertEquals(30 * 60 * 1000L, ongoingBreak.getDurationMs(thirtyMinLater))

        // Completed break
        val finishedBreak = ongoingBreak.copy(endEpochMillis = thirtyMinLater)
        assertFalse(finishedBreak.isOngoing)
        assertEquals(thirtyMinLater, finishedBreak.endEpochMillis)
        // Duration should be fixed at 30 minutes regardless of later nowEpoch
        assertEquals(30 * 60 * 1000L, finishedBreak.getDurationMs(startEpoch + (60 * 60 * 1000L)))
    }

    @Test
    fun testUnpaidBreakDeductionFromEarnings() {
        val config = standardConfig
        val shiftStart = 1700000000000L

        // 30 minute unpaid deducted break
        val breakRecord = BreakRecord(
            startEpochMillis = shiftStart + (2 * 3600 * 1000L),
            endEpochMillis = shiftStart + (2 * 3600 * 1000L) + (30 * 60 * 1000L),
            isDeductedFromSalary = true
        )

        val shift = Shift(
            startEpochMillis = shiftStart,
            state = ShiftState.RUNNING,
            salaryConfig = config,
            activeBreaks = listOf(breakRecord)
        )

        // Active duration: 4 hours
        val fourHoursMs = 4 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, fourHoursMs, shiftStart + fourHoursMs)

        assertEquals(30 * 60 * 1000L, earnings.deductedBreakDurationMs)
        // Deduction = 0.5 * hourlyRate
        val expectedDeduction = config.hourlyRate.multiply(BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedDeduction, earnings.unpaidBreakDeduction.setScale(2, RoundingMode.HALF_UP))

        // Net earned = 4h base - 0.5h break = 3.5h pay
        val expectedNet = config.hourlyRate.multiply(BigDecimal("3.5")).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedNet, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testPaidBreakIsNotDeducted() {
        val config = standardConfig
        val shiftStart = 1700000000000L

        // 45 minute paid break (isDeductedFromSalary = false)
        val paidBreak = BreakRecord(
            startEpochMillis = shiftStart + (3600 * 1000L),
            endEpochMillis = shiftStart + (3600 * 1000L) + (45 * 60 * 1000L),
            isDeductedFromSalary = false
        )

        val shift = Shift(
            startEpochMillis = shiftStart,
            state = ShiftState.RUNNING,
            salaryConfig = config,
            activeBreaks = listOf(paidBreak)
        )

        val fourHoursMs = 4 * 3_600_000L
        val earnings = calculator.calculateEarnings(shift, fourHoursMs, shiftStart + fourHoursMs)

        assertEquals(0L, earnings.deductedBreakDurationMs)
        assertEquals(0, BigDecimal.ZERO.compareTo(earnings.unpaidBreakDeduction))

        val expectedEarned = config.hourlyRate.multiply(BigDecimal(4)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedEarned, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    // =========================================================================
    // 4. COMBINED OVERTIME + NIGHT SHIFT + BREAK DEDUCTION
    // =========================================================================

    @Test
    fun testCombinedOvertimeNightDiffAndBreakDeduction() {
        val config = standardConfig.copy(
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50"),
            isNightDifferentialEnabled = true,
            nightDifferentialRate = BigDecimal("0.15"),
            nightShiftStartHour = 20,
            nightShiftEndHour = 6
        )

        // Shift starts at 16:00 and runs 12 hours until 04:00 next day
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startEpoch = cal.timeInMillis
        val twelveHoursMs = 12 * 3_600_000L
        val endEpoch = startEpoch + twelveHoursMs

        // 1 hour deducted break taken from 18:00 to 19:00
        val deductedBreak = BreakRecord(
            startEpochMillis = startEpoch + (2 * 3_600_000L),
            endEpochMillis = startEpoch + (3 * 3_600_000L),
            isDeductedFromSalary = true
        )

        val shift = Shift(
            startEpochMillis = startEpoch,
            state = ShiftState.RUNNING,
            salaryConfig = config,
            activeBreaks = listOf(deductedBreak)
        )

        val earnings = calculator.calculateEarnings(shift, twelveHoursMs, endEpoch)

        // Breakdown:
        // Regular duration: 8h threshold
        assertEquals(8 * 3_600_000L, earnings.regularDurationMs)
        // Overtime duration: 12h - 8h = 4h
        assertEquals(4 * 3_600_000L, earnings.overtimeDurationMs)
        assertTrue(earnings.isOvertimeActive)

        // Night window: 20:00 to 04:00 = 8h night shift
        assertEquals(8 * 3_600_000L, earnings.nightShiftDurationMs)
        assertTrue(earnings.isNightShiftActive)

        // Deducted break: 1h
        assertEquals(3_600_000L, earnings.deductedBreakDurationMs)

        // Rates:
        // Base = 8 * hourlyRate
        // Overtime = 4 * 1.5 * hourlyRate = 6 * hourlyRate
        // Night Diff = 8 * 0.15 * hourlyRate = 1.2 * hourlyRate
        // Break Deduction = 1 * hourlyRate
        // Net = 8 + 6 + 1.2 - 1 = 14.2 * hourlyRate
        val expectedNet = config.hourlyRate.multiply(BigDecimal("14.20")).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedNet, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))

        // Verify formatted output
        assertTrue(earnings.formattedBaseEarned.contains(config.currencySymbol))
        assertTrue(earnings.formattedOvertimeEarned.contains(config.currencySymbol))
        assertTrue(earnings.formattedNightDifferentialEarned.contains(config.currencySymbol))
        assertTrue(earnings.formattedUnpaidBreakDeduction.contains(config.currencySymbol))
    }

    // =========================================================================
    // 5. SHIFT TEMPLATE MODEL & PRESET TESTS
    // =========================================================================

    @Test
    fun testShiftTemplatePresetsValidity() {
        val presets = ShiftTemplate.PRESETS
        assertEquals(3, presets.size)

        val morning = presets.first { it.id == "preset_morning" }
        assertEquals("Sabah Vardiyası", morning.name)
        assertEquals(8, morning.startHour)
        assertEquals(0, morning.startMinute)
        assertEquals(BigDecimal("8.0"), morning.durationHours)

        val evening = presets.first { it.id == "preset_evening" }
        assertEquals("Akşam Vardiyası", evening.name)
        assertEquals(16, evening.startHour)

        val night = presets.first { it.id == "preset_night" }
        assertEquals("Gece Vardiyası", night.name)
        assertEquals(0, night.startHour)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testShiftTemplateRejectsBlankName() {
        ShiftTemplate(name = "", startHour = 8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testShiftTemplateRejectsInvalidHour() {
        ShiftTemplate(name = "Invalid", startHour = 25)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testShiftTemplateRejectsExceedingBreak() {
        // 8 hours shift, 500 min break deducted (> 8 hours)
        ShiftTemplate(
            name = "OverBreak",
            startHour = 8,
            durationHours = BigDecimal("8.0"),
            breakMinutes = 500,
            deductBreakFromSalary = true
        )
    }

    // =========================================================================
    // 6. EXTREME BOUNDARY & ROUNDING CONDITIONS
    // =========================================================================

    @Test
    fun testMicroSalaryWithOvertimeAndNightDiff() {
        val microConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("0.01"),
            monthlyWorkDays = 31,
            dailyWorkHours = BigDecimal("24"),
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50"),
            isNightDifferentialEnabled = true,
            nightDifferentialRate = BigDecimal("0.15")
        )
        val shift = Shift(salaryConfig = microConfig, state = ShiftState.RUNNING)

        val earnings = calculator.calculateEarnings(shift, 3600000L)
        assertTrue(earnings.earnedAmount >= BigDecimal.ZERO)
        assertTrue(earnings.hourlyRate > BigDecimal.ZERO)
    }

    @Test
    fun testMegaSalaryWithOvertimeCalculations() {
        val megaConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("100000000"), // 100M TL
            monthlyWorkDays = 20,
            dailyWorkHours = BigDecimal("8"),
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("2.00")
        )
        val shift = Shift(salaryConfig = megaConfig, state = ShiftState.RUNNING)

        // 10 hours = 8h base + 2h * 2.0 overtime = 12h pay
        val earnings = calculator.calculateEarnings(shift, 10 * 3_600_000L)
        val expected = megaConfig.hourlyRate.multiply(BigDecimal(12)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testDeductionsCannotProduceNegativeTotalEarnings() {
        val config = standardConfig
        // Huge break deduction exceeding active work
        val hugeBreak = BreakRecord(
            startEpochMillis = 1000000L,
            endEpochMillis = 1000000L + (100 * 3_600_000L),
            isDeductedFromSalary = true
        )
        val shift = Shift(
            startEpochMillis = 1000000L,
            state = ShiftState.RUNNING,
            salaryConfig = config,
            activeBreaks = listOf(hugeBreak)
        )

        val earnings = calculator.calculateEarnings(shift, 3600000L, 1000000L + (100 * 3_600_000L))
        // Must never go below ZERO
        assertEquals(0, BigDecimal.ZERO.compareTo(earnings.earnedAmount))
    }
}
