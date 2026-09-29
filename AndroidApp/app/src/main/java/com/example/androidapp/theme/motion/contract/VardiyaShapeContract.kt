package com.example.androidapp.theme.motion.contract

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.unit.Dp
import com.example.androidapp.theme.motion.VardiyaMotionScheme

/**
 * Semantic shape contract governing shape morphing transitions.
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Material 3 Expressive components morph shapes between interaction states using
 * spatial spring curves without jumping discrete corner values (e.g. MaterialShapeDrawable
 * dynamic corner animations).
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * Shape transitions query spatial spring specs from [VardiyaMotionScheme] without duplicating
 * physical constants. Concrete corner radius numbers are determined by specific UI component
 * themes, not hardcoded into this contract.
 */
interface VardiyaShapeContract {
    /**
     * Resolves the spatial animation spec for corner radius transitions between [from] and [to].
     */
    fun resolveCornerSpec(
        from: VardiyaSemanticMotionState,
        to: VardiyaSemanticMotionState,
        motionScheme: VardiyaMotionScheme
    ): FiniteAnimationSpec<Dp>
}

/**
 * Default implementation of [VardiyaShapeContract].
 */
object DefaultVardiyaShapeContract : VardiyaShapeContract {
    override fun resolveCornerSpec(
        from: VardiyaSemanticMotionState,
        to: VardiyaSemanticMotionState,
        motionScheme: VardiyaMotionScheme
    ): FiniteAnimationSpec<Dp> {
        return when {
            from == VardiyaSemanticMotionState.PRESSED || to == VardiyaSemanticMotionState.PRESSED ->
                motionScheme.fastSpatialSpec()
            from == VardiyaSemanticMotionState.EXPANDED || to == VardiyaSemanticMotionState.EXPANDED ->
                motionScheme.defaultSpatialSpec()
            else ->
                motionScheme.fastSpatialSpec()
        }
    }
}
