package com.example.androidapp.vardiya.domain.time

import android.os.SystemClock

/**
 * Interface abstracting system time retrieval to guarantee deterministic testing.
 */
interface TimeProvider {
    fun currentEpochMillis(): Long
    fun elapsedRealtimeMillis(): Long
}

/**
 * Android system implementation using wall-clock and monotonic clocks.
 */
class DefaultTimeProvider : TimeProvider {
    override fun currentEpochMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
