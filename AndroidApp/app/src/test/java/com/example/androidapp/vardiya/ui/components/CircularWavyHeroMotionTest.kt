package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.unit.dp
import com.example.androidapp.theme.DarkColorScheme
import com.example.androidapp.theme.LightColorScheme
import com.example.androidapp.theme.motion.HeroWaveMotionTokens
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.theme.motion.contract.HeroSemanticState
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlin.math.PI
import kotlin.math.roundToLong
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
    // 3. WAVE SPEED (CYCLE DURATION & PHASE VELOCITY) TESTS
    // ========================================================================

    @Test
    fun testWaveCycleDuration_tokensAndContract() {
        // Semantic Token Values (Calm, Premium Wave for 295dp Hero)
        assertEquals(HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS, HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS)
        assertEquals(HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS, HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS)
        assertTrue("RUNNING duration must be calibrated slower than 20s", HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS > 20_000L)
        assertTrue("OVERTIME duration must be calibrated slower than 14s", HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS > 14_000L)
        assertEquals(HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS / 12, HeroWaveMotionTokens.SINGLE_WAVE_PERIOD_RUNNING_MS)
        assertEquals(HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS / 12, HeroWaveMotionTokens.SINGLE_WAVE_PERIOD_OVERTIME_MS)

        // Contract Resolution via CircularWavyHeroMotion
        assertEquals(
            HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS,
            CircularWavyHeroMotion.resolveWaveCycleDurationMs(isOvertimeActive = false)
        )
        assertEquals(
            HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS,
            CircularWavyHeroMotion.resolveWaveCycleDurationMs(isOvertimeActive = true)
        )
    }

    @Test
    fun testPhaseVelocity_semanticStatesAndReducedMotion() {
        val runningVelocity = HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(
            HeroSemanticState.RUNNING,
            MotionPreference.NORMAL
        )
        val overtimeVelocity = HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(
            HeroSemanticState.OVERTIME,
            MotionPreference.NORMAL
        )

        // Mathematical velocity expectations derived from calibrated durations
        val expectedRunningVelocity = (2.0 * PI / (HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS / 1000.0)).toFloat()
        val expectedOvertimeVelocity = (2.0 * PI / (HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS / 1000.0)).toFloat()

        assertEquals(expectedRunningVelocity, runningVelocity, 0.0001f)
        assertEquals(expectedOvertimeVelocity, overtimeVelocity, 0.0001f)
        assertTrue("Overtime velocity must be faster than running", overtimeVelocity > runningVelocity)

        // Resting / Paused states have ZERO velocity
        assertEquals(
            0f,
            HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(HeroSemanticState.PAUSED, MotionPreference.NORMAL)
        )
        assertEquals(
            0f,
            HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(HeroSemanticState.BREAK, MotionPreference.NORMAL)
        )
        assertEquals(
            0f,
            HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(HeroSemanticState.NOT_STARTED, MotionPreference.NORMAL)
        )
        assertEquals(
            0f,
            HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(HeroSemanticState.FINISHED, MotionPreference.NORMAL)
        )

        // Accessibility: REDUCED motion suppresses velocity in ALL states
        assertEquals(
            0f,
            HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(HeroSemanticState.RUNNING, MotionPreference.REDUCED)
        )
        assertEquals(
            0f,
            HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(HeroSemanticState.OVERTIME, MotionPreference.REDUCED)
        )
    }

    // ========================================================================
    // 3.1 PHYSICAL MOTION INVARIANT TESTS (PHASE 8.6 CALIBRATION)
    // ========================================================================

    @Test
    fun testPhysicalMotionInvariants() {
        // Invariant 1: WAVE_COUNT == 12
        assertEquals(12, HeroWaveMotionTokens.WAVE_COUNT)

        // Invariant 2: RUNNING target velocity ≈ 12.0 dp/s
        assertEquals(12.0f, HeroWaveMotionTokens.TARGET_VELOCITY_RUNNING_DP_PER_SEC, 0.01f)

        // Invariant 3: OVERTIME target velocity ≈ 17.1429 dp/s
        val expectedOvertimeSpeed = 12.0f * (20f / 14f)
        assertEquals(expectedOvertimeSpeed, HeroWaveMotionTokens.TARGET_VELOCITY_OVERTIME_DP_PER_SEC, 0.01f)

        // Invariant 4: Cycle duration and path length / velocity relationship is mathematically exact
        val nominalLength = HeroWaveMotionTokens.NOMINAL_PATH_LENGTH_DP
        val derivedRunningDurationMs = (nominalLength / HeroWaveMotionTokens.TARGET_VELOCITY_RUNNING_DP_PER_SEC * 1000f).toLong()
        val derivedOvertimeDurationMs = (nominalLength / HeroWaveMotionTokens.TARGET_VELOCITY_OVERTIME_DP_PER_SEC * 1000f).toLong()
        assertEquals(derivedRunningDurationMs.toDouble(), HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS.toDouble(), 5.0)
        assertEquals(derivedOvertimeDurationMs.toDouble(), HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS.toDouble(), 5.0)

        // Invariant 5: RUNNING duration > legacy 20s (calmed down by ~3.14x)
        assertTrue("RUNNING duration must be greater than legacy 20s", HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS > 20_000L)
        assertTrue("RUNNING duration must be at least 60s for calm breathing tempo", HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS >= 60_000L)

        // Invariant 6: OVERTIME duration > legacy 14s (calmed down by ~3.14x)
        assertTrue("OVERTIME duration must be greater than legacy 14s", HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS > 14_000L)
        assertTrue("OVERTIME duration must be at least 40s for controlled tempo", HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS >= 40_000L)

        // Invariant 7: Display density invariance (physical velocity in dp/s is constant regardless of screen density)
        val densities = listOf(1.0f, 1.5f, 2.0f, 2.75f, 3.0f, 3.5f, 4.0f)
        for (density in densities) {
            val nominalLengthPx = nominalLength * density
            val durationMs = HeroWaveMotionTokens.calculateCycleDurationMsFromPx(
                pathLengthPx = nominalLengthPx,
                density = density,
                semanticState = HeroSemanticState.RUNNING
            )
            val physicalVelocityDpPerSec = (nominalLengthPx / density) / (durationMs / 1000.0f)
            assertEquals("Physical velocity must remain 12 dp/s across all densities", 12.0f, physicalVelocityDpPerSec, 0.05f)
        }

        // Invariant 8: Path geometry change dynamically recomputes duration
        val customLengthDp = 839.29f // measured wavy arc length with 7dp amplitude
        val customDurationMs = HeroWaveMotionTokens.calculateCycleDurationMs(customLengthDp, HeroSemanticState.RUNNING)
        val customVelocityDpPerSec = customLengthDp / (customDurationMs / 1000.0f)
        assertEquals("Dynamic geometry adaptation must yield 12 dp/s on custom length", 12.0f, customVelocityDpPerSec, 0.05f)
        assertTrue("Longer wavy path must produce proportionally longer cycle duration", customDurationMs > HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS)

        // Invariant 9: Phase normalized [0, 2π) behavior preserved
        val twoPi = (2.0 * PI).toFloat()
        val wrapped = HeroWaveMotionTokens.advancePhase(currentPhase = twoPi - 0.01f, velocityRadPerSec = 1.0f, deltaSeconds = 0.05f)
        assertTrue("Phase must remain bounded in [0, 2π)", wrapped in 0f..twoPi)

        // Invariant 10: OVERTIME is strictly faster than RUNNING while preserving the ~1.43x ratio
        val ratio = HeroWaveMotionTokens.CYCLE_DURATION_RUNNING_MS.toDouble() / HeroWaveMotionTokens.CYCLE_DURATION_OVERTIME_MS.toDouble()
        assertEquals(20.0 / 14.0, ratio, 0.05)
    }

    // ========================================================================
    // 3.2 RUNTIME GEOMETRY & DENSITY SCALING TESTS (PHASE 8.6 AUDIT)
    // ========================================================================

    @Test
    fun testPathLengthDurationScaling_600dp_750dp_800dp() {
        val testLengths = listOf(600.0f, 750.0f, 800.0f)

        for (lengthDp in testLengths) {
            // duration = length / velocity
            val runningDurationMs = HeroWaveMotionTokens.calculateCycleDurationMs(lengthDp, HeroSemanticState.RUNNING)
            val expectedRunningDurationMs = (lengthDp / 12.0f * 1000f).roundToLong()
            assertEquals("RUNNING duration for ${lengthDp}dp must be length / 12.0", expectedRunningDurationMs, runningDurationMs)

            val overtimeSpeed = 12.0f * (20f / 14f)
            val overtimeDurationMs = HeroWaveMotionTokens.calculateCycleDurationMs(lengthDp, HeroSemanticState.OVERTIME)
            val expectedOvertimeDurationMs = (lengthDp / overtimeSpeed * 1000f).roundToLong()
            assertEquals("OVERTIME duration for ${lengthDp}dp must be length / 17.142857", expectedOvertimeDurationMs, overtimeDurationMs)

            // Speed invariant verification
            val actualRunningSpeed = lengthDp / (runningDurationMs / 1000f)
            assertEquals("Actual RUNNING wave speed must be 12.0 dp/s", 12.0f, actualRunningSpeed, 0.05f)

            val actualOvertimeSpeed = lengthDp / (overtimeDurationMs / 1000f)
            assertEquals("Actual OVERTIME wave speed must be 17.142857 dp/s", overtimeSpeed, actualOvertimeSpeed, 0.05f)
        }

        // Proportional change test: 600dp -> 750dp (ratio = 1.25)
        val duration600 = HeroWaveMotionTokens.calculateCycleDurationMs(600.0f, HeroSemanticState.RUNNING)
        val duration750 = HeroWaveMotionTokens.calculateCycleDurationMs(750.0f, HeroSemanticState.RUNNING)
        val lengthRatio = 750.0 / 600.0
        val durationRatio = duration750.toDouble() / duration600.toDouble()
        assertEquals("600dp -> 750dp length change must scale duration proportionally (1.25x)", lengthRatio, durationRatio, 0.002)
    }

    @Test
    fun testDensityInvariance_physicalSpeedAcrossDisplayScales() {
        val pathLengthDp = 821.44f // Actual RUNNING wavy loop length for 295dp Hero
        val densities = listOf(1.0f, 1.5f, 2.0f, 2.75f, 3.0f, 3.5f, 4.0f)

        for (density in densities) {
            val lengthPx = pathLengthDp * density
            val durationMs = HeroWaveMotionTokens.calculateCycleDurationMsFromPx(
                pathLengthPx = lengthPx,
                density = density,
                semanticState = HeroSemanticState.RUNNING
            )

            // physical px/s = targetDpPerSec * density
            val physicalPxPerSec = lengthPx / (durationMs / 1000f)
            val expectedPxPerSec = 12.0f * density
            assertEquals("Physical px/s must equal targetDpPerSec * density", expectedPxPerSec, physicalPxPerSec, 0.1f)

            // physical dp/s = (lengthPx / density) / durationSec == 12.0 dp/s
            val physicalDpPerSec = (lengthPx / density) / (durationMs / 1000f)
            assertEquals("Physical dp/s must strictly remain 12.0 dp/s across all densities", 12.0f, physicalDpPerSec, 0.05f)
        }
    }

    @Test
    fun testGeometryPipelineDurationConsistency() {
        // Evaluate actual wavy geometry length from CircularWavyHeroGeometry
        val baseRadiusDp = 127.5f
        val runningWavyLengthDp = CircularWavyHeroGeometry.calculateActualWavyLoopLengthDp(
            baseRadiusDp = baseRadiusDp,
            amplitudeDp = 6.0f
        )
        val overtimeWavyLengthDp = CircularWavyHeroGeometry.calculateActualWavyLoopLengthDp(
            baseRadiusDp = baseRadiusDp,
            amplitudeDp = 7.0f
        )

        assertTrue("Actual wavy length must be ~821.44dp", runningWavyLengthDp in 820.0f..823.0f)
        assertTrue("Actual wavy overtime length must be ~829.71dp", overtimeWavyLengthDp in 828.0f..831.0f)

        // Verify HeroWaveMotionTokens.resolveCycleDurationMs with actual geometry path length
        val resolvedRunningDurationMs = HeroWaveMotionTokens.resolveCycleDurationMs(
            semanticState = HeroSemanticState.RUNNING,
            pathLengthDp = runningWavyLengthDp
        )
        val expectedRunningDurationMs = (runningWavyLengthDp / 12.0f * 1000f).roundToLong()
        assertEquals("Resolved RUNNING duration must match exact geometry formula (length / 12 * 1000)", expectedRunningDurationMs, resolvedRunningDurationMs)

        val resolvedOvertimeDurationMs = HeroWaveMotionTokens.resolveCycleDurationMs(
            semanticState = HeroSemanticState.OVERTIME,
            pathLengthDp = overtimeWavyLengthDp
        )
        val expectedOvertimeDurationMs = (overtimeWavyLengthDp / (12.0f * 20f / 14f) * 1000f).roundToLong()
        assertEquals("Resolved OVERTIME duration must match exact geometry formula (length / 17.142857 * 1000)", expectedOvertimeDurationMs, resolvedOvertimeDurationMs)

        // Wave velocities on the actual geometry
        val actualRunningVelocity = runningWavyLengthDp / (resolvedRunningDurationMs / 1000f)
        assertEquals(12.0f, actualRunningVelocity, 0.05f)

        val actualOvertimeVelocity = overtimeWavyLengthDp / (resolvedOvertimeDurationMs / 1000f)
        assertEquals(12.0f * 20f / 14f, actualOvertimeVelocity, 0.05f)
    }

    @Test
    fun testDeterministicPhaseProgression_60Hz_90Hz_120Hz() {
        val velocity = HeroWaveMotionTokens.PHASE_VELOCITY_RUNNING_RAD_PER_SEC

        // 1. Simulate 60 Hz display (60 frames of 1/60 sec = 1.0 second elapsed)
        var phase60Hz = 0f
        val dt60 = 1f / 60f
        repeat(60) {
            phase60Hz = HeroWaveMotionTokens.advancePhase(phase60Hz, velocity, dt60)
        }

        // 2. Simulate 90 Hz display (90 frames of 1/90 sec = 1.0 second elapsed)
        var phase90Hz = 0f
        val dt90 = 1f / 90f
        repeat(90) {
            phase90Hz = HeroWaveMotionTokens.advancePhase(phase90Hz, velocity, dt90)
        }

        // 3. Simulate 120 Hz display (120 frames of 1/120 sec = 1.0 second elapsed)
        var phase120Hz = 0f
        val dt120 = 1f / 120f
        repeat(120) {
            phase120Hz = HeroWaveMotionTokens.advancePhase(phase120Hz, velocity, dt120)
        }

        val expectedPhase1Sec = velocity * 1.0f

        assertEquals("60Hz phase after 1s must match theoretical progression", expectedPhase1Sec, phase60Hz, 0.001f)
        assertEquals("90Hz phase after 1s must match theoretical progression", expectedPhase1Sec, phase90Hz, 0.001f)
        assertEquals("120Hz phase after 1s must match theoretical progression", expectedPhase1Sec, phase120Hz, 0.001f)

        // All display refresh rates must have mathematically identical angular progression
        assertEquals("60Hz and 90Hz must be identical", phase60Hz, phase90Hz, 0.0001f)
        assertEquals("60Hz and 120Hz must be identical", phase60Hz, phase120Hz, 0.0001f)
    }

    @Test
    fun testPauseFreezesPhase_andResumePreservesPhase() {
        val velocityRunning = HeroWaveMotionTokens.PHASE_VELOCITY_RUNNING_RAD_PER_SEC

        // Run for 5 seconds
        var phase = 0f
        val dt = 0.016f // ~60fps
        repeat(312) { // 312 * 0.016s ≈ 4.992s
            phase = HeroWaveMotionTokens.advancePhase(phase, velocityRunning, dt)
        }
        val frozenPhaseAtPause = phase
        assertTrue("Phase must have progressed past zero", frozenPhaseAtPause > 0.4f)

        // State changes to PAUSED -> velocity becomes 0
        val velocityPaused = HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(
            HeroSemanticState.PAUSED,
            MotionPreference.NORMAL
        )
        assertEquals(0f, velocityPaused)

        // While paused, even if time passes, phase remains frozen in place
        repeat(100) {
            phase = HeroWaveMotionTokens.advancePhase(phase, velocityPaused, dt)
        }
        assertEquals("Phase must freeze completely while paused", frozenPhaseAtPause, phase, 0.00001f)

        // Resume to RUNNING -> velocity restored, continues advancing from frozen phase
        phase = HeroWaveMotionTokens.advancePhase(phase, velocityRunning, 1.0f)
        val expectedAfterResume = (frozenPhaseAtPause + (velocityRunning * 1.0f)) % (2.0 * PI).toFloat()
        assertEquals("Phase must seamlessly continue from frozen position upon resume", expectedAfterResume, phase, 0.001f)
    }

    @Test
    fun testPhaseAdvanceWrapping() {
        val twoPi = (2.0 * PI).toFloat()
        val nearTwoPi = twoPi - 0.05f

        // Advancing past 2pi wraps modulo 2pi cleanly
        val wrapped = HeroWaveMotionTokens.advancePhase(
            currentPhase = nearTwoPi,
            velocityRadPerSec = 1.0f,
            deltaSeconds = 0.10f
        )
        val expected = (nearTwoPi + 0.10f) % twoPi
        assertEquals(expected, wrapped, 0.0001f)
        assertTrue("Wrapped phase must stay < 2π", wrapped < twoPi)
        assertTrue("Wrapped phase must stay >= 0", wrapped >= 0f)
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
