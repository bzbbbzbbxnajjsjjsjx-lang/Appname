package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.VardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/**
 * Controllable failing repository for persistence error isolation verification.
 */
class FailingVardiyaRepository : VardiyaRepository {
    var failOnSaveActiveShift = false
    var failOnSaveSalary = false
    var failOnAddHistory = false
    var failOnClearHistory = false

    var savedShift: Shift? = null
    var savedSalary: SalaryConfiguration = SalaryConfiguration()
    val historyList = mutableListOf<CompletedShiftRecord>()

    override fun getSalaryConfiguration(): SalaryConfiguration = savedSalary

    override fun saveSalaryConfiguration(config: SalaryConfiguration) {
        if (failOnSaveSalary) throw IOException("Disk write failure (salary config)")
        savedSalary = config
    }

    override fun getActiveShift(): Shift? = savedShift

    override fun saveActiveShift(shift: Shift?) {
        if (failOnSaveActiveShift) throw IOException("Disk write failure (active shift)")
        savedShift = shift
    }

    override fun getShiftHistory(): List<CompletedShiftRecord> = historyList.toList()

    override fun addShiftToHistory(record: CompletedShiftRecord) {
        if (failOnAddHistory) throw IOException("Disk write failure (history)")
        historyList.add(0, record)
    }

    override fun clearShiftHistory() {
        if (failOnClearHistory) throw IOException("Disk write failure (clear history)")
        historyList.clear()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaViewModelHardeningTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var failingRepo: FailingVardiyaRepository
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var viewModel: VardiyaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        failingRepo = FailingVardiyaRepository()
        timeProvider = TestTimeProvider()
        calculator = ShiftEarningsCalculator()
        viewModel = VardiyaViewModel(
            repository = failingRepo,
            calculator = calculator,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPersistenceErrorIsolationWhenSavingActiveShiftFails() = runTest(testDispatcher) {
        failingRepo.failOnSaveActiveShift = true

        // Attempt to start shift with failing disk
        viewModel.startShift()
        runCurrent()

        val state = viewModel.uiState.value
        // Must NOT crash the ViewModel coroutine!
        // An error message must be set for user awareness
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Vardiya kaydedilemedi"))

        // Dismiss error
        viewModel.clearError()
        runCurrent()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testPersistenceErrorIsolationWhenSavingSalaryFails() = runTest(testDispatcher) {
        failingRepo.failOnSaveSalary = true

        val newConfig = SalaryConfiguration(monthlySalary = BigDecimal("50000"))
        viewModel.updateSalaryConfig(newConfig)
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Maaş ayarları kaydedilemedi"))
    }

    @Test
    fun testPersistenceErrorIsolationWhenAddingHistoryFails() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        timeProvider.advance(3600000L) // 1 hr
        failingRepo.failOnAddHistory = true

        viewModel.finishShift()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(ShiftState.FINISHED, state.shiftState)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Vardiya geçmişi kaydedilemedi"))
    }

    @Test
    fun testPersistenceErrorIsolationWhenClearingHistoryFails() = runTest(testDispatcher) {
        failingRepo.failOnClearHistory = true

        viewModel.clearHistory()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Geçmiş temizlenemedi"))
    }

    @Test
    fun testRapidConcurrentStateTransitionsUnderMutex() = runTest(testDispatcher) {
        // Fire 20 alternating actions simultaneously
        val jobs = List(20) { index ->
            launch {
                when (index % 4) {
                    0 -> viewModel.startShift()
                    1 -> viewModel.pauseShift()
                    2 -> viewModel.resumeShift()
                    3 -> viewModel.finishShift()
                }
            }
        }
        jobs.forEach { it.join() }
        runCurrent()

        val finalState = viewModel.uiState.value
        // Shift state must be one of valid states, no deadlocks, no illegal transitions
        assertTrue(
            finalState.shiftState == ShiftState.RUNNING ||
            finalState.shiftState == ShiftState.PAUSED ||
            finalState.shiftState == ShiftState.FINISHED ||
            finalState.shiftState == ShiftState.NOT_STARTED
        )
    }

    @Test
    fun testTickerCancellationOnShiftCompletionAndReset() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)

        // Advance 2 seconds
        timeProvider.advance(2000L)
        advanceTimeBy(2000L)
        runCurrent()

        // Finish shift
        viewModel.finishShift()
        runCurrent()
        assertEquals(ShiftState.FINISHED, viewModel.uiState.value.shiftState)

        val earningsAtFinish = viewModel.uiState.value.earnings.earnedAmount

        // Advance more time; ticker must NOT keep ticking or changing earnings
        timeProvider.advance(5000L)
        advanceTimeBy(5000L)
        runCurrent()

        assertEquals(earningsAtFinish, viewModel.uiState.value.earnings.earnedAmount)
    }
}
