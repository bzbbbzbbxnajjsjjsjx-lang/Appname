package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.BreakRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftEarnings
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaDashboardPhaseDTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryVardiyaRepository
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var viewModel: VardiyaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryVardiyaRepository()
        timeProvider = TestTimeProvider()
        calculator = ShiftEarningsCalculator()
        viewModel = VardiyaViewModel(
            repository = repository,
            calculator = calculator,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        viewModel.stopTicker()
        Dispatchers.resetMain()
    }

    @Test
    fun testStartBreakTransitionsUiStateToMolada() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)
        assertFalse(viewModel.uiState.value.isBreakActive)
        assertEquals("VARDİYA AKTİF", viewModel.uiState.value.stateBadgeText)

        viewModel.startBreak(isDeductedFromSalary = false, note = "Çay Molası")
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isBreakActive)
        assertEquals("MOLADA", state.stateBadgeText)
        assertEquals(1, state.currentShift?.activeBreaks?.size)

        val activeBreak = state.currentShift?.activeBreaks?.firstOrNull()
        assertNotNull(activeBreak)
        assertTrue(activeBreak!!.isOngoing)
        assertEquals("Çay Molası", activeBreak.note)
        assertFalse(activeBreak.isDeductedFromSalary)

        viewModel.stopTicker()
    }

    @Test
    fun testEndBreakRestoresRunningShift() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        timeProvider.advance(600_000L) // 10 min worked
        viewModel.startBreak(isDeductedFromSalary = false, note = "Yemek")
        runCurrent()
        assertTrue(viewModel.uiState.value.isBreakActive)

        timeProvider.advance(1800_000L) // 30 min break
        viewModel.endBreak()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isBreakActive)
        assertEquals("VARDİYA AKTİF", state.stateBadgeText)

        val finishedBreak = state.currentShift?.activeBreaks?.firstOrNull()
        assertNotNull(finishedBreak)
        assertFalse(finishedBreak!!.isOngoing)
        assertNotNull(finishedBreak.endEpochMillis)
        assertEquals(1800_000L, finishedBreak.getDurationMs())

        viewModel.stopTicker()
    }

    @Test
    fun testToggleBreakSwitchesState() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        assertFalse(viewModel.uiState.value.isBreakActive)

        // First toggle: starts break
        viewModel.toggleBreak()
        runCurrent()
        assertTrue(viewModel.uiState.value.isBreakActive)
        assertEquals("MOLADA", viewModel.uiState.value.stateBadgeText)

        // Second toggle: ends break
        viewModel.toggleBreak()
        runCurrent()
        assertFalse(viewModel.uiState.value.isBreakActive)
        assertEquals("VARDİYA AKTİF", viewModel.uiState.value.stateBadgeText)

        viewModel.stopTicker()
    }

    @Test
    fun testStartBreakIgnoredWhenNotRunningOrAlreadyOnBreak() = runTest(testDispatcher) {
        // When not started
        viewModel.startBreak()
        runCurrent()
        assertFalse(viewModel.uiState.value.isBreakActive)

        // Start shift and first break
        viewModel.startShift()
        runCurrent()
        viewModel.startBreak()
        runCurrent()
        assertEquals(1, viewModel.uiState.value.currentShift?.activeBreaks?.size)

        // Second break while ongoing should be rejected
        viewModel.startBreak()
        runCurrent()
        assertEquals(1, viewModel.uiState.value.currentShift?.activeBreaks?.size)

        viewModel.stopTicker()
    }

    @Test
    fun testFinishShiftAutoClosesOngoingBreak() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        timeProvider.advance(1_000_000L)
        viewModel.startBreak()
        runCurrent()
        assertTrue(viewModel.uiState.value.isBreakActive)

        timeProvider.advance(500_000L)
        viewModel.finishShift()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(ShiftState.FINISHED, state.shiftState)
        assertFalse(state.isBreakActive)
        assertEquals("VARDİYA TAMAMLANDI", state.stateBadgeText)

        val historyRecord = state.history.firstOrNull()
        assertNotNull(historyRecord)
        assertEquals(1, historyRecord!!.breaks.size)

        val sealedBreak = historyRecord.breaks.first()
        assertFalse("Finished shift break must not remain ongoing", sealedBreak.isOngoing)
        assertNotNull(sealedBreak.endEpochMillis)

        viewModel.stopTicker()
    }

    @Test
    fun testUiStateActiveBreakFormatting() {
        val ongoingBreak = BreakRecord(
            id = "test-break",
            startEpochMillis = 1000L,
            endEpochMillis = null
        )
        val shiftWithBreak = Shift(
            activeBreaks = listOf(ongoingBreak)
        )
        val state = VardiyaUiState(
            shiftState = ShiftState.RUNNING,
            currentShift = shiftWithBreak
        )

        assertTrue(state.isBreakActive)
    }

    @Test
    fun testOvertimeBadgeAndStateMapping() {
        val normalState = VardiyaUiState(
            shiftState = ShiftState.RUNNING,
            earnings = ShiftEarnings(overtimeDurationMs = 0L)
        )
        assertEquals("VARDİYA AKTİF", normalState.stateBadgeText)

        val overtimeState = VardiyaUiState(
            shiftState = ShiftState.RUNNING,
            earnings = ShiftEarnings(overtimeDurationMs = 1800_000L)
        )
        assertEquals("FAZLA MESAİDE", overtimeState.stateBadgeText)

        // Break takes priority when on break during overtime
        val breakAndOvertimeState = VardiyaUiState(
            shiftState = ShiftState.RUNNING,
            currentShift = Shift(activeBreaks = listOf(BreakRecord(startEpochMillis = 100L))),
            earnings = ShiftEarnings(overtimeDurationMs = 1800_000L)
        )
        assertEquals("MOLADA", breakAndOvertimeState.stateBadgeText)
    }

    @Test
    fun testProgressBehavior() {
        val config = SalaryConfiguration(dailyWorkHours = BigDecimal("8"))

        val notStarted = VardiyaUiState(shiftState = ShiftState.NOT_STARTED, salaryConfig = config)
        assertEquals(0f, notStarted.progress, 0.001f)

        val finished = VardiyaUiState(shiftState = ShiftState.FINISHED, salaryConfig = config)
        assertEquals(1f, finished.progress, 0.001f)

        // 4 hours out of 8 hours target -> 0.5f progress
        val halfShift = VardiyaUiState(
            shiftState = ShiftState.RUNNING,
            salaryConfig = config,
            earnings = ShiftEarnings(activeDurationMs = 4 * 3600 * 1000L)
        )
        assertEquals(0.5f, halfShift.progress, 0.001f)
    }
}
