package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.theme.motion.contract.DefaultVardiyaExpandableContainerMotionContract
import com.example.androidapp.theme.motion.contract.ExpandableSemanticState
import com.example.androidapp.theme.motion.contract.VardiyaExpandableContainerMotionContract
import com.example.androidapp.vardiya.domain.model.ShiftState

/**
 * Reusable motion specifications for expandable sections (Level 3 Supporting Motion).
 * Backed by [VardiyaExpandableContainerMotionContract] to separate semantic state
 * from UI container expansion.
 */
object VardiyaExpandableMotion {
    val contract: VardiyaExpandableContainerMotionContract = DefaultVardiyaExpandableContainerMotionContract

    fun resolveSemanticState(isExpanded: Boolean): ExpandableSemanticState =
        contract.resolveSemanticState(isExpanded)

    fun createEnterTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): EnterTransition = contract.createEnterTransition(motionScheme, motionPreference)

    fun createExitTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ExitTransition = contract.createExitTransition(motionScheme, motionPreference)
}

/**
 * State and lifecycle mapping for StateBadge micro interactions (Level 4 Micro Motion).
 * Guarantees zero continuous animation or frame loops when idle.
 */
object StateBadgeMotion {
    const val PulseInitialAlpha: Float = 0.35f
    const val PulseTargetAlpha: Float = 1.0f
    const val PulseInitialScale: Float = 0.90f
    const val PulseTargetScale: Float = 1.10f
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
