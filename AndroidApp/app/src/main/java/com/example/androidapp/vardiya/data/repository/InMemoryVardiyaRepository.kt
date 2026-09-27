package com.example.androidapp.vardiya.data.repository

import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift

/**
 * Pure in-memory implementation of VardiyaRepository for hermetic unit testing.
 */
class InMemoryVardiyaRepository(
    initialSalary: SalaryConfiguration = SalaryConfiguration(),
    initialShift: Shift? = null,
    initialHistory: List<CompletedShiftRecord> = emptyList()
) : VardiyaRepository {

    private var salaryConfig: SalaryConfiguration = initialSalary
    private var activeShift: Shift? = initialShift
    private val history = initialHistory.toMutableList()

    override fun getSalaryConfiguration(): SalaryConfiguration = salaryConfig

    override fun saveSalaryConfiguration(config: SalaryConfiguration) {
        salaryConfig = config
    }

    override fun getActiveShift(): Shift? = activeShift

    override fun saveActiveShift(shift: Shift?) {
        activeShift = shift
    }

    override fun getShiftHistory(): List<CompletedShiftRecord> = history.toList()

    override fun addShiftToHistory(record: CompletedShiftRecord) {
        history.add(0, record)
    }

    override fun clearShiftHistory() {
        history.clear()
    }
}
