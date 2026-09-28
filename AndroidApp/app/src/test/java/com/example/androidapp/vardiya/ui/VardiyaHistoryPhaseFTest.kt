package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.BreakRecord
import com.example.androidapp.vardiya.domain.model.CalendarDate
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.ShiftHistoryFilterEngine
import com.example.androidapp.vardiya.domain.model.ShiftHistoryFilterType
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaHistoryPhaseFTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: InMemoryVardiyaRepository
    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var viewModel: VardiyaViewModel

    private class FakeTimeProvider(
        var epochMillis: Long = 1759050000000L, // Fixed reference epoch
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
        templateId: String? = null,
        breaks: List<BreakRecord> = emptyList()
    ): CompletedShiftRecord {
        val startEpoch = makeEpochForDate(year, month1Indexed, day, 9)
        val activeMs = durationHours * 3_600_000L
        val finishEpoch = startEpoch + activeMs

        return CompletedShiftRecord(
            id = id,
            dateFormatted = "$day Eylül $year",
            timeRangeFormatted = "09:00 — 18:00",
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
    fun filterByTextQuery_matchesNotesCaseInsensitive() {
        val r1 = createRecord("1", 2026, 9, 10, 8, BigDecimal("1200"), note = "Ağır sevkiyat günü")
        val r2 = createRecord("2", 2026, 9, 11, 8, BigDecimal("1200"), note = "Normal ofis mesaisi")
        val list = listOf(r1, r2)

        val filtered = ShiftHistoryFilterEngine.filter(list, query = "sevkiyat")
        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)

        val upperFiltered = ShiftHistoryFilterEngine.filter(list, query = "SEVKİYAT")
        assertEquals(1, upperFiltered.size)
        assertEquals("1", upperFiltered[0].id)

        val emptyResult = ShiftHistoryFilterEngine.filter(list, query = "bulunmayan kelime")
        assertTrue(emptyResult.isEmpty())
    }

    @Test
    fun filterByTextQuery_matchesDateFormattedAndTemplate() {
        val r1 = createRecord("1", 2026, 9, 10, 8, BigDecimal("1200"), templateId = "sabah_vardiyasi")
        val r2 = createRecord("2", 2026, 9, 15, 8, BigDecimal("1200"), templateId = "gece_vardiyasi")
        val list = listOf(r1, r2)

        val matchedTemplate = ShiftHistoryFilterEngine.filter(list, query = "sabah")
        assertEquals(1, matchedTemplate.size)
        assertEquals("1", matchedTemplate[0].id)

        val matchedDate = ShiftHistoryFilterEngine.filter(list, query = "15 Eylül")
        assertEquals(1, matchedDate.size)
        assertEquals("2", matchedDate[0].id)
    }

    @Test
    fun filterByType_overtimeAndNightShift() {
        val normal = createRecord("norm", 2026, 9, 1, 8, BigDecimal("1200"))
        val overtime = createRecord("ot", 2026, 9, 2, 10, BigDecimal("1600"), overtimeEarned = BigDecimal("400"))
        val night = createRecord("night", 2026, 9, 3, 8, BigDecimal("1400"), nightDifferentialEarned = BigDecimal("200"))
        val both = createRecord("both", 2026, 9, 4, 11, BigDecimal("1900"), overtimeEarned = BigDecimal("400"), nightDifferentialEarned = BigDecimal("300"))
        val list = listOf(normal, overtime, night, both)

        val otOnly = ShiftHistoryFilterEngine.filter(list, filterType = ShiftHistoryFilterType.OVERTIME)
        assertEquals(2, otOnly.size)
        assertTrue(otOnly.any { it.id == "ot" })
        assertTrue(otOnly.any { it.id == "both" })

        val nightOnly = ShiftHistoryFilterEngine.filter(list, filterType = ShiftHistoryFilterType.NIGHT_SHIFT)
        assertEquals(2, nightOnly.size)
        assertTrue(nightOnly.any { it.id == "night" })
        assertTrue(nightOnly.any { it.id == "both" })
    }

    @Test
    fun filterBySelectedDate_filtersExactDay() {
        val r1 = createRecord("1", 2026, 9, 10, 8, BigDecimal("1200"))
        val r2 = createRecord("2", 2026, 9, 11, 8, BigDecimal("1200"))
        val r3 = createRecord("3", 2026, 9, 10, 4, BigDecimal("600"))
        val list = listOf(r1, r2, r3)

        val filteredDay10 = ShiftHistoryFilterEngine.filter(
            list,
            selectedDate = CalendarDate(2026, 9, 10)
        )
        assertEquals(2, filteredDay10.size)
        assertTrue(filteredDay10.any { it.id == "1" })
        assertTrue(filteredDay10.any { it.id == "3" })

        val filteredDay11 = ShiftHistoryFilterEngine.filter(
            list,
            selectedDate = CalendarDate(2026, 9, 11)
        )
        assertEquals(1, filteredDay11.size)
        assertEquals("2", filteredDay11[0].id)
    }

    @Test
    fun summarize_calculatesAggregateTotals() {
        val r1 = createRecord("1", 2026, 9, 1, 8, BigDecimal("1000"), overtimeEarned = BigDecimal("100"), nightDifferentialEarned = BigDecimal("50"))
        val r2 = createRecord("2", 2026, 9, 2, 4, BigDecimal("500"))
        val list = listOf(r1, r2)

        val summary = ShiftHistoryFilterEngine.summarize(list)
        assertEquals(2, summary.totalCount)
        assertEquals(12 * 3_600_000L, summary.totalDurationMs)
        assertEquals(BigDecimal("1500"), summary.totalEarned)
        assertEquals(BigDecimal("100"), summary.totalOvertimeEarned)
        assertEquals(BigDecimal("50"), summary.totalNightDifferentialEarned)
        assertEquals("12s 0dk", summary.formattedTotalDuration)
    }

    @Test
    fun aggregateMonth_calculatesHeatIntensityAccurately() {
        // Day 5: 3 hours -> light (< 5h) -> intensity 1
        val lightShift = createRecord("1", 2026, 9, 5, 3, BigDecimal("450"))
        // Day 10: 8 hours -> standard (5-8.5h) -> intensity 2
        val standardShift = createRecord("2", 2026, 9, 10, 8, BigDecimal("1200"))
        // Day 15: 10 hours with overtime -> high -> intensity 3
        val overtimeShift = createRecord("3", 2026, 9, 15, 10, BigDecimal("1600"), overtimeEarned = BigDecimal("400"))
        val list = listOf(lightShift, standardShift, overtimeShift)

        val monthMap = ShiftHistoryFilterEngine.aggregateMonth(list, 2026, 9)

        val day5 = monthMap[5]
        assertNotNull(day5)
        assertEquals(1, day5!!.heatIntensity)
        assertEquals(1, day5.shiftCount)

        val day10 = monthMap[10]
        assertNotNull(day10)
        assertEquals(2, day10!!.heatIntensity)

        val day15 = monthMap[15]
        assertNotNull(day15)
        assertEquals(3, day15!!.heatIntensity)
        assertTrue(day15.hasOvertime)

        // Day 20 not in map
        assertNull(monthMap[20])
    }

    @Test
    fun viewModelHistorySelection_updatesStateCorrectly() = runTest(testDispatcher) {
        val record = createRecord("rec-101", 2026, 9, 15, 8, BigDecimal("1500"))

        assertNull(viewModel.uiState.value.selectedHistoryRecord)

        viewModel.selectHistoryRecord(record)
        assertEquals(record, viewModel.uiState.value.selectedHistoryRecord)

        viewModel.selectHistoryRecord(null)
        assertNull(viewModel.uiState.value.selectedHistoryRecord)
    }

    @Test
    fun scalabilityCriterion_supports50PlusRecordsInstantly() {
        val largeHistory = (1..60).map { i ->
            val day = (i % 28) + 1
            createRecord(
                id = "shift-$i",
                year = 2026,
                month1Indexed = 9,
                day = day,
                durationHours = (4 + (i % 6)).toLong(),
                totalEarned = BigDecimal(500 + i * 50),
                overtimeEarned = if (i % 3 == 0) BigDecimal("200") else BigDecimal.ZERO,
                note = "Vardiya no $i"
            )
        }

        val startTime = System.currentTimeMillis()
        val filtered = ShiftHistoryFilterEngine.filter(
            largeHistory,
            query = "Vardiya",
            filterType = ShiftHistoryFilterType.OVERTIME
        )
        val monthAgg = ShiftHistoryFilterEngine.aggregateMonth(largeHistory, 2026, 9)
        val summary = ShiftHistoryFilterEngine.summarize(filtered)
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(20, filtered.size)
        assertTrue("Processing 60+ records should complete in under 100ms", elapsed < 100)
        assertEquals(28, monthAgg.size) // Days 1..28 all covered
        assertTrue(summary.totalCount == 20)
    }
}
