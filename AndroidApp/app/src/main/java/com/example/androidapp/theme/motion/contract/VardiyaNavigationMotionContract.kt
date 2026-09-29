package com.example.androidapp.theme.motion.contract

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme

/**
 * Semantic destination transition types.
 */
enum class NavigationDestinationTransition {
    FORWARD,
    POP
}

/**
 * Semantic navigation selection states.
 */
enum class NavigationItemSemanticState {
    SELECTED,
    UNSELECTED
}

/**
 * Contract governing application navigation motion:
 * A. Destination screen transitions (forward & pop)
 * B. Navigation item selection states (selected vs unselected)
 * C. Active indicator transitions
 * D. Adaptive navigation mode coordination
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Material 3 Expressive navigation distinguishes hierarchy level transitions using
 * spatial directional shifts paired with critically damped effects fades. Active
 * indicators expand and settle organically via spatial springs.
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * Screen transitions use a subtle spatial offset factor (0.08f / 8%) paired with
 * effects fades to maintain orientation without full-screen carousel dizziness.
 */
interface VardiyaNavigationMotionContract {
    /**
     * Resolves the spatial offset factor for horizontal destination transitions.
     * Documented as a Vardiya design decision preserving v3.0.3 baseline.
     */
    fun resolveSpatialOffsetFactor(): Float

    /**
     * Resolves the content transform for forward destination transitions.
     */
    fun createForwardTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform

    /**
     * Resolves the content transform for pop / back destination transitions.
     */
    fun createPopTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform

    /**
     * Resolves the spatial spring spec for navigation item indicator morphing.
     */
    fun <T> resolveIndicatorSpatialSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>

    /**
     * Resolves the effects spring spec for navigation icon / text color transitions.
     */
    fun <T> resolveItemEffectsSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
}

/**
 * Default implementation of [VardiyaNavigationMotionContract].
 */
object DefaultVardiyaNavigationMotionContract : VardiyaNavigationMotionContract {
    // ========================================================================
    // VARDIYA DESIGN DECISIONS (Preserving established v3.0.3 navigation geometry)
    // ========================================================================
    const val VARDIYA_SUBTLE_SPATIAL_OFFSET_FACTOR = 0.08f

    override fun resolveSpatialOffsetFactor(): Float = VARDIYA_SUBTLE_SPATIAL_OFFSET_FACTOR

    override fun createForwardTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform {
        return if (motionPreference == MotionPreference.REDUCED) {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            (fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { (it * VARDIYA_SUBTLE_SPATIAL_OFFSET_FACTOR).toInt() }
                )) togetherWith
                (fadeOut(animationSpec = motionScheme.defaultEffectsSpec()) +
                    slideOutHorizontally(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        targetOffsetX = { (-it * VARDIYA_SUBTLE_SPATIAL_OFFSET_FACTOR).toInt() }
                    ))
        }
    }

    override fun createPopTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform {
        return if (motionPreference == MotionPreference.REDUCED) {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            (fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { (-it * VARDIYA_SUBTLE_SPATIAL_OFFSET_FACTOR).toInt() }
                )) togetherWith
                (fadeOut(animationSpec = motionScheme.defaultEffectsSpec()) +
                    slideOutHorizontally(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        targetOffsetX = { (it * VARDIYA_SUBTLE_SPATIAL_OFFSET_FACTOR).toInt() }
                    ))
        }
    }

    override fun <T> resolveIndicatorSpatialSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastSpatialSpec()

    override fun <T> resolveItemEffectsSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastEffectsSpec()
}
