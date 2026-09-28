package com.example.androidapp.vardiya.ui

import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.model.ShiftTemplate
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class VardiyaTemplateAndBreakPhaseETest {

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
    fun testApplyShiftTemplateUpdatesSalaryConfigAndState() = runTest(testDispatcher) {
        val morningTemplate = ShiftTemplate.PRESETS[0] // 8 hours, 60m break, deduct = false

        viewModel.applyShiftTemplate(morningTemplate)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(morningTemplate.id, state.selectedTemplateId)
        assertEquals(morningTemplate, state.selectedTemplate)
        assertEquals(morningTemplate.durationHours, state.salaryConfig.dailyWorkHours)
        assertEquals(morningTemplate.breakMinutes, state.salaryConfig.breakMinutes)
        assertEquals(morningTemplate.deductBreakFromSalary, state.salaryConfig.deductBreakFromSalary)
        assertFalse(state.isTemplatePickerVisible)

        // Verify persisted repository config matches
        val persistedConfig = repository.getSalaryConfiguration()
        assertEquals(morningTemplate.durationHours, persistedConfig.dailyWorkHours)
        assertEquals(morningTemplate.breakMinutes, persistedConfig.breakMinutes)
    }

    @Test
    fun testApplyTemplateWhileShiftIsActiveUpdatesShiftAndEarnings() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        timeProvider.advance(3600_000L) // 1 hr worked
        val eveningTemplate = ShiftTemplate.PRESETS[1]

        viewModel.applyShiftTemplate(eveningTemplate)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(eveningTemplate.id, state.currentShift?.templateId)
        assertEquals(eveningTemplate.durationHours, state.currentShift?.salaryConfig?.dailyWorkHours)
        assertNotNull(state.earnings)

        viewModel.stopTicker()
    }

    @Test
    fun testClearSelectedTemplateRemovesTemplateLink() = runTest(testDispatcher) {
        viewModel.applyShiftTemplate(ShiftTemplate.PRESETS[2])
        runCurrent()
        assertNotNull(viewModel.uiState.value.selectedTemplate)

        viewModel.clearSelectedTemplate()
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.selectedTemplateId)
        assertNull(state.selectedTemplate)
        assertFalse(state.isTemplatePickerVisible)
    }

    @Test
    fun testStartShiftCarriesSelectedTemplateId() = runTest(testDispatcher) {
        val nightTemplate = ShiftTemplate.PRESETS[2]
        viewModel.applyShiftTemplate(nightTemplate)
        runCurrent()

        viewModel.startShift()
        runCurrent()

        assertEquals(nightTemplate.id, viewModel.uiState.value.currentShift?.templateId)

        timeProvider.advance(2 * 3600_000L)
        viewModel.finishShift()
        runCurrent()

        val historyRecord = viewModel.uiState.value.history.firstOrNull()
        assertNotNull(historyRecord)
        assertEquals(nightTemplate.id, historyRecord?.templateId)

        viewModel.stopTicker()
    }

    @Test
    fun testOpenAndCloseTemplatePickerAndBreakSheet() {
        assertFalse(viewModel.uiState.value.isTemplatePickerVisible)
        viewModel.openTemplatePicker()
        assertTrue(viewModel.uiState.value.isTemplatePickerVisible)
        viewModel.closeTemplatePicker()
        assertFalse(viewModel.uiState.value.isTemplatePickerVisible)

        assertFalse(viewModel.uiState.value.isBreakSheetVisible)
        viewModel.openBreakSheet()
        assertTrue(viewModel.uiState.value.isBreakSheetVisible)
        viewModel.closeBreakSheet()
        assertFalse(viewModel.uiState.value.isBreakSheetVisible)
    }

    @Test
    fun testBreakManagementMultipleBreaksTracking() = runTest(testDispatcher) {
        viewModel.startShift()
        runCurrent()

        // 1st break: 15 min paid coffee
        viewModel.startBreak(isDeductedFromSalary = false, note = "Kahve")
        runCurrent()
        assertTrue(viewModel.uiState.value.isBreakActive)
        assertEquals("Kahve", viewModel.uiState.value.ongoingBreak?.note)

        timeProvider.advance(900_000L)
        viewModel.endBreak()
        runCurrent()
        assertFalse(viewModel.uiState.value.isBreakActive)
        assertNull(viewModel.uiState.value.ongoingBreak)

        // 2nd break: 45 min deducted lunch
        timeProvider.advance(3600_000L)
        viewModel.startBreak(isDeductedFromSalary = true, note = "Öğle Yemeği")
        runCurrent()
        assertTrue(viewModel.uiState.value.isBreakActive)

        timeProvider.advance(2700_000L)
        viewModel.endBreak()
        runCurrent()

        val breaks = viewModel.uiState.value.currentShiftBreaks
        assertEquals(2, breaks.size)
        assertEquals("Kahve", breaks[0].note)
        assertFalse(breaks[0].isDeductedFromSalary)
        assertEquals(900_000L, breaks[0].getDurationMs())

        assertEquals("Öğle Yemeği", breaks[1].note)
        assertTrue(breaks[1].isDeductedFromSalary)
        assertEquals(2700_000L, breaks[1].getDurationMs())

        viewModel.stopTicker()
    }

    @Test
    fun testRestoredActiveShiftRestoresTemplateId() = runTest(testDispatcher) {
        val template = ShiftTemplate.PRESETS[0]
        viewModel.applyShiftTemplate(template)
        runCurrent()
        viewModel.startShift()
        runCurrent()
        timeProvider.advance(1800_000L)

        // Simulate app recreate with fresh ViewModel instance
        val newViewModel = VardiyaViewModel(
            repository = repository,
            calculator = calculator,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )

        assertEquals(ShiftState.RUNNING, newViewModel.uiState.value.shiftState)
        assertEquals(template.id, newViewModel.uiState.value.selectedTemplateId)
        assertEquals(template.id, newViewModel.uiState.value.currentShift?.templateId)

        newViewModel.stopTicker()
        viewModel.stopTicker()
    }
}
