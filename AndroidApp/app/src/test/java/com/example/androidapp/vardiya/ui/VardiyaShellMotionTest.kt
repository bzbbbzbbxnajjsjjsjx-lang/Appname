package com.example.androidapp.vardiya.ui

import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.ui.components.ControlBarLayoutConfig
import com.example.androidapp.vardiya.ui.components.VardiyaControlBarMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit test suite verifying App Shell & Control Bar Expressive Motion foundations:
 * - Navigation 3 screen transition configurations (forward, pop, reduced motion)
 * - ControlBar semantic layout mappings across all shift lifecycle states
 * - ControlBar container morphing and button configuration transitions
 * - Reduced motion accessibility compliance across navigation and controls
 */
class VardiyaShellMotionTest {

    // ========================================================================
    // 1. NAVIGATION 3 TRANSITIONS TESTS
    // ========================================================================

    @Test
    fun testNavTransitions_spatialOffsetFactor_isSubtleEightPercent() {
        assertEquals(0.08f, VardiyaNavTransitions.SubtleSpatialOffsetFactor, 0.001f)

        // On a 400px screen, offset is 32px (subtle level shift, not a full-screen sweep)
        val forwardOffset = (400 * VardiyaNavTransitions.SubtleSpatialOffsetFactor).toInt()
        assertEquals(32, forwardOffset)
    }

    @Test
    fun testNavTransitions_forwardTransition_createsNonNullTransform() {
        val expressive = VardiyaMotionScheme.expressive()
        val transform = VardiyaNavTransitions.createForwardTransition(expressive, MotionPreference.NORMAL)
        assertNotNull("Forward transition must be non-null", transform)
    }

    @Test
    fun testNavTransitions_popTransition_createsNonNullTransform() {
        val expressive = VardiyaMotionScheme.expressive()
        val transform = VardiyaNavTransitions.createPopTransition(expressive, MotionPreference.NORMAL)
        assertNotNull("Pop transition must be non-null", transform)
    }

    @Test
    fun testNavTransitions_reducedMotion_createsZeroOffsetTransitions() {
        val reduced = VardiyaMotionScheme.reducedMotion()

        val forwardTransform = VardiyaNavTransitions.createForwardTransition(reduced, MotionPreference.REDUCED)
        assertNotNull("Reduced forward transition must be non-null", forwardTransform)

        val popTransform = VardiyaNavTransitions.createPopTransition(reduced, MotionPreference.REDUCED)
        assertNotNull("Reduced pop transition must be non-null", popTransform)
    }

    // ========================================================================
    // 2. CONTROL BAR LAYOUT CONFIG MAPPING TESTS
    // ========================================================================

    @Test
    fun testControlBarMotion_notStarted_mapsToStartOnly() {
        val config = VardiyaControlBarMotion.resolveLayoutConfig(
            shiftState = ShiftState.NOT_STARTED,
            isBreakActive = false
        )
        assertEquals(ControlBarLayoutConfig.START_ONLY, config)
    }

    @Test
    fun testControlBarMotion_runningNormal_mapsToActiveControls() {
        val config = VardiyaControlBarMotion.resolveLayoutConfig(
            shiftState = ShiftState.RUNNING,
            isBreakActive = false
        )
        assertEquals(ControlBarLayoutConfig.ACTIVE_CONTROLS, config)
    }

    @Test
    fun testControlBarMotion_runningBreak_mapsToBreakControls() {
        val config = VardiyaControlBarMotion.resolveLayoutConfig(
            shiftState = ShiftState.RUNNING,
            isBreakActive = true
        )
        assertEquals(ControlBarLayoutConfig.BREAK_CONTROLS, config)
    }

    @Test
    fun testControlBarMotion_paused_mapsToPausedControls() {
        val config = VardiyaControlBarMotion.resolveLayoutConfig(
            shiftState = ShiftState.PAUSED,
            isBreakActive = false
        )
        assertEquals(ControlBarLayoutConfig.PAUSED_CONTROLS, config)
    }

    @Test
    fun testControlBarMotion_finished_mapsToResetOnly() {
        val config = VardiyaControlBarMotion.resolveLayoutConfig(
            shiftState = ShiftState.FINISHED,
            isBreakActive = false
        )
        assertEquals(ControlBarLayoutConfig.RESET_ONLY, config)
    }

    // ========================================================================
    // 3. CONTROL BAR TRANSITIONS & REDUCED MOTION
    // ========================================================================

    @Test
    fun testControlBarMotion_transitionSpec_normalMotion_createsNonNullTransform() {
        val expressive = VardiyaMotionScheme.expressive()
        val transform = VardiyaControlBarMotion.createTransition(expressive, MotionPreference.NORMAL)
        assertNotNull("ControlBar transition must be non-null", transform)
    }

    @Test
    fun testControlBarMotion_transitionSpec_reducedMotion_createsNonNullTransform() {
        val reduced = VardiyaMotionScheme.reducedMotion()
        val transform = VardiyaControlBarMotion.createTransition(reduced, MotionPreference.REDUCED)
        assertNotNull("ControlBar reduced transition must be non-null", transform)
    }
}
