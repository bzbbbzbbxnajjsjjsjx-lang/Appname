package com.example.androidapp.vardiya.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import java.math.BigDecimal

interface VardiyaRepository {
    fun getSalaryConfiguration(): SalaryConfiguration
    fun saveSalaryConfiguration(config: SalaryConfiguration)

    fun getActiveShift(): Shift?
    fun saveActiveShift(shift: Shift?)

    fun getShiftHistory(): List<CompletedShiftRecord>
    fun addShiftToHistory(record: CompletedShiftRecord)
    fun clearShiftHistory()
}

/**
 * Android SharedPreferences implementation of VardiyaRepository.
 */
class LocalVardiyaRepository(
    private val prefs: SharedPreferences
) : VardiyaRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences("vardiya_preferences", Context.MODE_PRIVATE)
    )

    override fun getSalaryConfiguration(): SalaryConfiguration {
        return try {
            val salaryStr = prefs.getString(KEY_SALARY_MONTHLY, "28000") ?: "28000"
            val days = prefs.getInt(KEY_SALARY_DAYS, 22)
            val hoursStr = prefs.getString(KEY_SALARY_HOURS, "8") ?: "8"
            val breakMin = prefs.getInt(KEY_SALARY_BREAK_MIN, 60)
            val deductBreak = prefs.getBoolean(KEY_SALARY_DEDUCT_BREAK, false)
            val currency = prefs.getString(KEY_SALARY_CURRENCY, "₺") ?: "₺"
            val currencyCode = prefs.getString(KEY_SALARY_CURRENCY_CODE, "TRY") ?: "TRY"

            SalaryConfiguration(
                monthlySalary = BigDecimal(salaryStr),
                monthlyWorkDays = days,
                dailyWorkHours = BigDecimal(hoursStr),
                breakMinutes = breakMin,
                deductBreakFromSalary = deductBreak,
                currencySymbol = currency,
                currencyCode = currencyCode
            )
        } catch (e: Exception) {
            SalaryConfiguration()
        }
    }

    override fun saveSalaryConfiguration(config: SalaryConfiguration) {
        prefs.edit()
            .putString(KEY_SALARY_MONTHLY, config.monthlySalary.toPlainString())
            .putInt(KEY_SALARY_DAYS, config.monthlyWorkDays)
            .putString(KEY_SALARY_HOURS, config.dailyWorkHours.toPlainString())
            .putInt(KEY_SALARY_BREAK_MIN, config.breakMinutes)
            .putBoolean(KEY_SALARY_DEDUCT_BREAK, config.deductBreakFromSalary)
            .putString(KEY_SALARY_CURRENCY, config.currencySymbol)
            .putString(KEY_SALARY_CURRENCY_CODE, config.currencyCode)
            .apply()
    }

    override fun getActiveShift(): Shift? {
        return try {
            val shiftId = prefs.getString(KEY_SHIFT_ID, null) ?: return null
            val stateStr = prefs.getString(KEY_SHIFT_STATE, ShiftState.NOT_STARTED.name) ?: ShiftState.NOT_STARTED.name
            val state = try {
                ShiftState.valueOf(stateStr)
            } catch (e: Exception) {
                ShiftState.NOT_STARTED
            }

            if (state == ShiftState.NOT_STARTED) return null

            val startEpoch = prefs.getLong(KEY_SHIFT_START_EPOCH, 0L)
            val startElapsed = prefs.getLong(KEY_SHIFT_START_ELAPSED, 0L)
            val lastResumeEpoch = prefs.getLong(KEY_SHIFT_LAST_RESUME_EPOCH, startEpoch)
            val accumulated = prefs.getLong(KEY_SHIFT_ACCUMULATED, 0L)
            val lastResume = prefs.getLong(KEY_SHIFT_LAST_RESUME, 0L)
            val pauseEpoch = if (prefs.contains(KEY_SHIFT_PAUSE_EPOCH)) prefs.getLong(KEY_SHIFT_PAUSE_EPOCH, 0L) else null
            val finishEpoch = if (prefs.contains(KEY_SHIFT_FINISH_EPOCH)) prefs.getLong(KEY_SHIFT_FINISH_EPOCH, 0L) else null
            val earnedStr = prefs.getString(KEY_SHIFT_EARNED_FINISHED, null)
            val earnedFinished = earnedStr?.let { try { BigDecimal(it) } catch (e: Exception) { null } }

            Shift(
                id = shiftId,
                startEpochMillis = startEpoch,
                startElapsedRealtime = startElapsed,
                lastResumeEpochMillis = lastResumeEpoch,
                accumulatedActiveElapsedMs = accumulated,
                lastResumeElapsedRealtime = lastResume,
                pauseEpochMillis = pauseEpoch,
                finishEpochMillis = finishEpoch,
                state = state,
                salaryConfig = getSalaryConfiguration(),
                totalEarnedWhenFinished = earnedFinished
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun saveActiveShift(shift: Shift?) {
        val editor = prefs.edit()
        if (shift == null || shift.state == ShiftState.NOT_STARTED) {
            editor.remove(KEY_SHIFT_ID)
                .remove(KEY_SHIFT_STATE)
                .remove(KEY_SHIFT_START_EPOCH)
                .remove(KEY_SHIFT_START_ELAPSED)
                .remove(KEY_SHIFT_LAST_RESUME_EPOCH)
                .remove(KEY_SHIFT_ACCUMULATED)
                .remove(KEY_SHIFT_LAST_RESUME)
                .remove(KEY_SHIFT_PAUSE_EPOCH)
                .remove(KEY_SHIFT_FINISH_EPOCH)
                .remove(KEY_SHIFT_EARNED_FINISHED)
        } else {
            editor.putString(KEY_SHIFT_ID, shift.id)
                .putString(KEY_SHIFT_STATE, shift.state.name)
                .putLong(KEY_SHIFT_START_EPOCH, shift.startEpochMillis)
                .putLong(KEY_SHIFT_START_ELAPSED, shift.startElapsedRealtime)
                .putLong(KEY_SHIFT_LAST_RESUME_EPOCH, shift.lastResumeEpochMillis)
                .putLong(KEY_SHIFT_ACCUMULATED, shift.accumulatedActiveElapsedMs)
                .putLong(KEY_SHIFT_LAST_RESUME, shift.lastResumeElapsedRealtime)

            if (shift.pauseEpochMillis != null) {
                editor.putLong(KEY_SHIFT_PAUSE_EPOCH, shift.pauseEpochMillis)
            } else {
                editor.remove(KEY_SHIFT_PAUSE_EPOCH)
            }

            if (shift.finishEpochMillis != null) {
                editor.putLong(KEY_SHIFT_FINISH_EPOCH, shift.finishEpochMillis)
            } else {
                editor.remove(KEY_SHIFT_FINISH_EPOCH)
            }

            if (shift.totalEarnedWhenFinished != null) {
                editor.putString(KEY_SHIFT_EARNED_FINISHED, shift.totalEarnedWhenFinished.toPlainString())
            } else {
                editor.remove(KEY_SHIFT_EARNED_FINISHED)
            }
        }
        editor.apply()
    }

    override fun getShiftHistory(): List<CompletedShiftRecord> {
        val raw = try {
            prefs.getString(KEY_SHIFT_HISTORY_LIST, null)
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        if (raw.isBlank()) return emptyList()

        return raw.split(RECORD_DELIMITER).mapNotNull { item ->
            if (item.isBlank()) return@mapNotNull null
            val fields = item.split(FIELD_DELIMITER)
            if (fields.size >= 9) {
                try {
                    val id = unescapeField(fields[0])
                    val dateFormatted = unescapeField(fields[1])
                    val timeRangeFormatted = unescapeField(fields[2])
                    val durationFormatted = unescapeField(fields[3])
                    val earnedFormatted = unescapeField(fields[4])
                    val totalEarned = BigDecimal(unescapeField(fields[5]))
                    val activeDurationMs = unescapeField(fields[6]).toLong()
                    val startEpochMillis = unescapeField(fields[7]).toLong()
                    val finishEpochMillis = unescapeField(fields[8]).toLong()
                    val totalDurationMs = if (fields.size > 9) {
                        unescapeField(fields[9]).toLongOrNull() ?: activeDurationMs
                    } else activeDurationMs

                    val config = if (fields.size >= 17) {
                        SalaryConfiguration(
                            monthlySalary = BigDecimal(unescapeField(fields[10])),
                            monthlyWorkDays = unescapeField(fields[11]).toInt(),
                            dailyWorkHours = BigDecimal(unescapeField(fields[12])),
                            breakMinutes = unescapeField(fields[13]).toInt(),
                            deductBreakFromSalary = unescapeField(fields[14]).toBoolean(),
                            currencySymbol = unescapeField(fields[15]),
                            currencyCode = unescapeField(fields[16])
                        )
                    } else {
                        SalaryConfiguration()
                    }

                    CompletedShiftRecord(
                        id = id,
                        dateFormatted = dateFormatted,
                        timeRangeFormatted = timeRangeFormatted,
                        durationFormatted = durationFormatted,
                        earnedFormatted = earnedFormatted,
                        totalEarned = totalEarned,
                        activeDurationMs = activeDurationMs,
                        totalDurationMs = totalDurationMs,
                        startEpochMillis = startEpochMillis,
                        finishEpochMillis = finishEpochMillis,
                        salaryConfigSnapshot = config,
                        currencySymbol = config.currencySymbol,
                        currencyCode = config.currencyCode
                    )
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }

    override fun addShiftToHistory(record: CompletedShiftRecord) {
        val current = getShiftHistory().toMutableList()
        current.add(0, record) // Newest first
        val trimmed = if (current.size > 50) current.take(50) else current

        val serialized = trimmed.joinToString(RECORD_DELIMITER) { r ->
            val c = r.salaryConfigSnapshot
            listOf(
                escapeField(r.id),
                escapeField(r.dateFormatted),
                escapeField(r.timeRangeFormatted),
                escapeField(r.durationFormatted),
                escapeField(r.earnedFormatted),
                escapeField(r.totalEarned.toPlainString()),
                escapeField(r.activeDurationMs.toString()),
                escapeField(r.startEpochMillis.toString()),
                escapeField(r.finishEpochMillis.toString()),
                escapeField(r.totalDurationMs.toString()),
                escapeField(c.monthlySalary.toPlainString()),
                escapeField(c.monthlyWorkDays.toString()),
                escapeField(c.dailyWorkHours.toPlainString()),
                escapeField(c.breakMinutes.toString()),
                escapeField(c.deductBreakFromSalary.toString()),
                escapeField(c.currencySymbol),
                escapeField(c.currencyCode)
            ).joinToString(FIELD_DELIMITER)
        }

        prefs.edit().putString(KEY_SHIFT_HISTORY_LIST, serialized).apply()
    }

    override fun clearShiftHistory() {
        prefs.edit().remove(KEY_SHIFT_HISTORY_LIST).apply()
    }

    companion object {
        private const val KEY_SALARY_MONTHLY = "salary_monthly"
        private const val KEY_SALARY_DAYS = "salary_days"
        private const val KEY_SALARY_HOURS = "salary_hours"
        private const val KEY_SALARY_BREAK_MIN = "salary_break_min"
        private const val KEY_SALARY_DEDUCT_BREAK = "salary_deduct_break"
        private const val KEY_SALARY_CURRENCY = "salary_currency"
        private const val KEY_SALARY_CURRENCY_CODE = "salary_currency_code"

        private const val KEY_SHIFT_ID = "shift_id"
        private const val KEY_SHIFT_STATE = "shift_state"
        private const val KEY_SHIFT_START_EPOCH = "shift_start_epoch"
        private const val KEY_SHIFT_START_ELAPSED = "shift_start_elapsed"
        private const val KEY_SHIFT_LAST_RESUME_EPOCH = "shift_last_resume_epoch"
        private const val KEY_SHIFT_ACCUMULATED = "shift_accumulated"
        private const val KEY_SHIFT_LAST_RESUME = "shift_last_resume"
        private const val KEY_SHIFT_PAUSE_EPOCH = "shift_pause_epoch"
        private const val KEY_SHIFT_FINISH_EPOCH = "shift_finish_epoch"
        private const val KEY_SHIFT_EARNED_FINISHED = "shift_earned_finished"

        private const val KEY_SHIFT_HISTORY_LIST = "shift_history_list"
        private const val RECORD_DELIMITER = "###REC###"
        private const val FIELD_DELIMITER = ":::FLD:::"

        internal fun escapeField(value: String): String {
            return value.replace("\\", "\\\\")
                .replace(":", "\\c")
                .replace("#", "\\h")
        }

        internal fun unescapeField(value: String): String {
            val sb = java.lang.StringBuilder()
            var i = 0
            while (i < value.length) {
                if (value[i] == '\\' && i + 1 < value.length) {
                    when (value[i + 1]) {
                        '\\' -> { sb.append('\\'); i += 2 }
                        'c' -> { sb.append(':'); i += 2 }
                        'h' -> { sb.append('#'); i += 2 }
                        else -> { sb.append(value[i]); i++ }
                    }
                } else {
                    sb.append(value[i])
                    i++
                }
            }
            return sb.toString()
        }
    }
}
