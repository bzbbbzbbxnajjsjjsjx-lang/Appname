package com.example.androidapp.vardiya.domain.model

import java.util.UUID

/**
 * Domain entity representing a rest break taken during a work shift.
 * Tracks start and end timestamps and whether this break is deducted from salary.
 */
data class BreakRecord(
    val id: String = UUID.randomUUID().toString(),
    val startEpochMillis: Long,
    val endEpochMillis: Long? = null,
    val isDeductedFromSalary: Boolean = false,
    val note: String? = null
) {
    val isOngoing: Boolean
        get() = endEpochMillis == null

    /**
     * Resolves the elapsed duration of this break in milliseconds.
     * If ongoing, calculates relative to [nowEpoch].
     */
    fun getDurationMs(nowEpoch: Long = System.currentTimeMillis()): Long {
        val end = endEpochMillis ?: nowEpoch
        return (end - startEpochMillis).coerceAtLeast(0L)
    }
}
