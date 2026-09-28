package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
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
import java.math.RoundingMode

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaSettingsPhaseHTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryVardiyaRepository
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var calculator: ShiftEarningsCalculator
    private lateinit var viewModel: VardiyaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryVardiyaRepository()
        timeProvider = TestTimeProvider(epochMillis = 1700000000000L, elapsedRealtime = 1000000L)
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
    fun testInitialStateLoadsAdvancedWorkConfigurationAndTheme() = runTest(testDispatcher) {
        val config = viewModel.uiState.value.salaryConfig
        assertEquals(BigDecimal("28000"), config.monthlySalary)
        assertEquals(22, config.monthlyWorkDays)
        assertEquals(BigDecimal("8"), config.dailyWorkHours)
        assertEquals(BigDecimal("1.50"), config.overtimeMultiplier)
        assertFalse(config.isOvertimeEnabled)
        assertEquals(BigDecimal("0.15"), config.nightDifferentialRate)
        assertFalse(config.isNightDifferentialEnabled)
        assertEquals(20, config.nightShiftStartHour)
        assertEquals(6, config.nightShiftEndHour)
        assertTrue(viewModel.uiState.value.isDynamicColorEnabled)
    }

    @Test
    fun testUpdateSalaryConfigPropagatesToActiveShiftAndRecalculatesEarnings() = runTest(testDispatcher) {
        // Start a shift
        viewModel.startShift()
        runCurrent()
        viewModel.stopTicker() // Stop infinite background ticker in test environment
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)

        // Advance 9 hours (32,400,000 ms) with initial 8h workday (no overtime enabled)
        timeProvider.advance(9 * 3600 * 1000L)
        runCurrent()

        // In 9 hours with 28,000 monthly, 22 days, 8h (rate: 28000 / 176 = 159.0909.../hr):
        val initialEarned = viewModel.uiState.value.earnings.earnedAmount
        assertEquals(BigDecimal.ZERO, viewModel.uiState.value.earnings.overtimeEarned)

        // Now enable overtime with 2.0x multiplier and higher base salary
        val updatedConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("35200"), // 35200 / (22 * 8) = 200/hr
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("2.00")
        )
        viewModel.updateSalaryConfig(updatedConfig)
        runCurrent()

        // Active shift should have updated configuration
        val activeShift = viewModel.uiState.value.currentShift
        assertNotNull(activeShift)
        assertEquals(updatedConfig, activeShift?.salaryConfig)
        assertEquals(updatedConfig, viewModel.uiState.value.salaryConfig)

        // In 9 hours with 8h regular and 1h overtime @ 2.0x:
        // Regular: 8 * 200 = 1600
        // Overtime: 1 * 400 = 400
        // Total: 2000
        val earnings = viewModel.uiState.value.earnings
        assertEquals(BigDecimal("1600.00"), earnings.baseEarned.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("400.00"), earnings.overtimeEarned.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("2000.00"), earnings.earnedAmount.setScale(2, RoundingMode.HALF_UP))
        assertTrue(earnings.earnedAmount > initialEarned)
    }

    @Test
    fun testUpdateSalaryConfigPreservesHistoricalImmutability() = runTest(testDispatcher) {
        // Complete a shift under baseline config (28,000 monthly)
        viewModel.startShift()
        runCurrent()
        viewModel.stopTicker()

        timeProvider.advance(4 * 3600 * 1000L) // 4 hours
        runCurrent()

        viewModel.finishShift()
        runCurrent()

        assertEquals(1, viewModel.uiState.value.history.size)
        val historicalRecord = viewModel.uiState.value.history.first()
        val originalSnapshot = historicalRecord.salaryConfigSnapshot
        assertEquals(BigDecimal("28000"), originalSnapshot.monthlySalary)

        // Now update salary configuration to a completely different salary & overtime
        val newConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("70000"),
            monthlyWorkDays = 20,
            dailyWorkHours = BigDecimal("7"),
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.75")
        )
        viewModel.updateSalaryConfig(newConfig)
        runCurrent()

        // Verify current config changed
        assertEquals(BigDecimal("70000"), viewModel.uiState.value.salaryConfig.monthlySalary)

        // Verify historical record is 100% frozen and untouched
        val postUpdateHistory = viewModel.uiState.value.history
        assertEquals(1, postUpdateHistory.size)
        assertEquals(BigDecimal("28000"), postUpdateHistory.first().salaryConfigSnapshot.monthlySalary)
        assertEquals(BigDecimal("1.50"), postUpdateHistory.first().salaryConfigSnapshot.overtimeMultiplier)
        assertFalse(postUpdateHistory.first().salaryConfigSnapshot.isOvertimeEnabled)
    }

    @Test
    fun testDynamicColorPreferenceToggleAndPersistence() = runTest(testDispatcher) {
        // Initially enabled
        assertTrue(viewModel.uiState.value.isDynamicColorEnabled)
        assertTrue(repository.isDynamicColorEnabled())

        // Toggle to false
        viewModel.setDynamicColorEnabled(false)
        runCurrent()

        assertFalse(viewModel.uiState.value.isDynamicColorEnabled)
        assertFalse(repository.isDynamicColorEnabled())

        // Toggle back to true
        viewModel.setDynamicColorEnabled(true)
        runCurrent()

        assertTrue(viewModel.uiState.value.isDynamicColorEnabled)
        assertTrue(repository.isDynamicColorEnabled())
    }

    @Test
    fun testOvertimeAndNightDifferentialConfigurationCombinations() = runTest(testDispatcher) {
        val config = SalaryConfiguration(
            monthlySalary = BigDecimal("35200"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"), // hourlyRate = 200
            isOvertimeEnabled = true,
            overtimeMultiplier = BigDecimal("1.50"), // overtime hourly = 300
            isNightDifferentialEnabled = true,
            nightDifferentialRate = BigDecimal("0.20"), // night diff = 40
            nightShiftStartHour = 22,
            nightShiftEndHour = 6
        )

        viewModel.updateSalaryConfig(config)
        runCurrent()

        val activeConfig = viewModel.uiState.value.salaryConfig
        assertEquals(BigDecimal("200.00"), activeConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("300.00"), activeConfig.overtimeHourlyRate.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("40.00"), activeConfig.nightDifferentialHourlyRate.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testBreakDeductionToggleAffectsDailyPaidHours() = runTest(testDispatcher) {
        // Without break deduction (8h paid)
        val configWithoutDeduction = SalaryConfiguration(
            monthlySalary = BigDecimal("22000"),
            monthlyWorkDays = 22,
            dailyWorkHours = BigDecimal("8"),
            breakMinutes = 60,
            deductBreakFromSalary = false
        )
        viewModel.updateSalaryConfig(configWithoutDeduction)
        runCurrent()

        assertEquals(BigDecimal("8.00"), viewModel.uiState.value.salaryConfig.dailyPaidHours.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("125.00"), viewModel.uiState.value.salaryConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))

        // With break deduction (7h paid: 8h - 60min)
        val configWithDeduction = configWithoutDeduction.copy(deductBreakFromSalary = true)
        viewModel.updateSalaryConfig(configWithDeduction)
        runCurrent()

        assertEquals(BigDecimal("7.00"), viewModel.uiState.value.salaryConfig.dailyPaidHours.setScale(2, RoundingMode.HALF_UP))
        // 22000 / (22 * 7) = 22000 / 154 = 142.86
        assertEquals(BigDecimal("142.86"), viewModel.uiState.value.salaryConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))
    }
}
