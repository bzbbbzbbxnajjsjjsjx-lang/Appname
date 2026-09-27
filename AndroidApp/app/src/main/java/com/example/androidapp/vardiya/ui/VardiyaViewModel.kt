package com.example.androidapp.vardiya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.vardiya.data.repository.VardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
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
import java.text.SimpleDateFormat
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

    init {
        loadPersistedState()
    }

    fun loadPersistedState() {
        val config = repository.getSalaryConfiguration()
        val persistedShift = repository.getActiveShift()
        val historyList = repository.getShiftHistory()

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
                    history = historyList
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
    }

    fun startShift() {
        if (_uiState.value.shiftState == ShiftState.RUNNING) return

        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val nowEpoch = timeProvider.currentEpochMillis()
        val config = _uiState.value.salaryConfig

        val newShift = Shift(
            id = UUID.randomUUID().toString(),
            startEpochMillis = nowEpoch,
            startElapsedRealtime = nowElapsed,
            accumulatedActiveElapsedMs = 0L,
            lastResumeElapsedRealtime = nowElapsed,
            state = ShiftState.RUNNING,
            salaryConfig = config
        )

        repository.saveActiveShift(newShift)

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

    fun pauseShift() {
        val current = _uiState.value.currentShift ?: return
        if (current.state != ShiftState.RUNNING) return

        stopTicker()

        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val nowEpoch = timeProvider.currentEpochMillis()
        val segment = (nowElapsed - current.lastResumeElapsedRealtime).coerceAtLeast(0L)
        val totalActive = current.accumulatedActiveElapsedMs + segment

        val pausedShift = current.copy(
            accumulatedActiveElapsedMs = totalActive,
            pauseEpochMillis = nowEpoch,
            state = ShiftState.PAUSED
        )

        repository.saveActiveShift(pausedShift)

        val earnings = calculator.calculateEarnings(pausedShift, totalActive)
        _uiState.update {
            it.copy(
                shiftState = ShiftState.PAUSED,
                currentShift = pausedShift,
                earnings = earnings
            )
        }
    }

    fun resumeShift() {
        val current = _uiState.value.currentShift ?: return
        if (current.state != ShiftState.PAUSED) return

        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val resumedShift = current.copy(
            lastResumeElapsedRealtime = nowElapsed,
            pauseEpochMillis = null,
            state = ShiftState.RUNNING
        )

        repository.saveActiveShift(resumedShift)

        _uiState.update {
            it.copy(
                shiftState = ShiftState.RUNNING,
                currentShift = resumedShift
            )
        }

        startTicker()
    }

    fun finishShift() {
        val current = _uiState.value.currentShift ?: return
        if (current.state != ShiftState.RUNNING && current.state != ShiftState.PAUSED) return

        stopTicker()

        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val nowEpoch = timeProvider.currentEpochMillis()
        val totalActiveMs = calculator.calculateActiveDurationMs(current, nowElapsed, nowEpoch)
        val finalEarnings = calculator.calculateEarnings(current, totalActiveMs)

        val finishedShift = current.copy(
            finishEpochMillis = nowEpoch,
            accumulatedActiveElapsedMs = totalActiveMs,
            state = ShiftState.FINISHED,
            totalEarnedWhenFinished = finalEarnings.earnedAmount
        )

        repository.saveActiveShift(finishedShift)

        // Record history entry
        val historyRecord = createHistoryRecord(finishedShift, finalEarnings.formattedEarned)
        repository.addShiftToHistory(historyRecord)

        val updatedHistory = repository.getShiftHistory()

        _uiState.update {
            it.copy(
                shiftState = ShiftState.FINISHED,
                currentShift = finishedShift,
                earnings = finalEarnings,
                history = updatedHistory
            )
        }
    }

    fun resetShift() {
        stopTicker()
        repository.saveActiveShift(null)

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

    fun updateSalaryConfig(newConfig: SalaryConfiguration) {
        repository.saveSalaryConfiguration(newConfig)

        val currentShift = _uiState.value.currentShift
        val updatedShift = currentShift?.copy(salaryConfig = newConfig)
        if (updatedShift != null) {
            repository.saveActiveShift(updatedShift)
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
        _uiState.update { it.copy(isHistoryVisible = false) }
    }

    fun clearHistory() {
        repository.clearShiftHistory()
        _uiState.update { it.copy(history = emptyList()) }
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
                val currentEarnings = calculator.calculateEarnings(current, activeMs)

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

    private fun createHistoryRecord(shift: Shift, earnedFormatted: String): CompletedShiftRecord {
        val turkishLocale = Locale("tr", "TR")
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", turkishLocale)
        val timeFormat = SimpleDateFormat("HH:mm", turkishLocale)

        val startDate = Date(shift.startEpochMillis)
        val finishDate = Date(shift.finishEpochMillis ?: shift.startEpochMillis)

        val dateStr = dateFormat.format(startDate)
        val timeRange = "${timeFormat.format(startDate)} — ${timeFormat.format(finishDate)}"

        val totalMinutes = shift.accumulatedActiveElapsedMs / 60000
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val durationFormatted = "${hours}s ${mins}dk"

        return CompletedShiftRecord(
            id = shift.id,
            dateFormatted = dateStr,
            timeRangeFormatted = timeRange,
            durationFormatted = durationFormatted,
            earnedFormatted = earnedFormatted,
            totalEarned = shift.totalEarnedWhenFinished ?: shift.salaryConfig.hourlyRate,
            activeDurationMs = shift.accumulatedActiveElapsedMs,
            startEpochMillis = shift.startEpochMillis,
            finishEpochMillis = shift.finishEpochMillis ?: shift.startEpochMillis
        )
    }
}
