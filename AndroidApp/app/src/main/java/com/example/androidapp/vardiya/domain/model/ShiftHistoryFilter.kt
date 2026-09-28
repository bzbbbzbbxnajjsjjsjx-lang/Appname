package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

/**
 * Filter categories for Shift History.
 */
enum class ShiftHistoryFilterType(val label: String) {
    ALL("Tümü"),
    THIS_MONTH("Bu Ay"),
    THIS_WEEK("Bu Hafta"),
    OVERTIME("Fazla Mesai"),
    NIGHT_SHIFT("Gece")
}

/**
 * Simple immutable date representation for calendar navigation and heatmap filtering.
 * [month] is 1-indexed (1 = January, 12 = December).
 */
data class CalendarDate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int
) : Comparable<CalendarDate> {

    override fun compareTo(other: CalendarDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return dayOfMonth.compareTo(other.dayOfMonth)
    }

    fun formattedDisplay(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.forLanguageTag("tr-TR")) ?: "$month"
        return "$dayOfMonth $monthName $year"
    }

    companion object {
        fun fromEpochMillis(epochMillis: Long): CalendarDate {
            val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
            return CalendarDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun today(): CalendarDate = fromEpochMillis(System.currentTimeMillis())
    }
}

/**
 * Aggregated shift statistics for a single calendar day.
 */
data class DayShiftSummary(
    val date: CalendarDate,
    val shiftCount: Int,
    val totalDurationMs: Long,
    val totalEarned: BigDecimal,
    val hasOvertime: Boolean,
    val hasNightShift: Boolean
) {
    val totalHours: Double
        get() = totalDurationMs.toDouble() / 3_600_000.0

    /**
     * Heat intensity scale from 0 to 3:
     * 0: No shifts
     * 1: Light shift (< 5 hours)
     * 2: Standard full shift (5 - 8.5 hours)
     * 3: Extended / Overtime shift (> 8.5 hours or with overtime bonus)
     */
    val heatIntensity: Int
        get() = when {
            shiftCount == 0 -> 0
            hasOvertime || totalHours > 8.5 -> 3
            totalHours >= 5.0 -> 2
            else -> 1
        }
}

/**
 * Overall summary metrics for a filtered set of completed shifts.
 */
data class ShiftHistorySummary(
    val totalCount: Int,
    val totalDurationMs: Long,
    val totalEarned: BigDecimal,
    val totalOvertimeEarned: BigDecimal,
    val totalNightDifferentialEarned: BigDecimal
) {
    val formattedTotalDuration: String
        get() {
            val totalMinutes = (totalDurationMs / 60000).coerceAtLeast(0L)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}s ${mins}dk"
        }
}

/**
 * Pure engine for filtering and aggregating shift history records.
 */
object ShiftHistoryFilterEngine {

    fun filter(
        history: List<CompletedShiftRecord>,
        query: String = "",
        filterType: ShiftHistoryFilterType = ShiftHistoryFilterType.ALL,
        selectedDate: CalendarDate? = null,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<CompletedShiftRecord> {
        val turkishLocale = Locale.forLanguageTag("tr-TR")
        val cleanQuery = query.trim().lowercase(turkishLocale)

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowEpochMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH) + 1
        val currentWeek = nowCal.get(Calendar.WEEK_OF_YEAR)

        return history.filter { record ->
            // 1. Text Search Filter
            if (cleanQuery.isNotEmpty()) {
                val matchesNote = record.note?.lowercase(turkishLocale)?.contains(cleanQuery) == true
                val matchesDate = record.dateFormatted.lowercase(turkishLocale).contains(cleanQuery)
                val matchesTime = record.timeRangeFormatted.lowercase(turkishLocale).contains(cleanQuery)
                val matchesTemplate = record.templateId?.lowercase(turkishLocale)?.contains(cleanQuery) == true
                if (!matchesNote && !matchesDate && !matchesTime && !matchesTemplate) {
                    return@filter false
                }
            }

            val recordDate = CalendarDate.fromEpochMillis(record.startEpochMillis)

            // 2. Specific Date Filter (e.g. from calendar selection)
            if (selectedDate != null && recordDate != selectedDate) {
                return@filter false
            }

            // 3. Category Filter
            when (filterType) {
                ShiftHistoryFilterType.ALL -> true
                ShiftHistoryFilterType.THIS_MONTH -> {
                    recordDate.year == currentYear && recordDate.month == currentMonth
                }
                ShiftHistoryFilterType.THIS_WEEK -> {
                    val recordCal = Calendar.getInstance().apply { timeInMillis = record.startEpochMillis }
                    recordCal.get(Calendar.YEAR) == currentYear &&
                        recordCal.get(Calendar.WEEK_OF_YEAR) == currentWeek
                }
                ShiftHistoryFilterType.OVERTIME -> {
                    record.overtimeEarned > BigDecimal.ZERO || record.overtimeDurationMs > 0L
                }
                ShiftHistoryFilterType.NIGHT_SHIFT -> {
                    record.nightDifferentialEarned > BigDecimal.ZERO || record.nightShiftDurationMs > 0L
                }
            }
        }
    }

    fun summarize(records: List<CompletedShiftRecord>): ShiftHistorySummary {
        val totalCount = records.size
        var totalDuration = 0L
        var totalEarned = BigDecimal.ZERO
        var totalOvertimeEarned = BigDecimal.ZERO
        var totalNightEarned = BigDecimal.ZERO

        for (r in records) {
            totalDuration += r.activeDurationMs
            totalEarned = totalEarned.add(r.totalEarned)
            totalOvertimeEarned = totalOvertimeEarned.add(r.overtimeEarned)
            totalNightEarned = totalNightEarned.add(r.nightDifferentialEarned)
        }

        return ShiftHistorySummary(
            totalCount = totalCount,
            totalDurationMs = totalDuration,
            totalEarned = totalEarned,
            totalOvertimeEarned = totalOvertimeEarned,
            totalNightDifferentialEarned = totalNightEarned
        )
    }

    fun aggregateMonth(
        history: List<CompletedShiftRecord>,
        year: Int,
        month: Int // 1-indexed (1..12)
    ): Map<Int, DayShiftSummary> {
        val result = mutableMapOf<Int, DayShiftSummary>()

        val monthShifts = history.filter { record ->
            val date = CalendarDate.fromEpochMillis(record.startEpochMillis)
            date.year == year && date.month == month
        }

        val grouped = monthShifts.groupBy {
            CalendarDate.fromEpochMillis(it.startEpochMillis).dayOfMonth
        }

        for ((day, shifts) in grouped) {
            var duration = 0L
            var earned = BigDecimal.ZERO
            var hasOt = false
            var hasNight = false

            for (s in shifts) {
                duration += s.activeDurationMs
                earned = earned.add(s.totalEarned)
                if (s.overtimeEarned > BigDecimal.ZERO || s.overtimeDurationMs > 0L) {
                    hasOt = true
                }
                if (s.nightDifferentialEarned > BigDecimal.ZERO || s.nightShiftDurationMs > 0L) {
                    hasNight = true
                }
            }

            result[day] = DayShiftSummary(
                date = CalendarDate(year, month, day),
                shiftCount = shifts.size,
                totalDurationMs = duration,
                totalEarned = earned,
                hasOvertime = hasOt,
                hasNightShift = hasNight
            )
        }

        return result
    }
}
