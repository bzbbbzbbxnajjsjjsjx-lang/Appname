package com.example.androidapp.vardiya.domain.model

/**
 * State machine stages for a work shift.
 */
enum class ShiftState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    FINISHED;

    /**
     * Strictly verifies valid transitions:
     * - NOT_STARTED -> RUNNING
     * - RUNNING -> PAUSED, FINISHED
     * - PAUSED -> RUNNING, FINISHED
     * - FINISHED -> NOT_STARTED (via reset)
     * All other transitions are invalid and rejected.
     */
    fun canTransitionTo(next: ShiftState): Boolean = when (this) {
        NOT_STARTED -> next == RUNNING
        RUNNING -> next == PAUSED || next == FINISHED
        PAUSED -> next == RUNNING || next == FINISHED
        FINISHED -> next == NOT_STARTED
    }
}
