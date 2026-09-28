package com.example.androidapp.vardiya.domain.analytics

import com.example.androidapp.vardiya.domain.model.CalendarDate
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale

/**
 * Period mode for analytics screen.
 */
enum class AnalyticsPeriod(val label: String) {
    WEEKLY("Haftalık"),
    MONTHLY("Aylık")
}

/**
 * Aggregated metrics for a single day in weekly analytics.
 */
data class DailyAnalyticsPoint(
    val date: CalendarDate,
    val dayOfWeekName: String, // "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"
    val shiftCount: Int,
    val totalEarned: BigDecimal,
    val baseEarned: BigDecimal,
    val overtimeEarned: BigDecimal,
    val nightDifferentialEarned: BigDecimal,
    val activeDurationMs: Long,
    val overtimeDurationMs: Long
) {
    val totalHours: Double
        get() = activeDurationMs.toDouble() / 3_600_000.0

    val overtimeHours: Double
        get() = overtimeDurationMs.toDouble() / 3_600_000.0
}

/**
 * Complete weekly analytics model.
 */
data class WeeklyAnalyticsData(
    val year: Int,
    val weekOfYear: Int,
    val weekRangeFormatted: String, // e.g. "22 Eylül — 28 Eylül 2026"
    val dailyPoints: List<DailyAnalyticsPoint>, // Exactly 7 days (Monday..Sunday)
    val totalEarned: BigDecimal,
    val totalDurationMs: Long,
    val totalShifts: Int,
    val totalOvertimeEarned: BigDecimal,
    val totalNightDifferentialEarned: BigDecimal,
    val averageDailyEarned: BigDecimal,
    val averageHourlyRate: BigDecimal
) {
    val totalHours: Double
        get() = totalDurationMs.toDouble() / 3_600_000.0

    val formattedTotalDuration: String
        get() {
            val totalMinutes = (totalDurationMs / 60000).coerceAtLeast(0L)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}s ${mins}dk"
        }

    val maxDailyEarned: BigDecimal
        get() = dailyPoints.maxOfOrNull { it.totalEarned } ?: BigDecimal.ZERO

    val maxDailyHours: Double
        get() = dailyPoints.maxOfOrNull { it.totalHours } ?: 0.0

    companion object {
        val EMPTY = WeeklyAnalyticsData(
            year = 2026,
            weekOfYear = 1,
            weekRangeFormatted = "",
            dailyPoints = emptyList(),
            totalEarned = BigDecimal.ZERO,
            totalDurationMs = 0L,
            totalShifts = 0,
            totalOvertimeEarned = BigDecimal.ZERO,
            totalNightDifferentialEarned = BigDecimal.ZERO,
            averageDailyEarned = BigDecimal.ZERO,
            averageHourlyRate = BigDecimal.ZERO
        )
    }
}

/**
 * 7-day bucket within a month (e.g. Days 1-7, 8-14, 15-21, 22-28, 29-31).
 */
data class MonthWeekBucket(
    val weekIndex: Int, // 1..5
    val label: String, // e.g. "1-7", "8-14", ...
    val totalEarned: BigDecimal,
    val totalDurationMs: Long,
    val shiftCount: Int
) {
    val totalHours: Double
        get() = totalDurationMs.toDouble() / 3_600_000.0
}

/**
 * Complete monthly analytics model.
 */
data class MonthlyAnalyticsData(
    val year: Int,
    val month: Int, // 1..12
    val monthName: String, // e.g. "Eylül 2026"
    val totalEarned: BigDecimal,
    val totalDurationMs: Long,
    val totalShifts: Int,
    val totalOvertimeEarned: BigDecimal,
    val totalNightDifferentialEarned: BigDecimal,
    val totalBreakDeductionMs: Long,
    val averageShiftEarned: BigDecimal,
    val averageHourlyRate: BigDecimal,
    val weeklyBuckets: List<MonthWeekBucket>
) {
    val totalHours: Double
        get() = totalDurationMs.toDouble() / 3_600_000.0

    val formattedTotalDuration: String
        get() {
            val totalMinutes = (totalDurationMs / 60000).coerceAtLeast(0L)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}s ${mins}dk"
        }

    val formattedBreakDeduction: String
        get() {
            val totalMinutes = (totalBreakDeductionMs / 60000).coerceAtLeast(0L)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}s ${mins}dk"
        }

    val maxBucketEarned: BigDecimal
        get() = weeklyBuckets.maxOfOrNull { it.totalEarned } ?: BigDecimal.ZERO

    companion object {
        val EMPTY = MonthlyAnalyticsData(
            year = 2026,
            month = 1,
            monthName = "",
            totalEarned = BigDecimal.ZERO,
            totalDurationMs = 0L,
            totalShifts = 0,
            totalOvertimeEarned = BigDecimal.ZERO,
            totalNightDifferentialEarned = BigDecimal.ZERO,
            totalBreakDeductionMs = 0L,
            averageShiftEarned = BigDecimal.ZERO,
            averageHourlyRate = BigDecimal.ZERO,
            weeklyBuckets = emptyList()
        )
    }
}

/**
 * Overall summary metrics across the entire shift history.
 */
data class OverallAnalyticsSummary(
    val totalShifts: Int,
    val totalEarned: BigDecimal,
    val totalDurationMs: Long,
    val totalOvertimeEarned: BigDecimal,
    val totalNightDifferentialEarned: BigDecimal,
    val averageShiftEarned: BigDecimal,
    val averageHourlyRate: BigDecimal
) {
    val formattedTotalDuration: String
        get() {
            val totalMinutes = (totalDurationMs / 60000).coerceAtLeast(0L)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}s ${mins}dk"
        }

    companion object {
        val EMPTY = OverallAnalyticsSummary(
            totalShifts = 0,
            totalEarned = BigDecimal.ZERO,
            totalDurationMs = 0L,
            totalOvertimeEarned = BigDecimal.ZERO,
            totalNightDifferentialEarned = BigDecimal.ZERO,
            averageShiftEarned = BigDecimal.ZERO,
            averageHourlyRate = BigDecimal.ZERO
        )
    }
}

/**
 * Pure domain engine for calculating deterministic weekly, monthly, and overall analytics.
 * Adheres strictly to the invariant:
 * Historical shifts preserve their frozen earnings, rates, and durations.
 * Never recalculates past shifts with current configuration.
 */
object ShiftAnalyticsEngine {

    private val WEEKDAY_NAMES_TR = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    /**
     * Aggregates shifts for the week containing [anchorEpochMillis].
     * Week begins on Monday 00:00:00.000 and ends on Sunday 23:59:59.999.
     */
    fun computeWeeklyAnalytics(
        history: List<CompletedShiftRecord>,
        anchorEpochMillis: Long
    ): WeeklyAnalyticsData {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = anchorEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        val mondayCal = cal.clone() as Calendar
        val mondayMillis = mondayCal.timeInMillis
        val weekOfYear = mondayCal.get(Calendar.WEEK_OF_YEAR)
        val year = mondayCal.get(Calendar.YEAR)

        val sundayCal = (mondayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val sundayMillis = sundayCal.timeInMillis

        val monthNameMon = mondayCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, turkishLocale) ?: ""
        val monthNameSun = sundayCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, turkishLocale) ?: ""
        val weekRangeFormatted = if (mondayCal.get(Calendar.MONTH) == sundayCal.get(Calendar.MONTH)) {
            "${mondayCal.get(Calendar.DAY_OF_MONTH)} — ${sundayCal.get(Calendar.DAY_OF_MONTH)} $monthNameMon $year"
        } else {
            "${mondayCal.get(Calendar.DAY_OF_MONTH)} $monthNameMon — ${sundayCal.get(Calendar.DAY_OF_MONTH)} $monthNameSun $year"
        }

        // Filter shifts that started in this week interval
        val weekShifts = history.filter { record ->
            record.startEpochMillis in mondayMillis..sundayMillis
        }

        // Build exactly 7 daily points
        val dailyPoints = mutableListOf<DailyAnalyticsPoint>()
        val dayIter = mondayCal.clone() as Calendar

        var totalEarned = BigDecimal.ZERO
        var totalDuration = 0L
        var totalOvertimeEarned = BigDecimal.ZERO
        var totalNightEarned = BigDecimal.ZERO
        var activeDaysWithEarnings = 0

        for (i in 0 until 7) {
            val startOfDay = dayIter.timeInMillis
            val dayCalendarDate = CalendarDate(
                year = dayIter.get(Calendar.YEAR),
                month = dayIter.get(Calendar.MONTH) + 1,
                dayOfMonth = dayIter.get(Calendar.DAY_OF_MONTH)
            )

            val endOfDayCal = (dayIter.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDay = endOfDayCal.timeInMillis

            val dayShifts = weekShifts.filter { it.startEpochMillis in startOfDay..endOfDay }

            var dayEarned = BigDecimal.ZERO
            var dayBase = BigDecimal.ZERO
            var dayOtEarned = BigDecimal.ZERO
            var dayNightEarned = BigDecimal.ZERO
            var dayDuration = 0L
            var dayOtDuration = 0L

            for (s in dayShifts) {
                dayEarned = dayEarned.add(s.totalEarned)
                dayBase = dayBase.add(s.baseEarned)
                dayOtEarned = dayOtEarned.add(s.overtimeEarned)
                dayNightEarned = dayNightEarned.add(s.nightDifferentialEarned)
                dayDuration += s.activeDurationMs
                dayOtDuration += s.overtimeDurationMs
            }

            if (dayShifts.isNotEmpty()) {
                activeDaysWithEarnings++
            }

            dailyPoints.add(
                DailyAnalyticsPoint(
                    date = dayCalendarDate,
                    dayOfWeekName = WEEKDAY_NAMES_TR[i],
                    shiftCount = dayShifts.size,
                    totalEarned = dayEarned,
                    baseEarned = dayBase,
                    overtimeEarned = dayOtEarned,
                    nightDifferentialEarned = dayNightEarned,
                    activeDurationMs = dayDuration,
                    overtimeDurationMs = dayOtDuration
                )
            )

            totalEarned = totalEarned.add(dayEarned)
            totalDuration += dayDuration
            totalOvertimeEarned = totalOvertimeEarned.add(dayOtEarned)
            totalNightEarned = totalNightEarned.add(dayNightEarned)

            dayIter.add(Calendar.DAY_OF_MONTH, 1)
        }

        val avgDailyEarned = if (activeDaysWithEarnings > 0) {
            totalEarned.divide(BigDecimal(activeDaysWithEarnings), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val avgHourlyRate = if (totalDuration > 0) {
            val hours = BigDecimal(totalDuration).divide(BigDecimal(3_600_000), 6, RoundingMode.HALF_UP)
            totalEarned.divide(hours, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return WeeklyAnalyticsData(
            year = year,
            weekOfYear = weekOfYear,
            weekRangeFormatted = weekRangeFormatted,
            dailyPoints = dailyPoints,
            totalEarned = totalEarned,
            totalDurationMs = totalDuration,
            totalShifts = weekShifts.size,
            totalOvertimeEarned = totalOvertimeEarned,
            totalNightDifferentialEarned = totalNightEarned,
            averageDailyEarned = avgDailyEarned,
            averageHourlyRate = avgHourlyRate
        )
    }

    /**
     * Aggregates shifts for the given [year] and [month] (1-indexed: 1..12).
     */
    fun computeMonthlyAnalytics(
        history: List<CompletedShiftRecord>,
        year: Int,
        month: Int
    ): MonthlyAnalyticsData {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthTitle = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, turkishLocale) ?: "$month"
        val formattedMonthName = "$monthTitle $year"

        val endOfMonthCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfMonth = endOfMonthCal.timeInMillis

        val monthShifts = history.filter { it.startEpochMillis in startOfMonth..endOfMonth }

        var totalEarned = BigDecimal.ZERO
        var totalDuration = 0L
        var totalOvertimeEarned = BigDecimal.ZERO
        var totalNightEarned = BigDecimal.ZERO
        var totalBreakDeduction = 0L

        for (s in monthShifts) {
            totalEarned = totalEarned.add(s.totalEarned)
            totalDuration += s.activeDurationMs
            totalOvertimeEarned = totalOvertimeEarned.add(s.overtimeEarned)
            totalNightEarned = totalNightEarned.add(s.nightDifferentialEarned)
            val deductedBreaks = s.breaks.filter { it.isDeductedFromSalary }
            totalBreakDeduction += deductedBreaks.sumOf { it.getDurationMs() }
        }

        val avgShiftEarned = if (monthShifts.isNotEmpty()) {
            totalEarned.divide(BigDecimal(monthShifts.size), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val avgHourlyRate = if (totalDuration > 0) {
            val hours = BigDecimal(totalDuration).divide(BigDecimal(3_600_000), 6, RoundingMode.HALF_UP)
            totalEarned.divide(hours, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Build weekly buckets (e.g. 1-7, 8-14, 15-21, 22-28, 29-31)
        val weeklyBuckets = mutableListOf<MonthWeekBucket>()
        val bucketRanges = listOf(
            1 to 7,
            8 to 14,
            15 to 21,
            22 to 28,
            29 to daysInMonth
        )

        bucketRanges.forEachIndexed { index, (startDay, endDay) ->
            if (startDay <= daysInMonth) {
                val effectiveEndDay = endDay.coerceAtMost(daysInMonth)
                val label = "$startDay-$effectiveEndDay"

                val bucketCalStart = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, startDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val bucketCalEnd = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, effectiveEndDay)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val bucketShifts = monthShifts.filter {
                    it.startEpochMillis in bucketCalStart.timeInMillis..bucketCalEnd.timeInMillis
                }

                var bucketEarned = BigDecimal.ZERO
                var bucketDuration = 0L
                for (bs in bucketShifts) {
                    bucketEarned = bucketEarned.add(bs.totalEarned)
                    bucketDuration += bs.activeDurationMs
                }

                weeklyBuckets.add(
                    MonthWeekBucket(
                        weekIndex = index + 1,
                        label = label,
                        totalEarned = bucketEarned,
                        totalDurationMs = bucketDuration,
                        shiftCount = bucketShifts.size
                    )
                )
            }
        }

        return MonthlyAnalyticsData(
            year = year,
            month = month,
            monthName = formattedMonthName,
            totalEarned = totalEarned,
            totalDurationMs = totalDuration,
            totalShifts = monthShifts.size,
            totalOvertimeEarned = totalOvertimeEarned,
            totalNightDifferentialEarned = totalNightEarned,
            totalBreakDeductionMs = totalBreakDeduction,
            averageShiftEarned = avgShiftEarned,
            averageHourlyRate = avgHourlyRate,
            weeklyBuckets = weeklyBuckets
        )
    }

    /**
     * Aggregates total metrics across all completed shifts.
     */
    fun computeOverallSummary(history: List<CompletedShiftRecord>): OverallAnalyticsSummary {
        val count = history.size
        var totalEarned = BigDecimal.ZERO
        var totalDuration = 0L
        var totalOtEarned = BigDecimal.ZERO
        var totalNightEarned = BigDecimal.ZERO

        for (s in history) {
            totalEarned = totalEarned.add(s.totalEarned)
            totalDuration += s.activeDurationMs
            totalOtEarned = totalOtEarned.add(s.overtimeEarned)
            totalNightEarned = totalNightEarned.add(s.nightDifferentialEarned)
        }

        val avgShift = if (count > 0) {
            totalEarned.divide(BigDecimal(count), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val avgHourly = if (totalDuration > 0) {
            val hours = BigDecimal(totalDuration).divide(BigDecimal(3_600_000), 6, RoundingMode.HALF_UP)
            totalEarned.divide(hours, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return OverallAnalyticsSummary(
            totalShifts = count,
            totalEarned = totalEarned,
            totalDurationMs = totalDuration,
            totalOvertimeEarned = totalOtEarned,
            totalNightDifferentialEarned = totalNightEarned,
            averageShiftEarned = avgShift,
            averageHourlyRate = avgHourly
        )
    }
}
