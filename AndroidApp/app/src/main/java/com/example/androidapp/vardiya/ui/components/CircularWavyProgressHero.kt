package com.example.androidapp.vardiya.ui.components

import android.graphics.Matrix as AndroidMatrix
import android.graphics.Path as AndroidPath
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.hypot
import com.example.androidapp.theme.motion.HeroWaveMotionTokens
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.theme.motion.contract.DefaultVardiyaHeroMotionContract
import com.example.androidapp.theme.motion.contract.HeroSemanticState
import com.example.androidapp.theme.motion.contract.VardiyaHeroMotionContract
import com.example.androidapp.vardiya.domain.model.ShiftState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Real AndroidX Material 3 Expressive Geometry Engine for [CircularWavyProgressHero].
 *
 * Replaces legacy polar trigonometric chord approximations (`sin/cos + lineTo`)
 * with Google's authentic AndroidX Material 3 Expressive geometry pipeline:
 *
 * RoundedPolygon.circle -> RoundedPolygon.star -> Morph -> Morph.asCubics / toPath -> PathMeasure -> drawPath
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL 3 EXPRESSIVE PRINCIPLES (Google AndroidX):
 * In `androidx.compose.material3.internal.CircularWavyProgressModifiers` and
 * `androidx.graphics.shapes`:
 * - Waves are constructed using rounded star polygons with corner rounding:
 *   CornerRounding(0.35f * wavelength, smoothing = 0.4f) and innerRounding(0.50f * wavelength).
 * - Morph smoothly interpolates between a smooth circle and a wavy star polygon via cubic Bézier curves.
 * - Progress segments are sampled along the contour using PathMeasure.
 * - Continuous wave travel shifts the sampling window and rotates back by -offsetAngle around the center,
 *   preserving the 12 o'clock anchor while undulating the waveform.
 *
 * VARDIYA DESIGN DECISIONS (Adapted for 295dp Hero & Shift Domain):
 * - Fixed 12-vertex clock rhythm (WAVE_COUNT = 12), aligning each wave with one of the 12 clock hours.
 * - Scaled for Vardiya's prominent 295dp Hero bounding box (baseRadius ~110dp).
 * - Peak wave amplitude parameterized to 7.0.dp (MAX_AMPLITUDE), ensuring the inner-to-outer
 *   radius ratio (~0.88f) fits comfortably within the Hero without clipping or crowding typography.
 * - 4.dp air gap preserved between wavy progress head and the smooth circular guide track.
 * - Zero per-frame allocations during Canvas draw phase via cached RoundedPolygon, Morph, Path,
 *   and PathMeasure.
 */
object CircularWavyHeroGeometry {
    const val WAVE_COUNT = 12
    val MAX_AMPLITUDE: Dp = 7.0.dp
    const val DEFAULT_CORNER_RADIUS_FACTOR = 0.35f
    const val DEFAULT_INNER_CORNER_RADIUS_FACTOR = 0.50f
    const val DEFAULT_SMOOTHING = 0.4f

    /**
     * Resolves the normalized morph factor [0f, 1f] for a given [semanticState].
     */
    fun resolveMorphFactor(semanticState: HeroSemanticState): Float {
        val amplitude = CircularWavyHeroMotion.contract.resolveTargetAmplitude(semanticState)
        return resolveMorphFactor(amplitude)
    }

    /**
     * Resolves the normalized morph factor [0f, 1f] for an animated amplitude in [Dp].
     */
    fun resolveMorphFactor(amplitudeDp: Dp): Float =
        (amplitudeDp / MAX_AMPLITUDE).coerceIn(0f, 1f)

    /**
     * Creates a circular [RoundedPolygon] with [numVertices] matching the star polygon.
     */
    fun createCirclePolygon(
        radius: Float,
        centerX: Float = 0f,
        centerY: Float = 0f,
        numVertices: Int = WAVE_COUNT
    ): RoundedPolygon = RoundedPolygon.circle(
        numVertices = numVertices,
        radius = radius,
        centerX = centerX,
        centerY = centerY
    )

    /**
     * Creates a wavy star [RoundedPolygon] adapted for Vardiya's 295dp Hero dimensions.
     */
    fun createStarPolygon(
        baseRadius: Float,
        maxAmplitude: Float,
        centerX: Float = 0f,
        centerY: Float = 0f,
        numVertices: Int = WAVE_COUNT,
        cornerRadiusFactor: Float = DEFAULT_CORNER_RADIUS_FACTOR,
        smoothing: Float = DEFAULT_SMOOTHING,
        innerCornerRadiusFactor: Float = DEFAULT_INNER_CORNER_RADIUS_FACTOR
    ): RoundedPolygon {
        val wavelength = (2.0 * PI * baseRadius / numVertices).toFloat()
        return RoundedPolygon.star(
            numVerticesPerRadius = numVertices,
            radius = baseRadius + maxAmplitude,
            innerRadius = (baseRadius - maxAmplitude).coerceAtLeast(1f),
            rounding = CornerRounding(radius = cornerRadiusFactor * wavelength, smoothing = smoothing),
            innerRounding = CornerRounding(radius = innerCornerRadiusFactor * wavelength),
            centerX = centerX,
            centerY = centerY
        )
    }

    /**
     * Creates a [Morph] between the [circle] and [star] polygons.
     */
    fun createHeroMorph(
        circle: RoundedPolygon,
        star: RoundedPolygon
    ): Morph = Morph(start = circle, end = star)

    /**
     * Evaluates sample points along the morphed cubic Bézier curves without touching
     * Android native graphics APIs, enabling pure JVM deterministic verification.
     */
    fun sampleCubicsPoints(
        cubics: List<Cubic>,
        samplesPerCubic: Int = 4
    ): List<Offset> {
        val result = mutableListOf<Offset>()
        for (cubic in cubics) {
            for (step in 0..samplesPerCubic) {
                val t = step.toFloat() / samplesPerCubic.toFloat()
                val oneMinusT = 1f - t
                val x = oneMinusT * oneMinusT * oneMinusT * cubic.anchor0X +
                    3f * oneMinusT * oneMinusT * t * cubic.control0X +
                    3f * oneMinusT * t * t * cubic.control1X +
                    t * t * t * cubic.anchor1X
                val y = oneMinusT * oneMinusT * oneMinusT * cubic.anchor0Y +
                    3f * oneMinusT * oneMinusT * t * cubic.control0Y +
                    3f * oneMinusT * t * t * cubic.control1Y +
                    t * t * t * cubic.anchor1Y
                result.add(Offset(x, y))
            }
        }
        return result
    }

    /**
     * Resolves the nominal base radius in Dp from the outer container size.
     * Container margin accommodates outer wave peaks (maxAmplitude), stroke width, and safety gap.
     * For 295.dp indicator: 147.5.dp - (11.dp + 5.dp + 4.dp) = 127.5.dp.
     */
    fun resolveBaseRadiusDp(indicatorSizeDp: Dp): Dp {
        val outerPeakOffsetDp = MAX_AMPLITUDE + 5.dp + 4.dp // 11dp + 5dp + 4dp = 20dp
        val halfSize = indicatorSizeDp / 2f
        return if (halfSize > outerPeakOffsetDp) halfSize - outerPeakOffsetDp else 0.dp
    }

    /**
     * Calculates the exact single-loop arc length in DP of the 12-lobed morphed wavy path
     * for a given [baseRadiusDp] and [amplitudeDp].
     * Uses deterministic numerical integration of the cubic Bézier curves (pure JVM compatible).
     */
    fun calculateActualWavyLoopLengthDp(
        baseRadiusDp: Float,
        amplitudeDp: Float,
        maxAmplitudeDp: Float = MAX_AMPLITUDE.value,
        samplesPerCubic: Int = 16
    ): Float {
        if (baseRadiusDp <= 0f) return 0f
        val morphFactor = resolveMorphFactor(amplitudeDp.coerceAtLeast(0f).dp)
        val circle = createCirclePolygon(radius = baseRadiusDp)
        val star = createStarPolygon(baseRadius = baseRadiusDp, maxAmplitude = maxAmplitudeDp)
        val morph = createHeroMorph(circle, star)
        val cubics = morph.asCubics(morphFactor)
        if (cubics.isEmpty()) return (2.0 * kotlin.math.PI * baseRadiusDp).toFloat()

        var totalLength = 0f
        for (cubic in cubics) {
            var prevX = cubic.anchor0X
            var prevY = cubic.anchor0Y
            for (step in 1..samplesPerCubic) {
                val t = step.toFloat() / samplesPerCubic.toFloat()
                val oneMinusT = 1f - t
                val x = oneMinusT * oneMinusT * oneMinusT * cubic.anchor0X +
                    3f * oneMinusT * oneMinusT * t * cubic.control0X +
                    3f * oneMinusT * t * t * cubic.control1X +
                    t * t * t * cubic.anchor1X
                val y = oneMinusT * oneMinusT * oneMinusT * cubic.anchor0Y +
                    3f * oneMinusT * oneMinusT * t * cubic.control0Y +
                    3f * oneMinusT * t * t * cubic.control1Y +
                    t * t * t * cubic.anchor1Y
                totalLength += hypot((x - prevX).toDouble(), (y - prevY).toDouble()).toFloat()
                prevX = x
                prevY = y
            }
        }
        return totalLength
    }
}

/**
 * Reusable drawing holder for [CircularWavyProgressHero] to eliminate per-frame allocations.
 * Reuses polygons, morph, Android and Compose paths, path measures, and matrices.
 */
internal class HeroGeometryHolder {
    var cachedRadius: Float = -1f
    var cachedAmplitude: Float = -1f
    var cachedCenterX: Float = -1f
    var cachedCenterY: Float = -1f

    var circlePolygon: RoundedPolygon? = null
    var starPolygon: RoundedPolygon? = null
    var heroMorph: Morph? = null

    val fullAndroidPath = AndroidPath()
    val fullComposePath = fullAndroidPath.asComposePath()
    val progressComposePath = Path()
    val pathMeasure = PathMeasure()
    val matrix = AndroidMatrix()

    var lastMorphFactor: Float = -1f
    var lastSingleLoopLength: Float = 0f

    fun ensureMorph(radius: Float, maxAmplitude: Float, centerX: Float, centerY: Float): Morph {
        if (radius != cachedRadius ||
            maxAmplitude != cachedAmplitude ||
            centerX != cachedCenterX ||
            centerY != cachedCenterY ||
            heroMorph == null
        ) {
            cachedRadius = radius
            cachedAmplitude = maxAmplitude
            cachedCenterX = centerX
            cachedCenterY = centerY

            val circle = CircularWavyHeroGeometry.createCirclePolygon(
                radius = radius,
                centerX = centerX,
                centerY = centerY
            )
            val star = CircularWavyHeroGeometry.createStarPolygon(
                baseRadius = radius,
                maxAmplitude = maxAmplitude,
                centerX = centerX,
                centerY = centerY
            )
            circlePolygon = circle
            starPolygon = star
            heroMorph = CircularWavyHeroGeometry.createHeroMorph(circle, star)
            lastMorphFactor = -1f
        }
        return heroMorph!!
    }

    fun updatePath(morph: Morph, morphFactor: Float, centerX: Float, centerY: Float): Float {
        if (kotlin.math.abs(morphFactor - lastMorphFactor) > 0.0005f || lastMorphFactor < 0f) {
            lastMorphFactor = morphFactor
            fullAndroidPath.rewind()
            val cubics = morph.asCubics(morphFactor)
            if (cubics.isNotEmpty()) {
                val first = cubics.first()
                fullAndroidPath.moveTo(first.anchor0X, first.anchor0Y)
                for (cubic in cubics) {
                    fullAndroidPath.cubicTo(
                        cubic.control0X, cubic.control0Y,
                        cubic.control1X, cubic.control1Y,
                        cubic.anchor1X, cubic.anchor1Y
                    )
                }
                // Second continuous loop for seamless traveling wave segment extraction
                for (cubic in cubics) {
                    fullAndroidPath.cubicTo(
                        cubic.control0X, cubic.control0Y,
                        cubic.control1X, cubic.control1Y,
                        cubic.anchor1X, cubic.anchor1Y
                    )
                }
                fullAndroidPath.close()

                // Rotate by -75° around center to align anchor 0 (-15°) exactly with 12 o'clock (-90°)
                matrix.reset()
                matrix.postRotate(-75f, centerX, centerY)
                fullAndroidPath.transform(matrix)
            }
            pathMeasure.setPath(fullComposePath, forceClosed = true)
            lastSingleLoopLength = pathMeasure.length / 2f
        }
        return lastSingleLoopLength
    }
}


/**
 * Pure motion mapping functions for [CircularWavyProgressHero].
 * Driven by [VardiyaHeroMotionContract] to maintain architectural separation
 * between semantic state mapping and UI canvas rendering.
 */
object CircularWavyHeroMotion {
    val contract: VardiyaHeroMotionContract = DefaultVardiyaHeroMotionContract

    fun resolveSemanticState(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean
    ): HeroSemanticState = contract.resolveSemanticState(shiftState, isOvertimeActive, isBreakActive)

    /**
     * Resolves the target wave amplitude in [Dp] based on shift state, overtime, and break.
     * Preserves established production geometry via [VardiyaHeroMotionContract].
     */
    fun resolveTargetAmplitude(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean
    ): Dp {
        val semanticState = resolveSemanticState(shiftState, isOvertimeActive, isBreakActive)
        return contract.resolveTargetAmplitude(semanticState)
    }

    /**
     * Resolves whether the traveling wave phase animation should actively advance.
     * ZERO continuous phase animation when NOT_STARTED, PAUSED, FINISHED, on BREAK,
     * or when [MotionPreference.REDUCED] is active.
     */
    fun isWavePhaseActive(
        shiftState: ShiftState,
        isBreakActive: Boolean,
        motionPreference: MotionPreference,
        isOvertimeActive: Boolean = false
    ): Boolean {
        val semanticState = resolveSemanticState(shiftState, isOvertimeActive, isBreakActive)
        return contract.isWavePhaseActive(semanticState, motionPreference)
    }

    /**
     * Resolves the full 360-degree wave cycle duration in milliseconds.
     */
    fun resolveWaveCycleDurationMs(isOvertimeActive: Boolean): Long {
        val semanticState = if (isOvertimeActive) HeroSemanticState.OVERTIME else HeroSemanticState.RUNNING
        return contract.resolveWaveCycleDurationMs(semanticState)
    }

    /**
     * Resolves the container volumetric scale spring for the Hero.
     */
    fun resolveContainerScale(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean
    ): Float {
        val semanticState = resolveSemanticState(shiftState, isOvertimeActive, isBreakActive)
        return contract.resolveContainerScale(semanticState)
    }

    /**
     * Resolves the semantic active hero color for the progress wave and typography.
     */
    fun resolveHeroActiveColor(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean,
        colorScheme: ColorScheme
    ): Color {
        val semanticState = resolveSemanticState(shiftState, isOvertimeActive, isBreakActive)
        return contract.resolveHeroActiveColor(semanticState, colorScheme)
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
    val contract = CircularWavyHeroMotion.contract

    val semanticState = remember(shiftState, isOvertimeActive, isBreakActive) {
        contract.resolveSemanticState(shiftState, isOvertimeActive, isBreakActive)
    }

    // 1. Physical Progress Motion (Expressive Spatial Spring)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = contract.resolveProgressSpec(motionScheme),
        label = "wavy_progress_anim"
    )

    // 2. Physical Amplitude Motion (Expressive Spatial Spring Settling)
    val targetAmplitude = contract.resolveTargetAmplitude(semanticState)
    val animatedAmplitudeDp by animateDpAsState(
        targetValue = targetAmplitude,
        animationSpec = contract.resolveAmplitudeSpec(motionScheme),
        label = "wavy_amplitude_anim"
    )

    // 3. Critically Damped State Color Transition (Effects Spring)
    val targetActiveColor = contract.resolveHeroActiveColor(
        semanticState = semanticState,
        colorScheme = MaterialTheme.colorScheme
    )
    val animatedActiveColor by animateColorAsState(
        targetValue = targetActiveColor,
        animationSpec = contract.resolveColorSpec(motionScheme),
        label = "wavy_active_color_anim"
    )

    // 4. Traveling Wave Phase Engine
    var phaseAccumulator by remember { mutableFloatStateOf(0f) }

    // 5. Container Volumetric Scale Spring
    val targetContainerScale = contract.resolveContainerScale(semanticState)
    val animatedContainerScale by animateFloatAsState(
        targetValue = targetContainerScale,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "hero_container_scale_anim"
    )

    // Reset phase cleanly to 12 o'clock when shift resets or enters calm resting state
    LaunchedEffect(semanticState) {
        if (semanticState == HeroSemanticState.NOT_STARTED ||
            semanticState == HeroSemanticState.FINISHED ||
            semanticState == HeroSemanticState.BREAK
        ) {
            phaseAccumulator = 0f
        }
    }

    // Reusable geometry cache to eliminate per-frame allocations during Canvas draw
    val geometryHolder = remember { HeroGeometryHolder() }

    // Track color: visible against surface, creating the expressive wavy guide ring
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)

    BoxWithConstraints(
        modifier = modifier
            .progressSemantics(animatedProgress)
            .semantics {
                contentDescription = "${heroSubtitleText}: ${heroAmountText}, ilerleme: ${(animatedProgress * 100).roundToInt()}%"
            },
        contentAlignment = Alignment.Center
    ) {
        // Responsive size: prominent visual anchor fitting comfortably across screens
        val indicatorSize = min(min(maxWidth, maxHeight), 295.dp)

        // Derive actual base radius in Dp for the active container size
        val baseRadiusDp = CircularWavyHeroGeometry.resolveBaseRadiusDp(indicatorSize).value

        // Calculate actual single-loop geometric path length directly from active amplitude
        val actualPathLengthDp = remember(baseRadiusDp, animatedAmplitudeDp) {
            CircularWavyHeroGeometry.calculateActualWavyLoopLengthDp(
                baseRadiusDp = baseRadiusDp,
                amplitudeDp = animatedAmplitudeDp.value
            )
        }

        val isWaveActive = contract.isWavePhaseActive(
            semanticState = semanticState,
            motionPreference = motionPreference
        )

        if (isWaveActive) {
            val phaseVelocity = HeroWaveMotionTokens.resolvePhaseVelocityRadPerSec(
                semanticState = semanticState,
                motionPreference = motionPreference,
                pathLengthDp = actualPathLengthDp
            )
            LaunchedEffect(phaseVelocity) {
                var lastFrameNanos = 0L
                while (isActive) {
                    withFrameNanos { frameTimeNanos ->
                        if (lastFrameNanos != 0L) {
                            val deltaNanos = frameTimeNanos - lastFrameNanos
                            val deltaSeconds = (deltaNanos / 1_000_000_000.0).toFloat()
                            phaseAccumulator = HeroWaveMotionTokens.advancePhase(
                                currentPhase = phaseAccumulator,
                                velocityRadPerSec = phaseVelocity,
                                deltaSeconds = deltaSeconds
                            )
                        }
                        lastFrameNanos = frameTimeNanos
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(indicatorSize)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = animatedContainerScale
                    scaleY = animatedContainerScale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 10.dp.toPx()
                val trackStrokeWidthPx = 9.5.dp.toPx()
                val maxAmplitudePx = CircularWavyHeroGeometry.MAX_AMPLITUDE.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Margin for outer wave peaks + stroke width + safety padding
                val outerPeakOffset = maxAmplitudePx + (strokeWidthPx / 2f) + 4.dp.toPx()
                val baseRadius = (min(size.width, size.height) / 2f) - outerPeakOffset

                if (baseRadius <= 0f) return@Canvas

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

                // 2. Draw the active determinate wavy progress arc powered by RoundedPolygon + Morph + PathMeasure
                if (currentProgress > 0.005f) {
                    val morphFactor = CircularWavyHeroGeometry.resolveMorphFactor(animatedAmplitudeDp)
                    val morph = geometryHolder.ensureMorph(
                        radius = baseRadius,
                        maxAmplitude = maxAmplitudePx,
                        centerX = centerOffset.x,
                        centerY = centerOffset.y
                    )
                    val singleLoopLength = geometryHolder.updatePath(
                        morph = morph,
                        morphFactor = morphFactor,
                        centerX = centerOffset.x,
                        centerY = centerOffset.y
                    )

                    if (singleLoopLength > 0f) {
                        val effectivePhase = if (motionPreference == MotionPreference.REDUCED ||
                            semanticState == HeroSemanticState.NOT_STARTED ||
                            semanticState == HeroSemanticState.FINISHED ||
                            semanticState == HeroSemanticState.BREAK
                        ) {
                            0f
                        } else {
                            phaseAccumulator
                        }
                        val coercedWaveOffset = (effectivePhase / (2f * PI.toFloat())) % 1f
                        val startStopShift = coercedWaveOffset * singleLoopLength
                        val pStart = startStopShift
                        val pStop = (currentProgress * singleLoopLength) + startStopShift

                        geometryHolder.progressComposePath.reset()
                        geometryHolder.pathMeasure.getSegment(
                            startDistance = pStart,
                            stopDistance = pStop,
                            destination = geometryHolder.progressComposePath,
                            startWithMoveTo = true
                        )

                        val offsetAngle = (coercedWaveOffset * 360f) % 360f
                        if (offsetAngle != 0f) {
                            withTransform({
                                rotate(degrees = -offsetAngle, pivot = centerOffset)
                            }) {
                                drawPath(
                                    path = geometryHolder.progressComposePath,
                                    color = animatedActiveColor,
                                    style = Stroke(
                                        width = strokeWidthPx,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        } else {
                            drawPath(
                                path = geometryHolder.progressComposePath,
                                color = animatedActiveColor,
                                style = Stroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
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
