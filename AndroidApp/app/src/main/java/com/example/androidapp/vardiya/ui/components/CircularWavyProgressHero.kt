package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Pure motion mapping functions for [CircularWavyProgressHero].
 * Extracted to allow deterministic unit testing of physical targets, speeds, and accessibility states.
 */
object CircularWavyHeroMotion {
    /**
     * Resolves the target wave amplitude in [Dp] based on shift state, overtime, and break.
     * Matches the established Vardiya 3.0.2 production geometry.
     */
    fun resolveTargetAmplitude(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean
    ): Dp = when {
        isBreakActive -> 5.0.dp
        isOvertimeActive -> 7.0.dp
        shiftState == ShiftState.RUNNING -> 6.0.dp
        shiftState == ShiftState.PAUSED -> 4.5.dp
        shiftState == ShiftState.FINISHED -> 5.0.dp
        else -> 5.0.dp
    }

    /**
     * Resolves whether the traveling wave phase animation should actively advance.
     * ZERO continuous phase animation when NOT_STARTED, PAUSED, FINISHED, on BREAK,
     * or when [MotionPreference.REDUCED] is active.
     */
    fun isWavePhaseActive(
        shiftState: ShiftState,
        isBreakActive: Boolean,
        motionPreference: MotionPreference
    ): Boolean =
        motionPreference != MotionPreference.REDUCED &&
        shiftState == ShiftState.RUNNING &&
        !isBreakActive

    /**
     * Resolves the full 360-degree wave cycle duration in milliseconds.
     */
    fun resolveWaveCycleDurationMs(isOvertimeActive: Boolean): Long =
        if (isOvertimeActive) 1800L else 2400L

    /**
     * Resolves the semantic active hero color for the progress wave and typography.
     */
    fun resolveHeroActiveColor(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean,
        colorScheme: ColorScheme
    ): Color = when {
        shiftState == ShiftState.FINISHED -> colorScheme.secondary
        shiftState == ShiftState.PAUSED || isBreakActive || isOvertimeActive -> colorScheme.tertiary
        shiftState == ShiftState.RUNNING -> colorScheme.primary
        else -> colorScheme.primary
    }
}

/**
 * Custom Canvas Wavy Progress Hero Indicator powered by Material 3 Expressive Motion.
 *
 * Integrated with the Vardiya Motion System:
 * - Progress advancement: Uses [VardiyaTheme.motionScheme.defaultSpatialSpec] (expressive physics spring).
 * - Wave amplitude: Settles organically via spatial spring rather than hard-snapping across state changes.
 * - State colors: Transitions smoothly via [VardiyaTheme.motionScheme.defaultEffectsSpec] (critically damped).
 * - Phase engine: Runs continuous frame-synchronized traveling wave ONLY while [ShiftState.RUNNING].
 *   When [ShiftState.PAUSED], freezes in place at its current phase without resetting to zero.
 * - Idle battery efficiency: ZERO continuous phase animation while NOT_STARTED, PAUSED, on BREAK, or FINISHED.
 * - Accessibility: When [MotionPreference.REDUCED] is active, phase animation is disabled and all
 *   spatial/effects transitions snap instantaneously.
 * - Zero per-frame allocations: Reuses a remembered [Path] with [Path.reset] inside Canvas drawing.
 * - Geometry preservation: Retains the physical 4.dp gap, 10.dp stroke width, 9.5.dp track, and 12-wave frequency.
 */
@Composable
fun CircularWavyProgressHero(
    progress: Float,
    shiftState: ShiftState,
    heroAmountText: String,
    heroSubtitleText: String,
    durationText: String?,
    modifier: Modifier = Modifier,
    isOvertimeActive: Boolean = false,
    isBreakActive: Boolean = false,
    breakDurationText: String? = null,
    isNightShiftActive: Boolean = false
) {
    val motionScheme = VardiyaTheme.motionScheme
    val motionPreference = VardiyaTheme.motionPreference

    // 1. Physical Progress Motion (Expressive Spatial Spring)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "wavy_progress_anim"
    )

    // 2. Physical Amplitude Motion (Expressive Spatial Spring Settling)
    val targetAmplitude = CircularWavyHeroMotion.resolveTargetAmplitude(
        shiftState = shiftState,
        isOvertimeActive = isOvertimeActive,
        isBreakActive = isBreakActive
    )
    val animatedAmplitudeDp by animateDpAsState(
        targetValue = targetAmplitude,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "wavy_amplitude_anim"
    )

    // 3. Critically Damped State Color Transition (Effects Spring)
    val targetActiveColor = CircularWavyHeroMotion.resolveHeroActiveColor(
        shiftState = shiftState,
        isOvertimeActive = isOvertimeActive,
        isBreakActive = isBreakActive,
        colorScheme = MaterialTheme.colorScheme
    )
    val animatedActiveColor by animateColorAsState(
        targetValue = targetActiveColor,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "wavy_active_color_anim"
    )

    // 4. Traveling Wave Phase Engine
    var phaseAccumulator by remember { mutableFloatStateOf(0f) }

    // Reset phase cleanly to 12 o'clock when shift resets to NOT_STARTED
    LaunchedEffect(shiftState) {
        if (shiftState == ShiftState.NOT_STARTED) {
            phaseAccumulator = 0f
        }
    }

    val isWaveActive = CircularWavyHeroMotion.isWavePhaseActive(
        shiftState = shiftState,
        isBreakActive = isBreakActive,
        motionPreference = motionPreference
    )

    if (isWaveActive) {
        val cycleDurationMs = CircularWavyHeroMotion.resolveWaveCycleDurationMs(isOvertimeActive)
        LaunchedEffect(cycleDurationMs) {
            val cycleDurationNanos = cycleDurationMs * 1_000_000L
            val twoPi = (2.0 * PI).toFloat()
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos != 0L) {
                        val deltaNanos = frameTimeNanos - lastFrameNanos
                        val deltaPhase = (deltaNanos.toDouble() / cycleDurationNanos.toDouble() * twoPi).toFloat()
                        phaseAccumulator = (phaseAccumulator + deltaPhase) % twoPi
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
        }
    }

    // Track color: visible against surface, creating the expressive wavy guide ring
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)

    // Reusable Path to eliminate per-frame allocations during Canvas draw
    val activePath = remember { Path() }

    BoxWithConstraints(
        modifier = modifier
            .progressSemantics(animatedProgress)
            .semantics {
                contentDescription = "${heroSubtitleText}: ${heroAmountText}, ilerleme: ${(animatedProgress * 100).roundToInt()}%"
            },
        contentAlignment = Alignment.Center
    ) {
        // Responsive size: fits comfortably on small phones, tablets, and large screens
        val indicatorSize = min(min(maxWidth, maxHeight), 280.dp)

        Box(
            modifier = Modifier
                .size(indicatorSize)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 10.dp.toPx()
                val trackStrokeWidthPx = 9.5.dp.toPx()
                val waveAmplitudePx = animatedAmplitudeDp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Margin for outer wave peaks + stroke width + safety padding
                val outerPeakOffset = waveAmplitudePx + (strokeWidthPx / 2f) + 4.dp.toPx()
                val baseRadius = (min(size.width, size.height) / 2f) - outerPeakOffset

                if (baseRadius <= 0f) return@Canvas

                // 12 waves around 360° (30° per wave cycle, aligning with clock hour marks)
                val waveFrequency = 12
                val phase = phaseAccumulator
                val startAngleRad = -PI / 2.0 // 12 o'clock
                val fullCircleRad = 2.0 * PI

                val currentProgress = animatedProgress.coerceIn(0f, 1f)
                val progressSweepRad = currentProgress * fullCircleRad

                // Gap handling inspired by official AndroidX CircularIndicatorTrackGapSize (4.dp)
                // Angular spacing accounts for 4.dp physical air gap plus round stroke caps
                val gapSizePx = 4.dp.toPx()
                val capWidthPx = (strokeWidthPx / 2f) + (trackStrokeWidthPx / 2f)
                val fullGapAngleRad = ((gapSizePx + capWidthPx) / baseRadius).toDouble()
                val headGapAngleRad = fullGapAngleRad.coerceAtMost(progressSweepRad)
                val tailGapAngleRad = fullGapAngleRad.coerceAtMost(progressSweepRad)

                // 1. Draw the non-overlapping smooth circular track (only where progress has not reached)
                val trackStartAngleRad = startAngleRad + progressSweepRad + headGapAngleRad
                val trackEndAngleRad = startAngleRad + fullCircleRad - tailGapAngleRad
                val trackSweepRad = trackEndAngleRad - trackStartAngleRad

                if (currentProgress < 0.995f && trackSweepRad > 0.02) {
                    if (currentProgress < 0.005f) {
                        // Full circle smooth track when no progress
                        drawCircle(
                            color = trackColor,
                            radius = baseRadius,
                            center = centerOffset,
                            style = Stroke(
                                width = trackStrokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )
                    } else {
                        // Smooth circular arc with rounded caps matching nominal centerline
                        val trackStartDeg = Math.toDegrees(trackStartAngleRad).toFloat()
                        val trackSweepDeg = Math.toDegrees(trackSweepRad).toFloat()
                        val trackDiameter = baseRadius * 2f
                        val trackTopLeft = Offset(centerOffset.x - baseRadius, centerOffset.y - baseRadius)

                        drawArc(
                            color = trackColor,
                            startAngle = trackStartDeg,
                            sweepAngle = trackSweepDeg,
                            useCenter = false,
                            topLeft = trackTopLeft,
                            size = Size(trackDiameter, trackDiameter),
                            style = Stroke(
                                width = trackStrokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                // 2. Draw the active determinate wavy progress arc
                if (currentProgress > 0.005f) {
                    val activeSteps = max(24, (currentProgress * 720.0).roundToInt())
                    activePath.reset()

                    for (i in 0..activeSteps) {
                        val fraction = i.toDouble() / activeSteps.toDouble()
                        val angle = startAngleRad + fraction * progressSweepRad
                        val waveOffset = waveAmplitudePx * sin(waveFrequency * angle - phase)
                        val r = baseRadius + waveOffset
                        val x = (centerOffset.x + r * cos(angle)).toFloat()
                        val y = (centerOffset.y + r * sin(angle)).toFloat()
                        if (i == 0) {
                            activePath.moveTo(x, y)
                        } else {
                            activePath.lineTo(x, y)
                        }
                    }

                    if (currentProgress >= 0.999f) {
                        activePath.close()
                    }

                    drawPath(
                        path = activePath,
                        color = animatedActiveColor,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Clean, accessible center area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Hero Money Value (dominant central element)
                Text(
                    text = heroAmountText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = when {
                            heroAmountText.length > 15 -> 30.sp
                            heroAmountText.length > 11 -> 36.sp
                            heroAmountText.length > 8 -> 44.sp
                            else -> 52.sp
                        },
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.0).sp
                    ),
                    color = animatedActiveColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hero Subtitle
                Text(
                    text = heroSubtitleText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Optional Duration & Progress percentage
                if (durationText != null && shiftState != ShiftState.NOT_STARTED) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val percentInt = (animatedProgress * 100).roundToInt()
                    Text(
                        text = "%$percentInt",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = animatedActiveColor
                    )
                }

                // Dynamic Status Badge Chip inside Hero
                if (isBreakActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = if (!breakDurationText.isNullOrEmpty()) "☕ Mola: $breakDurationText" else "☕ Molada",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                } else if (isOvertimeActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = "⚡ Fazla Mesai",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
