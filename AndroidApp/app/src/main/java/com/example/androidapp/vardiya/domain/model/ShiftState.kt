package com.example.androidapp.vardiya.domain.model

/**
 * State machine stages for a work shift.
 */
enum class ShiftState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    FINISHED
}
