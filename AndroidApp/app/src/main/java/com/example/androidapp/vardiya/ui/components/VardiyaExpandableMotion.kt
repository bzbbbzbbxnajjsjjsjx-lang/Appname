package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.vardiya.domain.model.ShiftState

/**
 * Reusable motion specifications for expandable sections (Level 3 Supporting Motion).
 * Used across Settings options, Calendar Heatmap visibility, and History item details.
 */
object VardiyaExpandableMotion {
    fun createEnterTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): EnterTransition =
        if (motionPreference == MotionPreference.REDUCED) {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            expandVertically(animationSpec = motionScheme.defaultSpatialSpec()) +
                fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
        }

    fun createExitTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ExitTransition =
        if (motionPreference == MotionPreference.REDUCED) {
            fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            shrinkVertically(animationSpec = motionScheme.defaultSpatialSpec()) +
                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        }
}

/**
 * State and lifecycle mapping for StateBadge micro interactions (Level 4 Micro Motion).
 * Guarantees zero continuous animation or frame loops when idle.
 */
object StateBadgeMotion {
    const val PulseInitialAlpha: Float = 0.35f
    const val PulseTargetAlpha: Float = 1.0f
    const val PulsePeriodMillis: Int = 1200

    /**
     * Determines whether the StateBadge continuous pulse effect should be active.
     * Strictly active ONLY during [ShiftState.RUNNING] without an active break,
     * and only when [MotionPreference.REDUCED] is not active.
     * In all other states ([ShiftState.NOT_STARTED], [ShiftState.PAUSED],
     * active break, [ShiftState.FINISHED]), returns false to eliminate idle frame drawing.
     */
    fun isPulseActive(
        shiftState: ShiftState,
        isBreakActive: Boolean,
        motionPreference: MotionPreference
    ): Boolean =
        motionPreference != MotionPreference.REDUCED &&
        shiftState == ShiftState.RUNNING &&
        !isBreakActive

    /**
     * Named semantic spec for StateBadge periodic breathing pulse.
     * Note: Physics-based spring specs settle to asymptotic equilibrium and cannot be used
     * directly in [androidx.compose.animation.core.infiniteRepeatable] cycles without continuous perturbations.
     * A harmonic tween with FastOutSlowInEasing provides mathematically deterministic periodic breathing.
     */
    fun pulseAnimationSpec(): DurationBasedAnimationSpec<Float> =
        tween(durationMillis = PulsePeriodMillis, easing = FastOutSlowInEasing)
}
