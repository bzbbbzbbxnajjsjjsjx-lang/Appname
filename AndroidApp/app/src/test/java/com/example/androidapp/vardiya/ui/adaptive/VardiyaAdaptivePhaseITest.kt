package com.example.androidapp.vardiya.ui.adaptive

import androidx.compose.ui.unit.dp
import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.analytics.ShiftAnalyticsEngine
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.CalendarDate
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.ShiftHistoryFilterEngine
import com.example.androidapp.vardiya.domain.model.ShiftHistoryFilterType
import com.example.androidapp.vardiya.domain.time.TimeProvider
import com.example.androidapp.vardiya.ui.VardiyaViewModel
import com.example.androidapp.vardiya.ui.navigation.WindowWidthSizeClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaAdaptivePhaseITest {

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

    private fun makeEpochForDate(year: Int, month1Indexed: Int, day: Int, hour: Int = 9): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1Indexed - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun createRecord(
        id: String,
        year: Int,
        month1Indexed: Int,
        day: Int,
        durationHours: Long,
        totalEarned: BigDecimal,
        overtimeEarned: BigDecimal = BigDecimal.ZERO,
        nightDifferentialEarned: BigDecimal = BigDecimal.ZERO,
        note: String? = null,
        templateId: String? = null
    ): CompletedShiftRecord {
        val startEpoch = makeEpochForDate(year, month1Indexed, day, 9)
        val activeMs = durationHours * 3_600_000L
        val finishEpoch = startEpoch + activeMs

        return CompletedShiftRecord(
            id = id,
            dateFormatted = "$day Eylül $year",
            timeRangeFormatted = "09:00 — 17:00",
            durationFormatted = "${durationHours}s 0dk",
            earnedFormatted = "₺$totalEarned",
            totalEarned = totalEarned,
            activeDurationMs = activeMs,
            totalDurationMs = activeMs,
            startEpochMillis = startEpoch,
            finishEpochMillis = finishEpoch,
            salaryConfigSnapshot = SalaryConfiguration(),
            currencySymbol = "₺",
            currencyCode = "TRY",
            baseEarned = totalEarned.subtract(overtimeEarned).subtract(nightDifferentialEarned),
            overtimeEarned = overtimeEarned,
            nightDifferentialEarned = nightDifferentialEarned,
            regularDurationMs = activeMs,
            overtimeDurationMs = if (overtimeEarned > BigDecimal.ZERO) 7_200_000L else 0L,
            nightShiftDurationMs = if (nightDifferentialEarned > BigDecimal.ZERO) 3_600_000L else 0L,
            templateId = templateId,
            note = note,
            breaks = emptyList()
        )
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryVardiyaRepository()
        calculator = ShiftEarningsCalculator()
        timeProvider = FakeTimeProvider()
        viewModel = VardiyaViewModel(
            repository = repository,
            calculator = calculator,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        viewModel.stopTicker()
        Dispatchers.resetMain()
    }

    // ==========================================
    // 1. Breakpoints & Window Size Classes
    // ==========================================

    @Test
    fun breakpoints_resolve_boundary_values_accurately() {
        // Compact (< 600dp)
        assertEquals(WindowWidthSizeClass.COMPACT, VardiyaBreakpoints.resolve(0.dp))
        assertEquals(WindowWidthSizeClass.COMPACT, VardiyaBreakpoints.resolve(320.dp))
        assertEquals(WindowWidthSizeClass.COMPACT, VardiyaBreakpoints.resolve(360.dp))
        assertEquals(WindowWidthSizeClass.COMPACT, VardiyaBreakpoints.resolve(411.dp))
        assertEquals(WindowWidthSizeClass.COMPACT, VardiyaBreakpoints.resolve(599.dp))
        assertEquals(WindowWidthSizeClass.COMPACT, VardiyaBreakpoints.resolve(599.9f.dp))

        // Medium (600dp ..< 840dp)
        assertEquals(WindowWidthSizeClass.MEDIUM, VardiyaBreakpoints.resolve(600.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, VardiyaBreakpoints.resolve(600.1f.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, VardiyaBreakpoints.resolve(720.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, VardiyaBreakpoints.resolve(800.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, VardiyaBreakpoints.resolve(839.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, VardiyaBreakpoints.resolve(839.9f.dp))

        // Expanded (>= 840dp)
        assertEquals(WindowWidthSizeClass.EXPANDED, VardiyaBreakpoints.resolve(840.dp))
        assertEquals(WindowWidthSizeClass.EXPANDED, VardiyaBreakpoints.resolve(840.1f.dp))
        assertEquals(WindowWidthSizeClass.EXPANDED, VardiyaBreakpoints.resolve(1024.dp))
        assertEquals(WindowWidthSizeClass.EXPANDED, VardiyaBreakpoints.resolve(1280.dp))
        assertEquals(WindowWidthSizeClass.EXPANDED, VardiyaBreakpoints.resolve(1920.dp))
    }

    @Test
    fun windowWidthSizeClass_convenience_properties_are_correct() {
        val compact = WindowWidthSizeClass.COMPACT
        assertTrue(compact.isCompact)
        assertFalse(compact.isMediumOrExpanded)

        val medium = WindowWidthSizeClass.MEDIUM
        assertFalse(medium.isCompact)
        assertTrue(medium.isMediumOrExpanded)

        val expanded = WindowWidthSizeClass.EXPANDED
        assertFalse(expanded.isCompact)
        assertTrue(expanded.isMediumOrExpanded)
    }

    // ==========================================
    // 2. Selection Preservation Across Breakpoints
    // ==========================================

    @Test
    fun selection_is_preserved_when_screen_transitions_between_compact_and_wide() = runTest(testDispatcher) {
        val record1 = createRecord("rec-1", 2026, 9, 10, 8, BigDecimal("1200.00"), note = "Gündüz")
        val record2 = createRecord("rec-2", 2026, 9, 11, 8, BigDecimal("1500.00"), note = "Gece")
        repository.addShiftToHistory(record1)
        repository.addShiftToHistory(record2)

        // Recreate ViewModel so it loads from repository
        val vm = VardiyaViewModel(repository, calculator, timeProvider, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.uiState.value.history.size)
        assertNull(vm.uiState.value.selectedHistoryRecord)

        // Select record2 on compact screen (simulated)
        val compactSize = VardiyaBreakpoints.resolve(390.dp)
        assertTrue(compactSize.isCompact)
        vm.selectHistoryRecord(record2)
        assertEquals(record2.id, vm.uiState.value.selectedHistoryRecord?.id)

        // Rotate device or unfold foldable to medium (720dp)
        val mediumSize = VardiyaBreakpoints.resolve(720.dp)
        assertTrue(mediumSize.isMediumOrExpanded)
        // Selection remains untouched
        assertEquals(record2.id, vm.uiState.value.selectedHistoryRecord?.id)
        assertEquals("Gece", vm.uiState.value.selectedHistoryRecord?.note)

        // Transition to expanded tablet (1024dp)
        val expandedSize = VardiyaBreakpoints.resolve(1024.dp)
        assertTrue(expandedSize.isMediumOrExpanded)
        assertEquals(record2.id, vm.uiState.value.selectedHistoryRecord?.id)

        // Deselect
        vm.selectHistoryRecord(null)
        assertNull(vm.uiState.value.selectedHistoryRecord)
        vm.stopTicker()
    }

    // ==========================================
    // 3. Search and Filter Interaction with Selection
    // ==========================================

    @Test
    fun filter_and_search_interact_cleanly_with_list_detail_selection() = runTest(testDispatcher) {
        val recordA = createRecord("rec-a", 2026, 9, 1, 8, BigDecimal("1000.00"), note = "Fabrika vardiyası")
        val recordB = createRecord("rec-b", 2026, 9, 2, 8, BigDecimal("1200.00"), note = "Ofis mesaisi")
        val recordC = createRecord("rec-c", 2026, 9, 3, 8, BigDecimal("1500.00"), note = "Depo kontrol")
        repository.addShiftToHistory(recordA)
        repository.addShiftToHistory(recordB)
        repository.addShiftToHistory(recordC)

        val vm = VardiyaViewModel(repository, calculator, timeProvider, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        // Select Record B
        vm.selectHistoryRecord(recordB)
        assertEquals("rec-b", vm.uiState.value.selectedHistoryRecord?.id)

        // Search query filtering out Record B
        val filteredQueryA = ShiftHistoryFilterEngine.filter(
            history = vm.uiState.value.history,
            query = "Fabrika",
            filterType = ShiftHistoryFilterType.ALL,
            selectedDate = null
        )
        assertEquals(1, filteredQueryA.size)
        assertEquals("rec-a", filteredQueryA[0].id)
        // Selection in ViewModel is unaffected
        assertEquals("rec-b", vm.uiState.value.selectedHistoryRecord?.id)

        // Search query matching Record B
        val filteredQueryB = ShiftHistoryFilterEngine.filter(
            history = vm.uiState.value.history,
            query = "Ofis",
            filterType = ShiftHistoryFilterType.ALL,
            selectedDate = null
        )
        assertEquals(1, filteredQueryB.size)
        assertEquals("rec-b", filteredQueryB[0].id)
        assertEquals(filteredQueryB[0].id, vm.uiState.value.selectedHistoryRecord?.id)

        // Clear query
        val resetList = ShiftHistoryFilterEngine.filter(
            history = vm.uiState.value.history,
            query = "",
            filterType = ShiftHistoryFilterType.ALL,
            selectedDate = null
        )
        assertEquals(3, resetList.size)
        assertEquals("rec-b", vm.uiState.value.selectedHistoryRecord?.id)
        vm.stopTicker()
    }

    // ==========================================
    // 4. History Empty State and Clear Safety
    // ==========================================

    @Test
    fun empty_history_produces_safe_summaries_without_exceptions() {
        val emptyList = emptyList<CompletedShiftRecord>()
        val summary = ShiftHistoryFilterEngine.summarize(emptyList)
        assertEquals(0, summary.totalCount)
        assertEquals(BigDecimal.ZERO, summary.totalEarned)
        assertEquals(0L, summary.totalDurationMs)
        assertEquals(BigDecimal.ZERO, summary.totalOvertimeEarned)

        val monthData = ShiftHistoryFilterEngine.aggregateMonth(emptyList, 2026, 9)
        assertNotNull(monthData)
        assertTrue(monthData.isEmpty())
    }

    @Test
    fun clearHistory_resets_selected_record_preventing_dangling_selection() = runTest(testDispatcher) {
        val record = createRecord("rec-del", 2026, 9, 15, 8, BigDecimal("1000.00"))
        repository.addShiftToHistory(record)

        val vm = VardiyaViewModel(repository, calculator, timeProvider, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectHistoryRecord(record)
        assertEquals("rec-del", vm.uiState.value.selectedHistoryRecord?.id)

        // Clear all history
        vm.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.history.isEmpty())
        assertNull(vm.uiState.value.selectedHistoryRecord)
        vm.stopTicker()
    }

    // ==========================================
    // 5. Analytics Aggregation on Wide Screens
    // ==========================================

    @Test
    fun analytics_engine_calculates_overall_and_trend_metrics_safely() {
        // Empty history check
        val emptyOverall = ShiftAnalyticsEngine.computeOverallSummary(emptyList())
        assertEquals(0, emptyOverall.totalShifts)
        assertEquals(BigDecimal.ZERO, emptyOverall.totalEarned)
        assertEquals(BigDecimal.ZERO, emptyOverall.averageShiftEarned)
        assertEquals(0L, emptyOverall.totalDurationMs)

        val refEpoch = makeEpochForDate(2026, 9, 15)
        val emptyWeekly = ShiftAnalyticsEngine.computeWeeklyAnalytics(emptyList(), refEpoch)
        assertEquals(7, emptyWeekly.dailyPoints.size)
        assertEquals(0, emptyWeekly.totalShifts)
        assertEquals(BigDecimal.ZERO, emptyWeekly.totalEarned)

        val emptyMonthly = ShiftAnalyticsEngine.computeMonthlyAnalytics(emptyList(), 2026, 9)
        assertEquals(0, emptyMonthly.totalShifts)
        assertEquals(BigDecimal.ZERO, emptyMonthly.totalEarned)

        // Populated history check
        val r1 = createRecord("r1", 2026, 9, 1, 8, BigDecimal("1000.00"))
        val r2 = createRecord("r2", 2026, 9, 8, 10, BigDecimal("2000.00"))
        val populatedOverall = ShiftAnalyticsEngine.computeOverallSummary(listOf(r1, r2))
        assertEquals(2, populatedOverall.totalShifts)
        assertEquals(BigDecimal("3000.00"), populatedOverall.totalEarned)
        assertEquals(BigDecimal("1500.00"), populatedOverall.averageShiftEarned)
        assertEquals(64_800_000L, populatedOverall.totalDurationMs)
    }

    // ==========================================
    // 6. Live Rates Calculation Exactness for Settings Pane
    // ==========================================

    @Test
    fun salary_configuration_live_rates_maintain_scale16_precision() {
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("30000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8.0"),
            breakMinutes = 30,
            deductBreakFromSalary = true,
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.5"),
            isNightDifferentialEnabled = true,
            nightDifferentialRate = BigDecimal("0.25")
        )

        // dailyPaidHours = 8.0 - 0.5 = 7.5
        assertEquals(0, config.dailyPaidHours.compareTo(BigDecimal("7.5")))

        // hourlyRate = 30000 / (22 * 7.5) = 30000 / 165 = 181.818181...
        val hourly = config.hourlyRate
        val hourlyRounded = hourly.setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("181.82"), hourlyRounded)

        // minuteRate = hourly / 60 = 3.030303...
        val minute = config.minuteRate
        val minuteRounded = minute.setScale(4, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("3.0303"), minuteRounded)

        // overtimeHourlyRate = hourly * 1.5 = 272.72727...
        val overtimeHourly = config.overtimeHourlyRate
        val overtimeRounded = overtimeHourly.setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("272.73"), overtimeRounded)

        // nightDifferentialHourlyRate = hourly * 0.25 = 45.4545...
        val nightDiff = config.nightDifferentialHourlyRate
        val nightRounded = nightDiff.setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("45.45"), nightRounded)
    }
}
