package com.example.androidapp.theme.motion

import com.example.androidapp.theme.motion.contract.HeroSemanticState
import kotlin.math.PI
import kotlin.math.roundToLong

/**
 * Semantic motion design tokens governing the continuous periodic traveling wave motion
 * of Vardiya's 295dp Hero Progress indicator.
 *
 * ============================================================================
 * ANDROIDX MATERIAL 3 EXPRESSIVE ADAPTATION:
 * ============================================================================
 *
 * Upstream AndroidX Material 3 Expressive (`CircularWavyProgressModifiers.kt` / `WavyProgressIndicator.kt`)
 * defines wave speed as:
 *   waveSpeed = wavelength = 20.dp/sec.
 *
 * In standard 48dp AndroidX components, 6 waves traverse a ~125dp circumference in 6.25 seconds.
 * For Vardiya's 295dp Hero:
 * - WAVE_COUNT = 12 is preserved as a core product/design invariant representing the 12-hour shift dial.
 * - Target spatial wave velocity is calibrated to 12.0 dp/s for RUNNING, producing a calm,
 *   organic breathing tempo suitable for a central workspace hero.
 * - OVERTIME preserves Vardiya's established 1.4286x (20s/14s) acceleration ratio:
 *   target velocity ≈ 17.1429 dp/s.
 * - Cycle durations and phase velocities are derived from physical path length:
 *   durationMs = (pathLengthDp / targetVelocityDpPerSec) * 1000
 */
object HeroWaveMotionTokens {

    /** Number of waves around the 360-degree circumference (12-hour clock dial metaphor). */
    const val WAVE_COUNT = 12

    /** Target spatial linear wave velocity for RUNNING in dp/second (12.0 dp/s). */
    const val TARGET_VELOCITY_RUNNING_DP_PER_SEC = 12.0f

    /** Semantic speed acceleration ratio between OVERTIME and RUNNING (20s / 14s ≈ 1.4285714). */
    const val OVERTIME_VELOCITY_RATIO = 20.0f / 14.0f

    /** Target spatial linear wave velocity for OVERTIME in dp/second (~17.1429 dp/s). */
    const val TARGET_VELOCITY_OVERTIME_DP_PER_SEC = TARGET_VELOCITY_RUNNING_DP_PER_SEC * OVERTIME_VELOCITY_RATIO

    /**
     * Nominal reference circumference in DP for Vardiya's 295dp Hero.
     * With nominal base radius of 120.0.dp: 2 * PI * 120.0.dp ≈ 753.98224.dp.
     */
    const val NOMINAL_PATH_LENGTH_DP = (2.0 * PI * 120.0).toFloat()

    /** Full 360-degree rotation cycle duration in milliseconds for [HeroSemanticState.RUNNING] (~62.8s). */
    const val CYCLE_DURATION_RUNNING_MS = 62_832L

    /** Full 360-degree rotation cycle duration in milliseconds for [HeroSemanticState.OVERTIME] (~44.0s). */
    const val CYCLE_DURATION_OVERTIME_MS = 43_982L

    /** Time in milliseconds for one wave peak to travel to the next wave's position in RUNNING (~5.24s). */
    const val SINGLE_WAVE_PERIOD_RUNNING_MS = CYCLE_DURATION_RUNNING_MS / WAVE_COUNT

    /** Time in milliseconds for one wave peak to travel to the next wave's position in OVERTIME (~3.67s). */
    const val SINGLE_WAVE_PERIOD_OVERTIME_MS = CYCLE_DURATION_OVERTIME_MS / WAVE_COUNT

    /** Visual angular velocity in degrees per second for RUNNING (~5.73°/s). */
    const val ANGULAR_VELOCITY_RUNNING_DEG_PER_SEC = 360f / (CYCLE_DURATION_RUNNING_MS / 1000f)

    /** Visual angular velocity in degrees per second for OVERTIME (~8.19°/s). */
    const val ANGULAR_VELOCITY_OVERTIME_DEG_PER_SEC = 360f / (CYCLE_DURATION_OVERTIME_MS / 1000f)

    /** Continuous phase angular velocity in radians per second for RUNNING (2π / 62.832s = 0.1000 rad/s). */
    val PHASE_VELOCITY_RUNNING_RAD_PER_SEC: Float =
        (2.0 * PI / (CYCLE_DURATION_RUNNING_MS / 1000.0)).toFloat()

    /** Continuous phase angular velocity in radians per second for OVERTIME (2π / 43.982s ≈ 0.1429 rad/s). */
    val PHASE_VELOCITY_OVERTIME_RAD_PER_SEC: Float =
        (2.0 * PI / (CYCLE_DURATION_OVERTIME_MS / 1000.0)).toFloat()

    /**
     * Resolves the target linear wave velocity in dp/second for a given [semanticState].
     */
    fun resolveTargetVelocityDpPerSec(semanticState: HeroSemanticState): Float = when (semanticState) {
        HeroSemanticState.OVERTIME -> TARGET_VELOCITY_OVERTIME_DP_PER_SEC
        HeroSemanticState.RUNNING -> TARGET_VELOCITY_RUNNING_DP_PER_SEC
        HeroSemanticState.NOT_STARTED,
        HeroSemanticState.PAUSED,
        HeroSemanticState.BREAK,
        HeroSemanticState.FINISHED -> 0f
    }

    /**
     * Calculates the cycle duration in milliseconds derived from the actual path length in DP.
     */
    fun calculateCycleDurationMs(
        pathLengthDp: Float,
        semanticState: HeroSemanticState
    ): Long {
        val targetVelocity = resolveTargetVelocityDpPerSec(semanticState)
        if (targetVelocity <= 0f || pathLengthDp <= 0f) return 0L
        return (pathLengthDp / targetVelocity * 1000f).roundToLong()
    }

    /**
     * Calculates the cycle duration in milliseconds derived from path length in pixels and display density.
     */
    fun calculateCycleDurationMsFromPx(
        pathLengthPx: Float,
        density: Float,
        semanticState: HeroSemanticState
    ): Long {
        if (density <= 0f || pathLengthPx <= 0f) return resolveCycleDurationMs(semanticState)
        val pathLengthDp = pathLengthPx / density
        return calculateCycleDurationMs(pathLengthDp, semanticState)
    }

    /**
     * Resolves the angular velocity in radians per second for continuous traveling wave animation.
     * Optionally accepts [pathLengthDp] to dynamically calibrate angular speed to actual geometry.
     */
    fun resolvePhaseVelocityRadPerSec(
        semanticState: HeroSemanticState,
        motionPreference: MotionPreference,
        pathLengthDp: Float = NOMINAL_PATH_LENGTH_DP
    ): Float {
        if (motionPreference == MotionPreference.REDUCED) return 0f
        val targetVelocity = resolveTargetVelocityDpPerSec(semanticState)
        if (targetVelocity <= 0f) return 0f
        val effectiveLength = if (pathLengthDp > 0f) pathLengthDp else NOMINAL_PATH_LENGTH_DP
        val durationSec = effectiveLength / targetVelocity
        return ((2.0 * PI) / durationSec).toFloat()
    }

    /**
     * Resolves the full 360-degree rotation cycle duration in milliseconds.
     * Optionally accepts [pathLengthDp] to dynamically calibrate duration to actual geometry.
     */
    fun resolveCycleDurationMs(
        semanticState: HeroSemanticState,
        pathLengthDp: Float = NOMINAL_PATH_LENGTH_DP
    ): Long {
        if (pathLengthDp <= 0f || pathLengthDp == NOMINAL_PATH_LENGTH_DP) {
            return when (semanticState) {
                HeroSemanticState.OVERTIME -> CYCLE_DURATION_OVERTIME_MS
                else -> CYCLE_DURATION_RUNNING_MS
            }
        }
        return calculateCycleDurationMs(pathLengthDp, semanticState)
    }

    /**
     * Advances the traveling wave phase deterministically based on elapsed time in seconds.
     * Normalized to [0, 2π) to guarantee identical visual velocity across 60Hz, 90Hz, and 120Hz displays.
     */
    fun advancePhase(
        currentPhase: Float,
        velocityRadPerSec: Float,
        deltaSeconds: Float
    ): Float {
        if (velocityRadPerSec <= 0f || deltaSeconds <= 0f) return currentPhase
        val twoPi = (2.0 * PI).toFloat()
        val deltaPhase = velocityRadPerSec * deltaSeconds
        return (currentPhase + deltaPhase) % twoPi
    }
}

