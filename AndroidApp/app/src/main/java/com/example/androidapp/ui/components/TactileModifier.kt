package com.example.androidapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.theme.motion.contract.ButtonSemanticState
import com.example.androidapp.theme.motion.contract.DefaultVardiyaButtonMotionContract
import com.example.androidapp.theme.motion.contract.VardiyaButtonMotionContract

/**
 * Material 3 Expressive Tactile Press Modifier.
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE (Google):
 * Interactive surfaces provide tactile physical feedback via spring-driven scale
 * depression when pressed, immediately rebounding on release.
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * - Uses [DefaultVardiyaButtonMotionContract.VARDIYA_BUTTON_PRESSED_SCALE] (0.94f).
 * - Under [MotionPreference.REDUCED], scale remains static at 1.0f.
 * - Hardware-accelerated via [graphicsLayer] to eliminate recomposition costs.
 */
fun Modifier.tactilePress(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    contract: VardiyaButtonMotionContract = DefaultVardiyaButtonMotionContract,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val motionScheme = VardiyaTheme.motionScheme
    val motionPreference = VardiyaTheme.motionPreference
    val isReducedMotion = motionPreference == MotionPreference.REDUCED

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val targetScale = if (isReducedMotion || !enabled) {
        1.0f
    } else if (isPressed) {
        contract.resolvePressedScale()
    } else {
        1.0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = contract.resolveScaleSpec(motionScheme),
        label = "tactile_scale_anim"
    )

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = source,
            indication = null, // Custom physical scale provides tactile feedback
            enabled = enabled,
            onClick = onClick
        )
    } else {
        Modifier
    }

    this
        .then(clickableMod)
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
}
