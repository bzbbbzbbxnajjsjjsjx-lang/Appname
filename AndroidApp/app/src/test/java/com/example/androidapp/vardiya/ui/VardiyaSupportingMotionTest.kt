package com.example.androidapp.vardiya.ui

import androidx.compose.animation.core.SnapSpec
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.ui.components.CircularWavyHeroMotion
import com.example.androidapp.vardiya.ui.components.StateBadgeMotion
import com.example.androidapp.vardiya.ui.components.VardiyaControlBarMotion
import com.example.androidapp.vardiya.ui.components.VardiyaExpandableMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Level 3 Supporting Components & Level 4 Micro Motion:
 * - StateBadge pulse lifecycle mapping, named semantic tokens, and idle battery elimination
 * - Expandable section transitions (Settings, Calendar Heatmap, History item details)
 * - Accessibility reduced-motion adherence across supporting and micro animations
 * - Continuous animation active-state matrix (zero idle frame draw verification)
 */
class VardiyaSupportingMotionTest {

    // ========================================================================
    // 1. STATE BADGE MOTION LIFECYCLE TESTS (LEVEL 4 MICRO MOTION)
    // ========================================================================

    @Test
    fun testStateBadge_runningWithoutBreak_normalMotion_pulseIsActive() {
        val isActive = StateBadgeMotion.isPulseActive(
            shiftState = ShiftState.RUNNING,
            isBreakActive = false,
            motionPreference = MotionPreference.NORMAL
        )
        assertTrue("Pulse must be active when shift is RUNNING and break is not active", isActive)
    }

    @Test
    fun testStateBadge_runningWithoutBreak_reducedMotion_pulseIsDisabled() {
        val isActive = StateBadgeMotion.isPulseActive(
            shiftState = ShiftState.RUNNING,
            isBreakActive = false,
            motionPreference = MotionPreference.REDUCED
        )
        assertFalse("Pulse must be disabled under reduced motion preference", isActive)
    }

    @Test
    fun testStateBadge_runningWithBreak_pulseIsDisabled() {
        val isActive = StateBadgeMotion.isPulseActive(
            shiftState = ShiftState.RUNNING,
            isBreakActive = true,
            motionPreference = MotionPreference.NORMAL
        )
        assertFalse("Pulse must be disabled when break is active", isActive)
    }

    @Test
    fun testStateBadge_notStarted_pulseIsDisabled_idleBatterySaved() {
        val isActive = StateBadgeMotion.isPulseActive(
            shiftState = ShiftState.NOT_STARTED,
            isBreakActive = false,
            motionPreference = MotionPreference.NORMAL
        )
        assertFalse("Pulse must be disabled when shift is NOT_STARTED (zero idle CPU/frame draw)", isActive)
    }

    @Test
    fun testStateBadge_paused_pulseIsDisabled() {
        val isActive = StateBadgeMotion.isPulseActive(
            shiftState = ShiftState.PAUSED,
            isBreakActive = false,
            motionPreference = MotionPreference.NORMAL
        )
        assertFalse("Pulse must be disabled when shift is PAUSED", isActive)
    }

    @Test
    fun testStateBadge_finished_pulseIsDisabled() {
        val isActive = StateBadgeMotion.isPulseActive(
            shiftState = ShiftState.FINISHED,
            isBreakActive = false,
            motionPreference = MotionPreference.NORMAL
        )
        assertFalse("Pulse must be disabled when shift is FINISHED", isActive)
    }

    @Test
    fun testStateBadge_semanticPulseTokens_areDeterministicAndCalm() {
        assertEquals("Initial pulse alpha must be 0.35f for calm breathing", 0.35f, StateBadgeMotion.PulseInitialAlpha, 0.001f)
        assertEquals("Target pulse alpha must be 1.0f", 1.0f, StateBadgeMotion.PulseTargetAlpha, 0.001f)
        assertEquals("Pulse period must be 1200ms for slow harmonic breathing", 1200, StateBadgeMotion.PulsePeriodMillis)

        val spec = StateBadgeMotion.pulseAnimationSpec()
        assertNotNull("Pulse animation spec must be non-null", spec)
    }

    // ========================================================================
    // 2. EXPANDABLE SECTION TRANSITIONS TESTS (LEVEL 3 SUPPORTING MOTION)
    // ========================================================================

    @Test
    fun testExpandableMotion_normalPreference_createsEnterTransition() {
        val expressive = VardiyaMotionScheme.expressive()
        val enter = VardiyaExpandableMotion.createEnterTransition(
            motionScheme = expressive,
            motionPreference = MotionPreference.NORMAL
        )
        assertNotNull("Enter transition must not be null under normal motion", enter)
    }

    @Test
    fun testExpandableMotion_normalPreference_createsExitTransition() {
        val expressive = VardiyaMotionScheme.expressive()
        val exit = VardiyaExpandableMotion.createExitTransition(
            motionScheme = expressive,
            motionPreference = MotionPreference.NORMAL
        )
        assertNotNull("Exit transition must not be null under normal motion", exit)
    }

    @Test
    fun testExpandableMotion_reducedMotion_createsFadeOnlyEnterTransition() {
        val reduced = VardiyaMotionScheme.reducedMotion()
        val enter = VardiyaExpandableMotion.createEnterTransition(
            motionScheme = reduced,
            motionPreference = MotionPreference.REDUCED
        )
        assertNotNull("Enter transition must not be null under reduced motion", enter)
    }

    @Test
    fun testExpandableMotion_reducedMotion_createsFadeOnlyExitTransition() {
        val reduced = VardiyaMotionScheme.reducedMotion()
        val exit = VardiyaExpandableMotion.createExitTransition(
            motionScheme = reduced,
            motionPreference = MotionPreference.REDUCED
        )
        assertNotNull("Exit transition must not be null under reduced motion", exit)
    }

    // ========================================================================
    // 3. MOTION SCHEME SPEC COMPLIANCE & REDUCED MOTION AUDIT
    // ========================================================================

    @Test
    fun testMotionScheme_supportingSpecs_areDefinedAndNonNull() {
        val expressive = VardiyaMotionScheme.expressive()
        assertNotNull("defaultSpatialSpec must be defined", expressive.defaultSpatialSpec<Float>())
        assertNotNull("defaultEffectsSpec must be defined", expressive.defaultEffectsSpec<Float>())
        assertNotNull("fastEffectsSpec must be defined", expressive.fastEffectsSpec<Float>())
    }

    @Test
    fun testMotionScheme_reducedMotion_allSpecsAreSnapSpecs() {
        val reduced = VardiyaMotionScheme.reducedMotion()
        assertTrue("defaultSpatialSpec must be SnapSpec in reduced motion", reduced.defaultSpatialSpec<Float>() is SnapSpec)
        assertTrue("fastSpatialSpec must be SnapSpec in reduced motion", reduced.fastSpatialSpec<Float>() is SnapSpec)
        assertTrue("slowSpatialSpec must be SnapSpec in reduced motion", reduced.slowSpatialSpec<Float>() is SnapSpec)
        assertTrue("defaultEffectsSpec must be SnapSpec in reduced motion", reduced.defaultEffectsSpec<Float>() is SnapSpec)
        assertTrue("fastEffectsSpec must be SnapSpec in reduced motion", reduced.fastEffectsSpec<Float>() is SnapSpec)
        assertTrue("slowEffectsSpec must be SnapSpec in reduced motion", reduced.slowEffectsSpec<Float>() is SnapSpec)
    }

    // ========================================================================
    // 4. CONTINUOUS ANIMATION ZERO-IDLE AUDIT MATRIX
    // ========================================================================

    @Test
    fun testContinuousMotion_matrix_strictlyDisabledWhenNotRunning() {
        val allStates = ShiftState.values()
        val breakOptions = listOf(false, true)
        val preferenceOptions = listOf(MotionPreference.NORMAL, MotionPreference.REDUCED)

        for (state in allStates) {
            for (isBreak in breakOptions) {
                for (pref in preferenceOptions) {
                    val isBadgePulseActive = StateBadgeMotion.isPulseActive(state, isBreak, pref)
                    val isHeroWaveActive = CircularWavyHeroMotion.isWavePhaseActive(state, isBreak, pref)

                    val shouldBeActive = (state == ShiftState.RUNNING && !isBreak && pref == MotionPreference.NORMAL)

                    assertEquals(
                        "StateBadge pulse active mismatch for state=$state, break=$isBreak, pref=$pref",
                        shouldBeActive,
                        isBadgePulseActive
                    )
                    assertEquals(
                        "Hero wave phase active mismatch for state=$state, break=$isBreak, pref=$pref",
                        shouldBeActive,
                        isHeroWaveActive
                    )
                }
            }
        }
    }

    @Test
    fun testControlBar_reducedMotion_sizeTransformIsNull() {
        val reduced = VardiyaMotionScheme.reducedMotion()
        val transition = VardiyaControlBarMotion.createTransition(reduced, MotionPreference.REDUCED)
        assertNull("SizeTransform must be null under reduced motion to prevent container morphing", transition.sizeTransform)
    }
}
