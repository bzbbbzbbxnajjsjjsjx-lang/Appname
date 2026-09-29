package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Custom Canvas Wavy Progress Hero Indicator modeled after Material 3 Expressive.
 *
 * NOTE: The official AndroidX Compose Material 3 CircularWavyProgressIndicator API is not available
 * in Material 3 1.4.0 (introduced in 1.5.0-alpha+). In strict compliance with zero unauthorized dependency
 * upgrades, this component provides a dependency-free custom Canvas implementation:
 * - Mathematical sinusoidal wave running along the circular track circumference: r(θ) = R + A * sin(n * θ - φ)
 * - Determinate progress arc smoothly advancing clockwise from 12 o'clock (-90°).
 * - Non-overlapping track and active progress segments: track only renders where progress has not reached.
 * - Smooth circular arc track (düzgün circular arc) along the nominal centerline.
 * - Physical 4.dp gap (CircularIndicatorTrackGapSize) separating active progress head and guide track.
 * - Bold 10.dp active stroke width and 9.5.dp track stroke width, matching Material 3 Expressive visual weight.
 * - Dynamic, traveling wave animation (waveSpeed) running exclusively when shift is RUNNING.
 * - Zero CPU / idle battery consumption when PAUSED, FINISHED, or NOT_STARTED.
 * - Preserves all hero typography, labels, duration, percentage, and accessibility semantics.
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
    // Smoothly animate progress updates (e.g. per second increments)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "wavy_progress_anim"
    )

    // Animated traveling wave phase (runs ONLY when RUNNING, zero idle overhead in other states)
    val isRunning = shiftState == ShiftState.RUNNING
    val phaseAnimation: Float = if (isRunning) {
        val transition = rememberInfiniteTransition(label = "wavy_phase_transition")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isOvertimeActive) 1800 else 2400,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavy_phase"
        )
        phase
    } else {
        0f
    }

    // Material 3 Expressive state-adaptive colors
    val activeColor = when {
        shiftState == ShiftState.FINISHED -> MaterialTheme.colorScheme.secondary
        shiftState == ShiftState.PAUSED || isBreakActive || isOvertimeActive -> MaterialTheme.colorScheme.tertiary
        shiftState == ShiftState.RUNNING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    // Track color: visible against surface, creating the expressive wavy guide ring
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)

    // Amplitude: distinct, pronounced, smooth wave height per state
    val waveAmplitudeDp = when {
        isOvertimeActive -> 7.0.dp
        shiftState == ShiftState.RUNNING -> 6.0.dp
        isBreakActive -> 5.0.dp
        shiftState == ShiftState.PAUSED -> 4.5.dp
        shiftState == ShiftState.FINISHED -> 5.0.dp
        else -> 5.0.dp
    }

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
                val waveAmplitudePx = waveAmplitudeDp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Margin for outer wave peaks + stroke width + safety padding
                val outerPeakOffset = waveAmplitudePx + (strokeWidthPx / 2f) + 4.dp.toPx()
                val baseRadius = (min(size.width, size.height) / 2f) - outerPeakOffset

                if (baseRadius <= 0f) return@Canvas

                // 12 waves around 360° (30° per wave cycle, aligning with clock hour marks)
                val waveFrequency = 12
                val phase = phaseAnimation
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
                    val activePath = Path()

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
                        color = activeColor,
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
                    color = activeColor,
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
                        color = activeColor
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
