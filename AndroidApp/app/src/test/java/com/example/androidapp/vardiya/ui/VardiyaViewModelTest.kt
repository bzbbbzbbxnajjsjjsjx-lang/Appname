package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class TestTimeProvider(
    var epochMillis: Long = 1700000000000L,
    var elapsedRealtime: Long = 1000000L
) : TimeProvider {
    override fun currentEpochMillis(): Long = epochMillis
    override fun elapsedRealtimeMillis(): Long = elapsedRealtime

    fun advance(durationMs: Long) {
        epochMillis += durationMs
        elapsedRealtime += durationMs
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaViewModelTest {

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
    fun testInitialStateIsNotStarted() {
        val state = viewModel.uiState.value
        assertEquals(ShiftState.NOT_STARTED, state.shiftState)
        assertEquals("VARDİYAYA HAZIR", state.stateBadgeText)
        assertEquals("₺28.000", state.heroAmountText)
        assertEquals("AYLIK MAAŞ", state.heroSubtitleText)
        assertNull(state.currentShift)
    }

    @Test
    fun testStartShiftTransitionsToRunning() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(ShiftState.RUNNING, state.shiftState)
        assertEquals("VARDİYA AKTİF", state.stateBadgeText)
        assertNotNull(state.currentShift)
        assertEquals(ShiftState.RUNNING, repository.getActiveShift()?.state)
        viewModel.stopTicker()
    }

    @Test
    fun testPauseShiftFreezesDurationAndEarnings() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        // 10 minutes pass
        timeProvider.advance(10 * 60 * 1000L)
        advanceTimeBy(10 * 60 * 1000L)

        viewModel.pauseShift()
        runCurrent()

        val pausedState = viewModel.uiState.value
        assertEquals(ShiftState.PAUSED, pausedState.shiftState)
        assertEquals("VARDİYA DURAKLATILDI", pausedState.stateBadgeText)

        val frozenDuration = pausedState.earnings.activeDurationMs
        assertEquals(10 * 60 * 1000L, frozenDuration)

        // 30 minutes pass while paused
        timeProvider.advance(30 * 60 * 1000L)
        advanceTimeBy(30 * 60 * 1000L)
        runCurrent()

        // State remains frozen
        assertEquals(frozenDuration, viewModel.uiState.value.earnings.activeDurationMs)
    }

    @Test
    fun testResumeShiftContinuesAccumulation() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        // 10 mins active
        timeProvider.advance(10 * 60 * 1000L)
        advanceTimeBy(10 * 60 * 1000L)

        viewModel.pauseShift()
        runCurrent()

        // 20 mins paused
        timeProvider.advance(20 * 60 * 1000L)
        advanceTimeBy(20 * 60 * 1000L)

        viewModel.resumeShift()
        runCurrent()
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)

        // 5 mins active after resume
        timeProvider.advance(5 * 60 * 1000L)
        advanceTimeBy(5 * 60 * 1000L)

        // Total active duration should be 15 mins (900,000 ms), not 35 mins
        val activeMs = calculator.calculateActiveDurationMs(
            viewModel.uiState.value.currentShift!!,
            timeProvider.elapsedRealtimeMillis(),
            timeProvider.currentEpochMillis()
        )
        assertEquals(15 * 60 * 1000L, activeMs)
        viewModel.stopTicker()
    }

    @Test
    fun testFinishShiftRecordsHistoryAndFreezes() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        // 2 hours pass
        timeProvider.advance(2 * 3600 * 1000L)
        advanceTimeBy(2 * 3600 * 1000L)

        viewModel.finishShift()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(ShiftState.FINISHED, state.shiftState)
        assertEquals("VARDİYA TAMAMLANDI", state.stateBadgeText)
        assertEquals(1, state.history.size)

        val historyItem = state.history.first()
        assertTrue(historyItem.durationFormatted.contains("2s"))
        assertNotNull(state.currentShift?.totalEarnedWhenFinished)
    }

    @Test
    fun testResetShiftReturnsToReady() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()
        viewModel.finishShift()
        runCurrent()
        viewModel.resetShift()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(ShiftState.NOT_STARTED, state.shiftState)
        assertNull(state.currentShift)
        assertNull(repository.getActiveShift())
    }

    @Test
    fun testInvalidTransitionsAreRejected() = runTest(testDispatcher) {
        // NOT_STARTED -> PAUSE should fail
        viewModel.pauseShift()
        runCurrent()
        assertEquals(ShiftState.NOT_STARTED, viewModel.uiState.value.shiftState)

        // Start shift
        viewModel.startShift()
        runCurrent()
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)

        // RUNNING -> START again should be ignored
        viewModel.startShift()
        runCurrent()
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)

        // Finish shift
        viewModel.finishShift()
        runCurrent()
        assertEquals(ShiftState.FINISHED, viewModel.uiState.value.shiftState)

        // FINISHED -> RUNNING directly without reset should be rejected
        viewModel.startShift()
        runCurrent()
        assertEquals(ShiftState.FINISHED, viewModel.uiState.value.shiftState)
    }

    @Test
    fun testAppRestartSimulationRestoresRunningShift() = runTest(testDispatcher) {
        // Start shift in one session
        viewModel.startShift()
        runCurrent()
        timeProvider.advance(45 * 60 * 1000L) // 45 minutes active

        // Simulate app closing and new ViewModel instance opening
        val restoredViewModel = VardiyaViewModel(
            repository = repository,
            calculator = calculator,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )

        val restoredState = restoredViewModel.uiState.value
        assertEquals(ShiftState.RUNNING, restoredState.shiftState)
        assertEquals(45 * 60 * 1000L, restoredState.earnings.activeDurationMs)

        // 45 min earnings on 159.09 TL/hr = ~119.32 TL
        val expectedEarned = repository.getSalaryConfiguration().hourlyRate
            .multiply(BigDecimal("0.75"))
            .setScale(2, RoundingMode.HALF_UP)
        assertEquals(expectedEarned, restoredState.earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))

        restoredViewModel.stopTicker()
        viewModel.stopTicker()
    }

    @Test
    fun testUpdateSalaryConfigurationUpdatesBaseline() = runTest(testDispatcher) {
        val newConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("44000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8")
        )

        viewModel.updateSalaryConfig(newConfig)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(BigDecimal("44000"), state.salaryConfig.monthlySalary)
        assertEquals("₺44.000", state.heroAmountText)

        // Hourly rate: 44,000 / 176 = 250 TL
        val hourly = state.earnings.hourlyRate.setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("250.00"), hourly)
    }

    @Test
    fun testHistoryPreservesSalaryConfigSnapshotAfterConfigChange() = runTest(testDispatcher) {
        // Complete shift under 28,000 TL config
        viewModel.startShift()
        runCurrent()
        timeProvider.advance(3600 * 1000L) // 1 hour
        viewModel.finishShift()
        runCurrent()

        val oldHistoryEarned = viewModel.uiState.value.history.first().totalEarned.setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("159.09"), oldHistoryEarned)

        // Update salary config to 50,000 TL
        viewModel.resetShift()
        runCurrent()
        viewModel.updateSalaryConfig(
            SalaryConfiguration(monthlySalary = BigDecimal("50000"))
        )
        runCurrent()

        // Old history record must NOT be mutated!
        val historyRecord = viewModel.uiState.value.history.first()
        assertEquals(BigDecimal("159.09"), historyRecord.totalEarned.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("28000"), historyRecord.salaryConfigSnapshot.monthlySalary)
    }
}
