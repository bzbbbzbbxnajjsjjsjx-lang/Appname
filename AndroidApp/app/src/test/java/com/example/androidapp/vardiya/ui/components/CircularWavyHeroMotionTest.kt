package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.unit.dp
import com.example.androidapp.theme.DarkColorScheme
import com.example.androidapp.theme.LightColorScheme
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.vardiya.domain.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying the hero motion system and mapping contracts:
 * - Phase engine activity per state (NOT_STARTED, RUNNING, PAUSED, BREAK, FINISHED)
 * - Target amplitude per state matching production geometry
 * - Wave speed (cycle duration) in normal vs overtime
 * - Color transitions preserving semantic color contract
 * - Accessibility / Reduced-motion compliance
 */
class CircularWavyHeroMotionTest {

    // ========================================================================
    // 1. PHASE ENGINE LIFECYCLE TESTS
    // ========================================================================

    @Test
    fun testPhaseEngine_notStarted_hasNoActivePhase() {
        assertFalse(
            "Phase animation must NOT run when shift is NOT_STARTED",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.NOT_STARTED,
                isBreakActive = false,
                motionPreference = MotionPreference.NORMAL
            )
        )
    }

    @Test
    fun testPhaseEngine_running_hasActivePhase() {
        assertTrue(
            "Phase animation MUST run when shift is RUNNING and not in break",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.RUNNING,
                isBreakActive = false,
                motionPreference = MotionPreference.NORMAL
            )
        )
    }

    @Test
    fun testPhaseEngine_paused_stopsPhase() {
        assertFalse(
            "Phase animation must freeze/stop when shift is PAUSED",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.PAUSED,
                isBreakActive = false,
                motionPreference = MotionPreference.NORMAL
            )
        )
    }

    @Test
    fun testPhaseEngine_break_stopsPhase() {
        assertFalse(
            "Phase animation must freeze/stay calm when on BREAK even if state is RUNNING",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.RUNNING,
                isBreakActive = true,
                motionPreference = MotionPreference.NORMAL
            )
        )
    }

    @Test
    fun testPhaseEngine_finished_stopsPhase() {
        assertFalse(
            "Phase animation must stop when shift is FINISHED",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.FINISHED,
                isBreakActive = false,
                motionPreference = MotionPreference.NORMAL
            )
        )
    }

    // ========================================================================
    // 2. AMPLITUDE TARGET TESTS (MATCHING PRODUCTION GEOMETRY)
    // ========================================================================

    @Test
    fun testTargetAmplitude_matchesProductionGeometry() {
        // Overtime: 7.0.dp
        assertEquals(
            7.0.dp,
            CircularWavyHeroMotion.resolveTargetAmplitude(
                shiftState = ShiftState.RUNNING,
                isOvertimeActive = true,
                isBreakActive = false
            )
        )

        // Normal Running: 6.0.dp
        assertEquals(
            6.0.dp,
            CircularWavyHeroMotion.resolveTargetAmplitude(
                shiftState = ShiftState.RUNNING,
                isOvertimeActive = false,
                isBreakActive = false
            )
        )

        // Break: 5.0.dp
        assertEquals(
            5.0.dp,
            CircularWavyHeroMotion.resolveTargetAmplitude(
                shiftState = ShiftState.RUNNING,
                isOvertimeActive = false,
                isBreakActive = true
            )
        )

        // Paused: 4.5.dp
        assertEquals(
            4.5.dp,
            CircularWavyHeroMotion.resolveTargetAmplitude(
                shiftState = ShiftState.PAUSED,
                isOvertimeActive = false,
                isBreakActive = false
            )
        )

        // Finished: 5.0.dp
        assertEquals(
            5.0.dp,
            CircularWavyHeroMotion.resolveTargetAmplitude(
                shiftState = ShiftState.FINISHED,
                isOvertimeActive = false,
                isBreakActive = false
            )
        )

        // Not Started: 5.0.dp
        assertEquals(
            5.0.dp,
            CircularWavyHeroMotion.resolveTargetAmplitude(
                shiftState = ShiftState.NOT_STARTED,
                isOvertimeActive = false,
                isBreakActive = false
            )
        )
    }

    // ========================================================================
    // 3. WAVE SPEED (CYCLE DURATION) TESTS
    // ========================================================================

    @Test
    fun testWaveCycleDuration_normalVsOvertime() {
        assertEquals(2400L, CircularWavyHeroMotion.resolveWaveCycleDurationMs(isOvertimeActive = false))
        assertEquals(1800L, CircularWavyHeroMotion.resolveWaveCycleDurationMs(isOvertimeActive = true))
    }

    // ========================================================================
    // 4. COLOR MAPPING CONTRACT TESTS
    // ========================================================================

    @Test
    fun testHeroActiveColor_lightThemeMapping() {
        val scheme = LightColorScheme

        assertEquals(
            scheme.primary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.RUNNING,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.tertiary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.PAUSED,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.tertiary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.RUNNING,
                isOvertimeActive = false,
                isBreakActive = true,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.tertiary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.RUNNING,
                isOvertimeActive = true,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.secondary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.FINISHED,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.primary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.NOT_STARTED,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
    }

    @Test
    fun testHeroActiveColor_darkThemeMapping() {
        val scheme = DarkColorScheme

        assertEquals(
            scheme.primary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.RUNNING,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.tertiary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.PAUSED,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.tertiary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.RUNNING,
                isOvertimeActive = false,
                isBreakActive = true,
                colorScheme = scheme
            )
        )
        assertEquals(
            scheme.secondary,
            CircularWavyHeroMotion.resolveHeroActiveColor(
                ShiftState.FINISHED,
                isOvertimeActive = false,
                isBreakActive = false,
                colorScheme = scheme
            )
        )
    }

    // ========================================================================
    // 5. REDUCED MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testReducedMotion_disablesPhaseInAllStates() {
        assertFalse(
            "Phase must be disabled in RUNNING under reduced motion",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.RUNNING,
                isBreakActive = false,
                motionPreference = MotionPreference.REDUCED
            )
        )
        assertFalse(
            "Phase must be disabled in OVERTIME under reduced motion",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.RUNNING,
                isBreakActive = false,
                motionPreference = MotionPreference.REDUCED
            )
        )
        assertFalse(
            "Phase must be disabled in NOT_STARTED under reduced motion",
            CircularWavyHeroMotion.isWavePhaseActive(
                shiftState = ShiftState.NOT_STARTED,
                isBreakActive = false,
                motionPreference = MotionPreference.REDUCED
            )
        )
    }

    @Test
    fun testReducedMotion_motionSchemeReturnsSnapSpecs() {
        val reducedScheme = VardiyaMotionScheme.reducedMotion()
        assertTrue("Spatial spec must be SnapSpec", reducedScheme.defaultSpatialSpec<Float>() is SnapSpec<Float>)
        assertTrue("Effects spec must be SnapSpec", reducedScheme.defaultEffectsSpec<Float>() is SnapSpec<Float>)

        val expressiveScheme = VardiyaMotionScheme.expressive()
        assertTrue("Expressive spatial spec must be SpringSpec", expressiveScheme.defaultSpatialSpec<Float>() is SpringSpec<Float>)
        assertTrue("Expressive effects spec must be SpringSpec", expressiveScheme.defaultEffectsSpec<Float>() is SpringSpec<Float>)
    }
}
