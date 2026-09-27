package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaConcurrencyTest {

    @Test
    fun testConcurrentViewModelActionsMaintainStateIntegrity() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = InMemoryVardiyaRepository()
        val timeProvider = TestTimeProvider()
        val calculator = ShiftEarningsCalculator()
        val viewModel = VardiyaViewModel(
            repository = repository,
            calculator = calculator,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )

        // Launch 50 concurrent tasks attempting random lifecycle and config operations
        val jobs = (1..50).map { index ->
            launch(Dispatchers.Default) {
                when (index % 6) {
                    0 -> viewModel.startShift()
                    1 -> viewModel.pauseShift()
                    2 -> viewModel.resumeShift()
                    3 -> viewModel.finishShift()
                    4 -> viewModel.updateSalaryConfig(
                        SalaryConfiguration(monthlySalary = BigDecimal((30000 + index * 100).toString()))
                    )
                    5 -> {
                        viewModel.openHistory()
                        viewModel.closeHistory()
                    }
                }
            }
        }

        jobs.joinAll()
        testDispatcher.scheduler.runCurrent()

        val finalState = viewModel.uiState.value
        // Invariant: The shiftState must be one of the 4 valid enum states, never null or corrupted
        assertTrue(
            finalState.shiftState in listOf(
                ShiftState.NOT_STARTED,
                ShiftState.RUNNING,
                ShiftState.PAUSED,
                ShiftState.FINISHED
            )
        )
        // Invariant: Salary configuration is non-null and valid
        assertTrue(finalState.salaryConfig.monthlySalary > BigDecimal.ZERO)
        // Invariant: Earnings amount is non-null and >= 0
        assertNotNull(finalState.earnings)
        assertTrue(finalState.earnings.earnedAmount >= BigDecimal.ZERO)

        viewModel.stopTicker()
    }
}
