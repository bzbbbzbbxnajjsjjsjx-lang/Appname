package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.analytics.AnalyticsPeriod
import com.example.androidapp.vardiya.domain.analytics.ShiftAnalyticsEngine
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.BreakRecord
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaAnalyticsPhaseGTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: InMemoryVardiyaRepository
    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var viewModel: VardiyaViewModel

    private class FakeTimeProvider(
        var epochMillis: Long = 1759050000000L,
        var elapsedMillis: Long = 100000L
    ) : TimeProvider {
        override fun currentEpochMillis(): Long = epochMillis
        override fun elapsedRealtimeMillis(): Long = elapsedMillis
    }

    private fun makeEpochForDate(
        year: Int,
        month1Indexed: Int,
        day: Int,
        hour: Int = 9,
        minute: Int = 0
    ): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1Indexed - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun createShift(
        id: String,
        startEpoch: Long,
        durationHours: Long,
        totalEarned: BigDecimal,
        overtimeEarned: BigDecimal = BigDecimal.ZERO,
        nightDifferentialEarned: BigDecimal = BigDecimal.ZERO,
        breaks: List<BreakRecord> = emptyList(),
        historicalRate: BigDecimal = BigDecimal("150")
    ): CompletedShiftRecord {
        val activeMs = durationHours * 3_600_000L
        val finishEpoch = startEpoch + activeMs

        return CompletedShiftRecord(
            id = id,
            dateFormatted = "Tarih $id",
            timeRangeFormatted = "09:00 — 17:00",
            durationFormatted = "${durationHours}s 0dk",
            earnedFormatted = "₺$totalEarned",
            totalEarned = totalEarned,
            activeDurationMs = activeMs,
            totalDurationMs = activeMs,
            startEpochMillis = startEpoch,
            finishEpochMillis = finishEpoch,
            salaryConfigSnapshot = SalaryConfiguration(monthlySalary = historicalRate.multiply(BigDecimal("176"))),
            currencySymbol = "₺",
            currencyCode = "TRY",
            baseEarned = totalEarned.subtract(overtimeEarned).subtract(nightDifferentialEarned),
            overtimeEarned = overtimeEarned,
            nightDifferentialEarned = nightDifferentialEarned,
            regularDurationMs = activeMs,
            overtimeDurationMs = if (overtimeEarned > BigDecimal.ZERO) 7_200_000L else 0L,
            nightShiftDurationMs = if (nightDifferentialEarned > BigDecimal.ZERO) 3_600_000L else 0L,
            breaks = breaks
        )
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryVardiyaRepository()
        calculator = ShiftEarningsCalculator()
        timeProvider = FakeTimeProvider()
        viewModel = VardiyaViewModel(repository, calculator, timeProvider, testDispatcher)
    }

    @After
    fun tearDown() {
        viewModel.stopTicker()
        Dispatchers.resetMain()
    }

    @Test
    fun weeklyAggregation_aggregates7DailyPointsCorrectly() {
        // Reference Wednesday: 2026-09-23
        val wednesdayEpoch = makeEpochForDate(2026, 9, 23, 9)

        // Monday shift: 2026-09-21
        val mondayShift = createShift(
            "mon",
            makeEpochForDate(2026, 9, 21, 9),
            8,
            BigDecimal("1200"),
            overtimeEarned = BigDecimal("200")
        )
        // Wednesday shift: 2026-09-23
        val wednesdayShift = createShift(
            "wed",
            wednesdayEpoch,
            6,
            BigDecimal("900")
        )
        // Friday shift: 2026-09-25
        val fridayShift = createShift(
            "fri",
            makeEpochForDate(2026, 9, 25, 9),
            8,
            BigDecimal("1300"),
            nightDifferentialEarned = BigDecimal("100")
        )
        // Shift outside week (Next Monday 2026-09-28)
        val nextWeekShift = createShift(
            "next",
            makeEpochForDate(2026, 9, 28, 9),
            8,
            BigDecimal("1500")
        )

        val history = listOf(mondayShift, wednesdayShift, fridayShift, nextWeekShift)
        val weekly = ShiftAnalyticsEngine.computeWeeklyAnalytics(history, wednesdayEpoch)

        assertEquals(7, weekly.dailyPoints.size)
        assertEquals(3, weekly.totalShifts)
        assertEquals(BigDecimal("3400"), weekly.totalEarned)
        assertEquals(BigDecimal("200"), weekly.totalOvertimeEarned)
        assertEquals(BigDecimal("100"), weekly.totalNightDifferentialEarned)
        assertEquals(22 * 3_600_000L, weekly.totalDurationMs)

        // Monday is dailyPoints[0]
        assertEquals("Pzt", weekly.dailyPoints[0].dayOfWeekName)
        assertEquals(1, weekly.dailyPoints[0].shiftCount)
        assertEquals(BigDecimal("1200"), weekly.dailyPoints[0].totalEarned)
        assertEquals(BigDecimal("200"), weekly.dailyPoints[0].overtimeEarned)

        // Tuesday is dailyPoints[1] (empty)
        assertEquals("Sal", weekly.dailyPoints[1].dayOfWeekName)
        assertEquals(0, weekly.dailyPoints[1].shiftCount)
        assertEquals(BigDecimal.ZERO, weekly.dailyPoints[1].totalEarned)

        // Wednesday is dailyPoints[2]
        assertEquals("Çar", weekly.dailyPoints[2].dayOfWeekName)
        assertEquals(1, weekly.dailyPoints[2].shiftCount)
        assertEquals(BigDecimal("900"), weekly.dailyPoints[2].totalEarned)

        // Sunday is dailyPoints[6] (empty)
        assertEquals("Paz", weekly.dailyPoints[6].dayOfWeekName)
        assertEquals(0, weekly.dailyPoints[6].shiftCount)
    }

    @Test
    fun monthlyAggregation_aggregatesMonthlyTotalsAndBuckets() {
        // Shifts in September 2026
        val s1 = createShift("s1", makeEpochForDate(2026, 9, 3, 9), 8, BigDecimal("1200"))
        val s2 = createShift("s2", makeEpochForDate(2026, 9, 10, 10), 10, BigDecimal("1800"), overtimeEarned = BigDecimal("400"))
        val s3 = createShift("s3", makeEpochForDate(2026, 9, 18, 9), 8, BigDecimal("1400"), nightDifferentialEarned = BigDecimal("200"))
        val s4 = createShift("s4", makeEpochForDate(2026, 9, 25, 8), 8, BigDecimal("1200"))

        // Shift in October (should not be included in September)
        val sOct = createShift("oct", makeEpochForDate(2026, 10, 1, 9), 8, BigDecimal("1200"))

        val history = listOf(s1, s2, s3, s4, sOct)
        val monthly = ShiftAnalyticsEngine.computeMonthlyAnalytics(history, 2026, 9)

        assertEquals(2026, monthly.year)
        assertEquals(9, monthly.month)
        assertEquals(4, monthly.totalShifts)
        assertEquals(BigDecimal("5600"), monthly.totalEarned)
        assertEquals(BigDecimal("400"), monthly.totalOvertimeEarned)
        assertEquals(BigDecimal("200"), monthly.totalNightDifferentialEarned)
        assertEquals(34 * 3_600_000L, monthly.totalDurationMs)

        // Check buckets (September has 30 days -> 5 buckets: 1-7, 8-14, 15-21, 22-28, 29-30)
        assertEquals(5, monthly.weeklyBuckets.size)
        // Bucket 1 (1-7): contains s1
        assertEquals(1, monthly.weeklyBuckets[0].shiftCount)
        assertEquals(BigDecimal("1200"), monthly.weeklyBuckets[0].totalEarned)
        // Bucket 2 (8-14): contains s2
        assertEquals(1, monthly.weeklyBuckets[1].shiftCount)
        assertEquals(BigDecimal("1800"), monthly.weeklyBuckets[1].totalEarned)
    }

    @Test
    fun breakDeductions_aggregatedInMonthlyAnalytics() {
        val deductedBreak = BreakRecord(
            startEpochMillis = makeEpochForDate(2026, 9, 5, 12, 0),
            endEpochMillis = makeEpochForDate(2026, 9, 5, 12, 45),
            isDeductedFromSalary = true
        )
        val paidBreak = BreakRecord(
            startEpochMillis = makeEpochForDate(2026, 9, 5, 15, 0),
            endEpochMillis = makeEpochForDate(2026, 9, 5, 15, 15),
            isDeductedFromSalary = false
        )

        val shiftWithBreaks = createShift(
            "bShift",
            makeEpochForDate(2026, 9, 5, 9),
            8,
            BigDecimal("1200"),
            breaks = listOf(deductedBreak, paidBreak)
        )

        val monthly = ShiftAnalyticsEngine.computeMonthlyAnalytics(listOf(shiftWithBreaks), 2026, 9)
        assertEquals(45 * 60_000L, monthly.totalBreakDeductionMs)
        assertEquals("0s 45dk", monthly.formattedBreakDeduction)
    }

    @Test
    fun boundaryTest_emptyDatasetReturnsZeroSafeMetricsWithoutCrash() {
        val weekly = ShiftAnalyticsEngine.computeWeeklyAnalytics(emptyList(), makeEpochForDate(2026, 9, 23))
        assertEquals(0, weekly.totalShifts)
        assertEquals(BigDecimal.ZERO, weekly.totalEarned)
        assertEquals(0L, weekly.totalDurationMs)
        assertEquals(BigDecimal.ZERO, weekly.averageDailyEarned)
        assertEquals(BigDecimal.ZERO, weekly.averageHourlyRate)
        assertEquals(7, weekly.dailyPoints.size)

        val monthly = ShiftAnalyticsEngine.computeMonthlyAnalytics(emptyList(), 2026, 9)
        assertEquals(0, monthly.totalShifts)
        assertEquals(BigDecimal.ZERO, monthly.totalEarned)
        assertEquals(BigDecimal.ZERO, monthly.averageShiftEarned)
        assertEquals(BigDecimal.ZERO, monthly.averageHourlyRate)

        val overall = ShiftAnalyticsEngine.computeOverallSummary(emptyList())
        assertEquals(0, overall.totalShifts)
        assertEquals(BigDecimal.ZERO, overall.totalEarned)
    }

    @Test
    fun boundaryTest_multipleShiftsOnSameDay() {
        val epochDay = makeEpochForDate(2026, 9, 22, 8)
        val morningShift = createShift("morning", epochDay, 4, BigDecimal("600"))
        val eveningShift = createShift("evening", makeEpochForDate(2026, 9, 22, 14), 4, BigDecimal("700"), overtimeEarned = BigDecimal("100"))

        val weekly = ShiftAnalyticsEngine.computeWeeklyAnalytics(listOf(morningShift, eveningShift), epochDay)
        assertEquals(2, weekly.totalShifts)
        assertEquals(BigDecimal("1300"), weekly.totalEarned)

        // Tuesday (index 1) has 2 shifts
        val tuesday = weekly.dailyPoints[1]
        assertEquals(2, tuesday.shiftCount)
        assertEquals(BigDecimal("1300"), tuesday.totalEarned)
        assertEquals(BigDecimal("100"), tuesday.overtimeEarned)
        assertEquals(8 * 3_600_000L, tuesday.activeDurationMs)
    }

    @Test
    fun boundaryTest_midnightCrossingShiftAnchorsToStartDay() {
        // Shift starts Sunday 2026-09-20 at 22:00 and finishes Monday 2026-09-21 at 06:00
        val sundayNightStart = makeEpochForDate(2026, 9, 20, 22)
        val midnightShift = createShift(
            "nightCrossing",
            sundayNightStart,
            8,
            BigDecimal("1600"),
            nightDifferentialEarned = BigDecimal("300")
        )

        // Querying week of Sunday 2026-09-20 (week 38): Sunday is last day of that week
        val sundayWeek = ShiftAnalyticsEngine.computeWeeklyAnalytics(listOf(midnightShift), sundayNightStart)
        assertEquals(1, sundayWeek.totalShifts)
        // It belongs to Sunday (dailyPoints[6])
        assertEquals(1, sundayWeek.dailyPoints[6].shiftCount)
        assertEquals(BigDecimal("1600"), sundayWeek.dailyPoints[6].totalEarned)

        // Querying the following week (Monday 2026-09-21): Sunday shift does not spill into Monday
        val mondayEpoch = makeEpochForDate(2026, 9, 21, 10)
        val mondayWeek = ShiftAnalyticsEngine.computeWeeklyAnalytics(listOf(midnightShift), mondayEpoch)
        assertEquals(0, mondayWeek.totalShifts)
        assertEquals(0, mondayWeek.dailyPoints[0].shiftCount)
    }

    @Test
    fun historicalInvariant_preservesFrozenEarningsRegardlessOfCurrentConfig() {
        // Record completed with historical 100 TL / hr rate and 500 TL total
        val frozenRecord = createShift(
            "frozen",
            makeEpochForDate(2026, 9, 15, 9),
            5,
            BigDecimal("500"),
            historicalRate = BigDecimal("100")
        )

        val history = listOf(frozenRecord)
        val weekly = ShiftAnalyticsEngine.computeWeeklyAnalytics(history, makeEpochForDate(2026, 9, 15))

        // Total must be exactly the recorded 500, never recalculated
        assertEquals(BigDecimal("500"), weekly.totalEarned)
        assertEquals(5 * 3_600_000L, weekly.totalDurationMs)
    }

    @Test
    fun viewModelAnalyticsIntegration_navigatesAndSwitchesPeriods() = runTest(testDispatcher) {
        val testEpoch = makeEpochForDate(2026, 9, 23, 10)
        timeProvider.epochMillis = testEpoch

        viewModel.resetAnalyticsToCurrent()

        assertEquals(AnalyticsPeriod.WEEKLY, viewModel.uiState.value.analyticsPeriod)

        // Switch to Monthly
        viewModel.setAnalyticsPeriod(AnalyticsPeriod.MONTHLY)
        assertEquals(AnalyticsPeriod.MONTHLY, viewModel.uiState.value.analyticsPeriod)

        // Navigate Month Previous (9 -> 8)
        viewModel.navigateAnalyticsPrevious()
        assertEquals(8, viewModel.uiState.value.analyticsMonth)
        assertEquals(2026, viewModel.uiState.value.analyticsYear)

        // Navigate Month Next (8 -> 9)
        viewModel.navigateAnalyticsNext()
        assertEquals(9, viewModel.uiState.value.analyticsMonth)

        // Switch back to Weekly
        viewModel.setAnalyticsPeriod(AnalyticsPeriod.WEEKLY)
        val initialAnchor = viewModel.uiState.value.analyticsWeekAnchorMillis

        // Navigate Week Previous
        viewModel.navigateAnalyticsPrevious()
        assertEquals(initialAnchor - 7 * 86_400_000L, viewModel.uiState.value.analyticsWeekAnchorMillis)

        // Navigate Week Next
        viewModel.navigateAnalyticsNext()
        assertEquals(initialAnchor, viewModel.uiState.value.analyticsWeekAnchorMillis)

        // Reset to Current
        viewModel.resetAnalyticsToCurrent()
        assertEquals(testEpoch, viewModel.uiState.value.analyticsWeekAnchorMillis)
    }
}
