package com.example.androidapp.vardiya.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftStateMachineTest {

    @Test
    fun testValidTransitions() {
        // NOT_STARTED -> RUNNING
        assertTrue(ShiftState.NOT_STARTED.canTransitionTo(ShiftState.RUNNING))

        // RUNNING -> PAUSED
        assertTrue(ShiftState.RUNNING.canTransitionTo(ShiftState.PAUSED))

        // RUNNING -> FINISHED
        assertTrue(ShiftState.RUNNING.canTransitionTo(ShiftState.FINISHED))

        // PAUSED -> RUNNING
        assertTrue(ShiftState.PAUSED.canTransitionTo(ShiftState.RUNNING))

        // PAUSED -> FINISHED
        assertTrue(ShiftState.PAUSED.canTransitionTo(ShiftState.FINISHED))

        // FINISHED -> NOT_STARTED (via reset)
        assertTrue(ShiftState.FINISHED.canTransitionTo(ShiftState.NOT_STARTED))
    }

    @Test
    fun testInvalidTransitionsFromNotStarted() {
        assertFalse(ShiftState.NOT_STARTED.canTransitionTo(ShiftState.NOT_STARTED))
        assertFalse(ShiftState.NOT_STARTED.canTransitionTo(ShiftState.PAUSED))
        assertFalse(ShiftState.NOT_STARTED.canTransitionTo(ShiftState.FINISHED))
    }

    @Test
    fun testInvalidTransitionsFromRunning() {
        assertFalse(ShiftState.RUNNING.canTransitionTo(ShiftState.NOT_STARTED))
        assertFalse(ShiftState.RUNNING.canTransitionTo(ShiftState.RUNNING))
    }

    @Test
    fun testInvalidTransitionsFromPaused() {
        assertFalse(ShiftState.PAUSED.canTransitionTo(ShiftState.NOT_STARTED))
        assertFalse(ShiftState.PAUSED.canTransitionTo(ShiftState.PAUSED))
    }

    @Test
    fun testInvalidTransitionsFromFinished() {
        assertFalse(ShiftState.FINISHED.canTransitionTo(ShiftState.RUNNING))
        assertFalse(ShiftState.FINISHED.canTransitionTo(ShiftState.PAUSED))
        assertFalse(ShiftState.FINISHED.canTransitionTo(ShiftState.FINISHED))
    }
}
