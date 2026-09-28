package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
 * Large circular progress indicator with an organic, wavy edge according to Material 3 Expressive.
 * - Dynamic wave frequency, amplitude, and color adapting to shift state, overtime, and active break.
 * - Clockwise progression starting from 12 o'clock (-90°).
 * - Smoothly animates on progress changes without continuous idle CPU consumption.
 * - Keeps center area clean, legible, and accessible.
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
    // Smoothly animate progress without continuous idle ticker loop
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "wavy_progress_anim"
    )

    val activeColor = when {
        shiftState == ShiftState.FINISHED -> MaterialTheme.colorScheme.secondary
        shiftState == ShiftState.PAUSED -> MaterialTheme.colorScheme.tertiary
        isBreakActive -> MaterialTheme.colorScheme.tertiary
        isOvertimeActive -> MaterialTheme.colorScheme.tertiary
        shiftState == ShiftState.RUNNING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    val (waveAmplitudeDp, waveCount) = when {
        shiftState == ShiftState.FINISHED -> Pair(2.5.dp, 18.0)
        shiftState == ShiftState.PAUSED -> Pair(1.2.dp, 14.0)
        isBreakActive -> Pair(2.0.dp, 14.0)
        isOvertimeActive -> Pair(4.2.dp, 20.0)
        shiftState == ShiftState.RUNNING -> Pair(3.0.dp, 16.0)
        else -> Pair(2.5.dp, 18.0)
    }

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)

    BoxWithConstraints(
        modifier = modifier
            .progressSemantics(animatedProgress)
            .semantics {
                contentDescription = "${heroSubtitleText}: ${heroAmountText}, ilerleme: ${(animatedProgress * 100).roundToInt()}%"
            },
        contentAlignment = Alignment.Center
    ) {
        // Responsive size: fits comfortably on small and large screens
        val indicatorSize = min(min(maxWidth, maxHeight), 280.dp)

        Box(
            modifier = Modifier
                .size(indicatorSize)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 16.dp.toPx()
                val waveAmplitudePx = waveAmplitudeDp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (min(size.width, size.height) - strokeWidthPx * 2f - waveAmplitudePx * 2f) / 2f

                if (baseRadius <= 0f) return@Canvas

                // 1. Draw smooth background track
                drawCircle(
                    color = trackColor,
                    radius = baseRadius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidthPx * 0.7f, cap = StrokeCap.Round)
                )

                // 2. Draw organic wavy progress arc
                val currentProgress = animatedProgress
                if (currentProgress > 0.005f) {
                    val startAngleRad = -PI / 2.0 // 12 o'clock
                    val sweepAngleRad = currentProgress * 2.0 * PI
                    val sampleSteps = max(24, (currentProgress * 180.0).roundToInt())

                    val path = Path()

                    // Outer wavy contour (clockwise)
                    for (i in 0..sampleSteps) {
                        val fraction = i.toDouble() / sampleSteps.toDouble()
                        val angle = startAngleRad + fraction * sweepAngleRad
                        val waveOffset = waveAmplitudePx * sin(waveCount * angle)
                        val rOuter = baseRadius + (strokeWidthPx / 2f) + waveOffset
                        val x = (centerOffset.x + rOuter * cos(angle)).toFloat()
                        val y = (centerOffset.y + rOuter * sin(angle)).toFloat()

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Forward rounded tip at leading edge
                    val endAngle = startAngleRad + sweepAngleRad
                    val endWaveOffset = waveAmplitudePx * sin(waveCount * endAngle)
                    val rInnerEnd = baseRadius - (strokeWidthPx / 2f) - endWaveOffset
                    val xInnerEnd = (centerOffset.x + rInnerEnd * cos(endAngle)).toFloat()
                    val yInnerEnd = (centerOffset.y + rInnerEnd * sin(endAngle)).toFloat()

                    // Quadratic curve around the tip
                    val rMidEnd = baseRadius
                    val tipExtension = strokeWidthPx * 0.35f
                    val ctrlX = (centerOffset.x + (rMidEnd + tipExtension) * cos(endAngle) - tipExtension * sin(endAngle)).toFloat()
                    val ctrlY = (centerOffset.y + (rMidEnd + tipExtension) * sin(endAngle) + tipExtension * cos(endAngle)).toFloat()
                    path.quadraticTo(ctrlX, ctrlY, xInnerEnd, yInnerEnd)

                    // Inner wavy contour (counter-clockwise back to start)
                    for (i in sampleSteps downTo 0) {
                        val fraction = i.toDouble() / sampleSteps.toDouble()
                        val angle = startAngleRad + fraction * sweepAngleRad
                        val waveOffset = waveAmplitudePx * sin(waveCount * angle)
                        val rInner = baseRadius - (strokeWidthPx / 2f) - waveOffset
                        val x = (centerOffset.x + rInner * cos(angle)).toFloat()
                        val y = (centerOffset.y + rInner * sin(angle)).toFloat()
                        path.lineTo(x, y)
                    }

                    // Rounded start cap
                    val startWaveOffset = waveAmplitudePx * sin(waveCount * startAngleRad)
                    val rOuterStart = baseRadius + (strokeWidthPx / 2f) + startWaveOffset
                    val xOuterStart = (centerOffset.x + rOuterStart * cos(startAngleRad)).toFloat()
                    val yOuterStart = (centerOffset.y + rOuterStart * sin(startAngleRad)).toFloat()

                    val tipStartExtension = strokeWidthPx * 0.35f
                    val ctrlStartX = (centerOffset.x + baseRadius * cos(startAngleRad) + tipStartExtension * sin(startAngleRad)).toFloat()
                    val ctrlStartY = (centerOffset.y + baseRadius * sin(startAngleRad) - tipStartExtension * cos(startAngleRad)).toFloat()
                    path.quadraticTo(ctrlStartX, ctrlStartY, xOuterStart, yOuterStart)

                    path.close()

                    // Render active wavy ribbon
                    drawPath(
                        path = path,
                        color = activeColor
                    )
                }
            }

            // Clean, accessible center area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Hero Money Value
                Text(
                    text = heroAmountText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = when {
                            heroAmountText.length > 15 -> 30.sp
                            heroAmountText.length > 11 -> 36.sp
                            heroAmountText.length > 8 -> 44.sp
                            else -> 50.sp
                        },
                        fontWeight = FontWeight.Bold,
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
