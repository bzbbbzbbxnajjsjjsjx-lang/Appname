package com.example.androidapp.vardiya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.vardiya.data.repository.VardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.BreakRecord
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.model.ShiftTemplate
import com.example.androidapp.vardiya.domain.time.DefaultTimeProvider
import com.example.androidapp.vardiya.domain.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.androidapp.vardiya.domain.analytics.AnalyticsPeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class VardiyaViewModel(
    private val repository: VardiyaRepository,
    private val calculator: ShiftEarningsCalculator = ShiftEarningsCalculator(),
    private val timeProvider: TimeProvider = DefaultTimeProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(VardiyaUiState())
    val uiState: StateFlow<VardiyaUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private val actionMutex = Mutex()

    init {
        loadPersistedState()
    }

    fun loadPersistedState() {
        val config = try {
            repository.getSalaryConfiguration()
        } catch (e: Exception) {
            SalaryConfiguration()
        }
        val persistedShift = try {
            repository.getActiveShift()
        } catch (e: Exception) {
            null
        }
        val historyList = try {
            repository.getShiftHistory()
        } catch (e: Exception) {
            emptyList()
        }

        if (persistedShift != null && persistedShift.state != ShiftState.NOT_STARTED) {
            val nowElapsed = timeProvider.elapsedRealtimeMillis()
            val nowEpoch = timeProvider.currentEpochMillis()
            val activeMs = calculator.calculateActiveDurationMs(persistedShift, nowElapsed, nowEpoch)
            val currentEarnings = calculator.calculateEarnings(persistedShift, activeMs)

            _uiState.update {
                it.copy(
                    shiftState = persistedShift.state,
                    salaryConfig = config,
                    earnings = currentEarnings,
                    currentShift = persistedShift,
                    history = historyList,
                    selectedTemplateId = persistedShift.templateId
                )
            }

            if (persistedShift.state == ShiftState.RUNNING) {
                startTicker()
            }
        } else {
            val emptyShift = Shift(salaryConfig = config)
            val baselineEarnings = calculator.calculateEarnings(emptyShift, 0L)
            _uiState.update {
                it.copy(
                    shiftState = ShiftState.NOT_STARTED,
                    salaryConfig = config,
                    earnings = baselineEarnings,
                    currentShift = null,
                    history = historyList
                )
            }
        }

        val initNowEpoch = timeProvider.currentEpochMillis()
        val initCal = Calendar.getInstance().apply { timeInMillis = initNowEpoch }
        _uiState.update {
            it.copy(
                analyticsWeekAnchorMillis = initNowEpoch,
                analyticsYear = initCal.get(Calendar.YEAR),
                analyticsMonth = initCal.get(Calendar.MONTH) + 1
            )
        }
    }

    fun startShift() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val currentState = _uiState.value.shiftState
                if (!currentState.canTransitionTo(ShiftState.RUNNING)) return@withLock

                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val nowEpoch = timeProvider.currentEpochMillis()
                val config = _uiState.value.salaryConfig

                val newShift = Shift(
                    id = UUID.randomUUID().toString(),
                    startEpochMillis = nowEpoch,
                    startElapsedRealtime = nowElapsed,
                    lastResumeEpochMillis = nowEpoch,
                    accumulatedActiveElapsedMs = 0L,
                    lastResumeElapsedRealtime = nowElapsed,
                    state = ShiftState.RUNNING,
                    salaryConfig = config,
                    templateId = _uiState.value.selectedTemplateId
                )

                try {
                    repository.saveActiveShift(newShift)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Vardiya kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                val earnings = calculator.calculateEarnings(newShift, 0L)
                _uiState.update {
                    it.copy(
                        shiftState = ShiftState.RUNNING,
                        currentShift = newShift,
                        earnings = earnings
                    )
                }

                startTicker()
            }
        }
    }

    fun pauseShift() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift ?: return@withLock
                if (!current.state.canTransitionTo(ShiftState.PAUSED)) return@withLock

                stopTicker()

                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val nowEpoch = timeProvider.currentEpochMillis()
                val totalActive = calculator.calculateActiveDurationMs(current, nowElapsed, nowEpoch)

                val pausedShift = current.copy(
                    accumulatedActiveElapsedMs = totalActive,
                    pauseEpochMillis = nowEpoch,
                    state = ShiftState.PAUSED
                )

                try {
                    repository.saveActiveShift(pausedShift)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Vardiya duraklatma kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                val earnings = calculator.calculateEarnings(pausedShift, totalActive)
                _uiState.update {
                    it.copy(
                        shiftState = ShiftState.PAUSED,
                        currentShift = pausedShift,
                        earnings = earnings
                    )
                }
            }
        }
    }

    fun resumeShift() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift ?: return@withLock
                if (!current.state.canTransitionTo(ShiftState.RUNNING)) return@withLock

                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val nowEpoch = timeProvider.currentEpochMillis()

                val resumedShift = current.copy(
                    lastResumeElapsedRealtime = nowElapsed,
                    lastResumeEpochMillis = nowEpoch,
                    pauseEpochMillis = null,
                    state = ShiftState.RUNNING
                )

                try {
                    repository.saveActiveShift(resumedShift)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Vardiya devam durumu kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                _uiState.update {
                    it.copy(
                        shiftState = ShiftState.RUNNING,
                        currentShift = resumedShift
                    )
                }

                startTicker()
            }
        }
    }

    fun startBreak(isDeductedFromSalary: Boolean = false, note: String? = null) {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift ?: return@withLock
                if (current.state != ShiftState.RUNNING) return@withLock
                if (current.activeBreaks.any { it.isOngoing }) return@withLock

                val nowEpoch = timeProvider.currentEpochMillis()
                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val newBreak = BreakRecord(
                    startEpochMillis = nowEpoch,
                    isDeductedFromSalary = isDeductedFromSalary,
                    note = note
                )
                val updatedBreaks = current.activeBreaks + newBreak
                val updatedShift = current.copy(activeBreaks = updatedBreaks)

                try {
                    repository.saveActiveShift(updatedShift)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Mola başlatılamadı: ${e.localizedMessage ?: e.message}") }
                }

                val activeMs = calculator.calculateActiveDurationMs(updatedShift, nowElapsed, nowEpoch)
                val earnings = calculator.calculateEarnings(updatedShift, activeMs, nowEpoch)

                _uiState.update {
                    it.copy(
                        currentShift = updatedShift,
                        earnings = earnings
                    )
                }
            }
        }
    }

    fun endBreak() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift ?: return@withLock
                if (current.state != ShiftState.RUNNING) return@withLock
                if (!current.activeBreaks.any { it.isOngoing }) return@withLock

                val nowEpoch = timeProvider.currentEpochMillis()
                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val updatedBreaks = current.activeBreaks.map {
                    if (it.isOngoing) it.copy(endEpochMillis = nowEpoch) else it
                }
                val updatedShift = current.copy(activeBreaks = updatedBreaks)

                try {
                    repository.saveActiveShift(updatedShift)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Mola tamamlanamadı: ${e.localizedMessage ?: e.message}") }
                }

                val activeMs = calculator.calculateActiveDurationMs(updatedShift, nowElapsed, nowEpoch)
                val earnings = calculator.calculateEarnings(updatedShift, activeMs, nowEpoch)

                _uiState.update {
                    it.copy(
                        currentShift = updatedShift,
                        earnings = earnings
                    )
                }
            }
        }
    }

    fun toggleBreak() {
        if (_uiState.value.isBreakActive) {
            endBreak()
        } else {
            startBreak()
        }
    }

    fun finishShift() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift ?: return@withLock
                if (!current.state.canTransitionTo(ShiftState.FINISHED)) return@withLock

                stopTicker()

                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val nowEpoch = timeProvider.currentEpochMillis()

                // Close any ongoing break cleanly
                val finalBreaks = if (current.activeBreaks.any { it.isOngoing }) {
                    current.activeBreaks.map { if (it.isOngoing) it.copy(endEpochMillis = nowEpoch) else it }
                } else {
                    current.activeBreaks
                }
                val currentWithFinalBreaks = current.copy(activeBreaks = finalBreaks)

                val totalActiveMs = calculator.calculateActiveDurationMs(currentWithFinalBreaks, nowElapsed, nowEpoch)
                val finalEarnings = calculator.calculateEarnings(currentWithFinalBreaks, totalActiveMs, nowEpoch)

                val finishedShift = currentWithFinalBreaks.copy(
                    finishEpochMillis = nowEpoch,
                    accumulatedActiveElapsedMs = totalActiveMs,
                    state = ShiftState.FINISHED,
                    totalEarnedWhenFinished = finalEarnings.earnedAmount
                )

                try {
                    repository.saveActiveShift(finishedShift)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Vardiya tamamlama kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                // Record immutable history entry with frozen salary config snapshot
                val historyRecord = createHistoryRecord(finishedShift, finalEarnings.formattedEarned, nowEpoch)
                try {
                    repository.addShiftToHistory(historyRecord)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Vardiya geçmişi kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                val updatedHistory = try {
                    repository.getShiftHistory()
                } catch (e: Exception) {
                    _uiState.value.history
                }

                _uiState.update {
                    it.copy(
                        shiftState = ShiftState.FINISHED,
                        currentShift = finishedShift,
                        earnings = finalEarnings,
                        history = updatedHistory
                    )
                }
            }
        }
    }

    fun resetShift() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift
                if (current != null && !current.state.canTransitionTo(ShiftState.NOT_STARTED)) {
                    return@withLock
                }

                stopTicker()
                try {
                    repository.saveActiveShift(null)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Vardiya sıfırlama kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                val config = _uiState.value.salaryConfig
                val emptyShift = Shift(salaryConfig = config)
                val baselineEarnings = calculator.calculateEarnings(emptyShift, 0L)

                _uiState.update {
                    it.copy(
                        shiftState = ShiftState.NOT_STARTED,
                        currentShift = null,
                        earnings = baselineEarnings
                    )
                }
            }
        }
    }

    fun updateSalaryConfig(newConfig: SalaryConfiguration) {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                try {
                    repository.saveSalaryConfiguration(newConfig)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Maaş ayarları kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                val currentShift = _uiState.value.currentShift
                val updatedShift = currentShift?.copy(salaryConfig = newConfig)
                if (updatedShift != null) {
                    try {
                        repository.saveActiveShift(updatedShift)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Aktif vardiya güncellenemedi: ${e.localizedMessage ?: e.message}") }
                    }
                }

                val activeDurationMs = _uiState.value.earnings.activeDurationMs
                val dummyOrCurrentShift = updatedShift ?: Shift(salaryConfig = newConfig)
                val updatedEarnings = calculator.calculateEarnings(dummyOrCurrentShift, activeDurationMs)

                _uiState.update {
                    it.copy(
                        salaryConfig = newConfig,
                        currentShift = updatedShift,
                        earnings = updatedEarnings,
                        isSetupVisible = false
                    )
                }
            }
        }
    }

    fun openSetup() {
        _uiState.update { it.copy(isSetupVisible = true) }
    }

    fun closeSetup() {
        _uiState.update { it.copy(isSetupVisible = false) }
    }

    fun openHistory() {
        _uiState.update { it.copy(isHistoryVisible = true) }
    }

    fun closeHistory() {
        _uiState.update { it.copy(isHistoryVisible = false, selectedHistoryRecord = null) }
    }

    fun openTemplatePicker() {
        _uiState.update { it.copy(isTemplatePickerVisible = true) }
    }

    fun closeTemplatePicker() {
        _uiState.update { it.copy(isTemplatePickerVisible = false) }
    }

    fun applyShiftTemplate(template: ShiftTemplate) {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val currentConfig = _uiState.value.salaryConfig
                val updatedConfig = currentConfig.copy(
                    dailyWorkHours = template.durationHours,
                    breakMinutes = template.breakMinutes,
                    deductBreakFromSalary = template.deductBreakFromSalary
                )

                try {
                    repository.saveSalaryConfiguration(updatedConfig)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Şablon ayarları kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                }

                val current = _uiState.value.currentShift
                val updatedShift = current?.copy(
                    salaryConfig = updatedConfig,
                    templateId = template.id
                )
                if (updatedShift != null) {
                    try {
                        repository.saveActiveShift(updatedShift)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Aktif vardiya şablonu kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                    }
                }

                val activeMs = _uiState.value.earnings.activeDurationMs
                val shiftForCalc = updatedShift ?: Shift(salaryConfig = updatedConfig, templateId = template.id)
                val updatedEarnings = calculator.calculateEarnings(shiftForCalc, activeMs)

                _uiState.update {
                    it.copy(
                        selectedTemplateId = template.id,
                        salaryConfig = updatedConfig,
                        currentShift = updatedShift,
                        earnings = updatedEarnings,
                        isTemplatePickerVisible = false
                    )
                }
            }
        }
    }

    fun clearSelectedTemplate() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                val current = _uiState.value.currentShift
                val updatedShift = current?.copy(templateId = null)
                if (updatedShift != null) {
                    try {
                        repository.saveActiveShift(updatedShift)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Şablon temizleme kaydedilemedi: ${e.localizedMessage ?: e.message}") }
                    }
                }
                _uiState.update {
                    it.copy(
                        selectedTemplateId = null,
                        currentShift = updatedShift,
                        isTemplatePickerVisible = false
                    )
                }
            }
        }
    }

    fun openBreakSheet() {
        _uiState.update { it.copy(isBreakSheetVisible = true) }
    }

    fun closeBreakSheet() {
        _uiState.update { it.copy(isBreakSheetVisible = false) }
    }

    fun selectHistoryRecord(record: CompletedShiftRecord?) {
        _uiState.update { it.copy(selectedHistoryRecord = record) }
    }

    fun clearHistory() {
        viewModelScope.launch(dispatcher) {
            actionMutex.withLock {
                try {
                    repository.clearShiftHistory()
                    _uiState.update { it.copy(history = emptyList(), selectedHistoryRecord = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Geçmiş temizlenemedi: ${e.localizedMessage ?: e.message}") }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _uiState.update { it.copy(analyticsPeriod = period) }
    }

    fun navigateAnalyticsPrevious() {
        _uiState.update { current ->
            when (current.analyticsPeriod) {
                AnalyticsPeriod.WEEKLY -> {
                    val prevWeek = current.analyticsWeekAnchorMillis - 7 * 86_400_000L
                    current.copy(analyticsWeekAnchorMillis = prevWeek)
                }
                AnalyticsPeriod.MONTHLY -> {
                    if (current.analyticsMonth == 1) {
                        current.copy(analyticsMonth = 12, analyticsYear = current.analyticsYear - 1)
                    } else {
                        current.copy(analyticsMonth = current.analyticsMonth - 1)
                    }
                }
            }
        }
    }

    fun navigateAnalyticsNext() {
        _uiState.update { current ->
            when (current.analyticsPeriod) {
                AnalyticsPeriod.WEEKLY -> {
                    val nextWeek = current.analyticsWeekAnchorMillis + 7 * 86_400_000L
                    current.copy(analyticsWeekAnchorMillis = nextWeek)
                }
                AnalyticsPeriod.MONTHLY -> {
                    if (current.analyticsMonth == 12) {
                        current.copy(analyticsMonth = 1, analyticsYear = current.analyticsYear + 1)
                    } else {
                        current.copy(analyticsMonth = current.analyticsMonth + 1)
                    }
                }
            }
        }
    }

    fun resetAnalyticsToCurrent() {
        val nowEpoch = timeProvider.currentEpochMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = nowEpoch }
        _uiState.update {
            it.copy(
                analyticsWeekAnchorMillis = nowEpoch,
                analyticsYear = cal.get(Calendar.YEAR),
                analyticsMonth = cal.get(Calendar.MONTH) + 1
            )
        }
    }


    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch(dispatcher) {
            while (isActive) {
                delay(1000L)
                val current = _uiState.value.currentShift ?: break
                if (current.state != ShiftState.RUNNING) break

                val nowElapsed = timeProvider.elapsedRealtimeMillis()
                val nowEpoch = timeProvider.currentEpochMillis()
                val activeMs = calculator.calculateActiveDurationMs(current, nowElapsed, nowEpoch)
                val currentEarnings = calculator.calculateEarnings(current, activeMs, nowEpoch)

                _uiState.update {
                    it.copy(earnings = currentEarnings)
                }
            }
        }
    }

    fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTicker()
    }

    private fun createHistoryRecord(
        shift: Shift,
        earnedFormatted: String,
        finishEpoch: Long
    ): CompletedShiftRecord {
        val turkishLocale = Locale.forLanguageTag("tr-TR")
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", turkishLocale)
        val timeFormat = SimpleDateFormat("HH:mm", turkishLocale)

        val startDate = Date(shift.startEpochMillis)
        val finishDate = Date(finishEpoch)

        val dateStr = dateFormat.format(startDate)
        val timeRange = "${timeFormat.format(startDate)} — ${timeFormat.format(finishDate)}"

        val totalMinutes = (shift.accumulatedActiveElapsedMs / 60000).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val durationFormatted = "${hours}s ${mins}dk"
        val totalSpanMs = (finishEpoch - shift.startEpochMillis).coerceAtLeast(shift.accumulatedActiveElapsedMs)
        val finalEarnings = calculator.calculateEarnings(shift, shift.accumulatedActiveElapsedMs, finishEpoch)

        return CompletedShiftRecord(
            id = shift.id,
            dateFormatted = dateStr,
            timeRangeFormatted = timeRange,
            durationFormatted = durationFormatted,
            earnedFormatted = earnedFormatted,
            totalEarned = shift.totalEarnedWhenFinished ?: finalEarnings.earnedAmount,
            activeDurationMs = shift.accumulatedActiveElapsedMs,
            totalDurationMs = totalSpanMs,
            startEpochMillis = shift.startEpochMillis,
            finishEpochMillis = finishEpoch,
            salaryConfigSnapshot = shift.salaryConfig,
            currencySymbol = shift.salaryConfig.currencySymbol,
            currencyCode = shift.salaryConfig.currencyCode,
            baseEarned = finalEarnings.baseEarned,
            overtimeEarned = finalEarnings.overtimeEarned,
            nightDifferentialEarned = finalEarnings.nightDifferentialEarned,
            regularDurationMs = finalEarnings.regularDurationMs,
            overtimeDurationMs = finalEarnings.overtimeDurationMs,
            nightShiftDurationMs = finalEarnings.nightShiftDurationMs,
            templateId = shift.templateId,
            note = shift.note,
            breaks = shift.activeBreaks
        )
    }
}
