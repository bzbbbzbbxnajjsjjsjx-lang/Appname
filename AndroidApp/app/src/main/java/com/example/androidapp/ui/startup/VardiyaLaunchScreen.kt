package com.example.androidapp.ui.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.theme.shape.VardiyaExpressiveShapes
import com.example.androidapp.theme.shape.VardiyaShapeMorph
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Material 3 Expressive Launch & Startup Experience for Vardiya.
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE (Google):
 * In Material 3 Expressive Loading and Brand moments, central visual identity is
 * expressed by continuous polygon morphing across canonical geometries:
 * Soft Burst -> Cookie 9 -> Pentagon -> Pill -> Sunny -> Cookie 4 -> Oval.
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * - The launch screen renders a LARGE, centered morphing shape (220dp).
 * - Intentionally SHORT (1200ms nominal) to prevent blocking the user, with tap-to-skip.
 * - Under [MotionPreference.REDUCED], morphing is suppressed; a calm, static branded
 *   gem is shown with a clean alpha transition.
 */
@Composable
fun VardiyaLaunchScreen(
    onLaunchComplete: () -> Unit,
    modifier: Modifier = Modifier,
    nominalDurationMs: Long = 1350L
) {
    val motionPreference = VardiyaTheme.motionPreference
    val isReducedMotion = motionPreference == MotionPreference.REDUCED

    val shapes = remember { VardiyaExpressiveShapes.IndeterminateSequence }
    val reusablePath = remember { Path() }

    var morphProgress by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

    // Continuous morph engine during launch
    if (!isReducedMotion) {
        LaunchedEffect(Unit) {
            val speed = 0.0016f // Morph speed fraction per millisecond
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos != 0L) {
                        val deltaMs = (frameTimeNanos - lastFrameNanos) / 1_000_000f
                        morphProgress = (morphProgress + deltaMs * speed) % shapes.size
                        rotationDegrees = (rotationDegrees + deltaMs * 0.045f) % 360f
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
        }
    }

    // Nominal non-blocking lifecycle timer
    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            delay(400L) // Minimal brief display for accessibility
        } else {
            delay(nominalDurationMs)
        }
        onLaunchComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLaunchComplete
            )
            .semantics {
                contentDescription = "Vardiya başlatılıyor"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // LARGE Centered Expressive Morphing Shape (220dp)
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bounds = Rect(0f, 0f, size.width, size.height)

                    if (isReducedMotion) {
                        // Static, calm branded shape without continuous morphing
                        val staticPath = VardiyaExpressiveShapes.Gem.toPath(bounds, reusablePath)
                        drawPath(path = staticPath, color = primaryColor)
                    } else {
                        // Dynamic organic shape morphing
                        val currentPath = VardiyaShapeMorph.morphSequence(
                            shapes = shapes,
                            overallProgress = morphProgress,
                            bounds = bounds,
                            outPath = reusablePath,
                            rotationDegrees = rotationDegrees
                        )
                        drawPath(path = currentPath, color = primaryColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Brand Title & Identity
            Text(
                text = "Vardiya",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Akıllı Mesai & Gelir Takibi",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
