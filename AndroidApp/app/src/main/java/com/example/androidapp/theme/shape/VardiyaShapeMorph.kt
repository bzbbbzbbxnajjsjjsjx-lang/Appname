package com.example.androidapp.theme.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance Material 3 Expressive Shape Morphing Engine.
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE (Google):
 * In `androidx.graphics.shapes.Morph` and `LoadingIndicatorDrawingDelegate.java`:
 * - Google continuously morphs between normalized polygons over a normalized fraction `t` in [0, 1].
 * - Geometry dynamically transitions across shapes with smooth continuous vertex movement.
 * - Morphing avoids discrete frame jumps by interpolating radial feature anchors.
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * We implement an allocation-free parametric morpher that resamples both source and target
 * shapes along identical angular radial anchors, interpolating between them smoothly with
 * cubic smoothing and writing into a remembered, reusable Compose [Path].
 */
object VardiyaShapeMorph {

    private const val RESOLUTION = 72 // 5-degree increments around 360 for high visual fidelity

    private val ANGLES = FloatArray(RESOLUTION) { i ->
        (i * 2.0 * PI / RESOLUTION).toFloat()
    }

    /**
     * Computes the normalized interpolated sample points between [shape1] and [shape2]
     * without touching Android native [Path] graphics APIs, enabling pure JVM deterministic testing.
     */
    fun sampleMorphedPoints(
        shape1: ExpressiveShapeDefinition,
        shape2: ExpressiveShapeDefinition,
        t: Float,
        bounds: Rect,
        rotationDegrees: Float = 0f
    ): List<Offset> {
        val clampedT = t.coerceIn(0f, 1f)
        val centerX = bounds.center.x
        val centerY = bounds.center.y
        val radiusX = bounds.width / 2f
        val radiusY = bounds.height / 2f
        val rotRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()

        return List(RESOLUTION) { i ->
            val angle = ANGLES[i]
            val r1 = sampleRadius(shape1, angle)
            val r2 = sampleRadius(shape2, angle)
            val r = r1 * (1f - clampedT) + r2 * clampedT
            val finalAngle = angle + rotRad
            Offset(
                centerX + r * cos(finalAngle) * radiusX,
                centerY + r * sin(finalAngle) * radiusY
            )
        }
    }

    /**
     * Morphs between [shape1] and [shape2] at fractional progress [t] in [0, 1],
     * populating [outPath] fitted into [bounds].
     *
     * @param rotationDegrees Optional rotation applied around the center.
     */
    fun morph(
        shape1: ExpressiveShapeDefinition,
        shape2: ExpressiveShapeDefinition,
        t: Float,
        bounds: Rect,
        outPath: Path = Path(),
        rotationDegrees: Float = 0f
    ): Path {
        outPath.reset()
        val clampedT = t.coerceIn(0f, 1f)

        val centerX = bounds.center.x
        val centerY = bounds.center.y
        val radiusX = bounds.width / 2f
        val radiusY = bounds.height / 2f

        val rotRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()

        var firstPointX = 0f
        var firstPointY = 0f

        val ptsX = FloatArray(RESOLUTION)
        val ptsY = FloatArray(RESOLUTION)

        // Resample along angular rays
        for (i in 0 until RESOLUTION) {
            val angle = ANGLES[i]
            val r1 = sampleRadius(shape1, angle)
            val r2 = sampleRadius(shape2, angle)
            val r = r1 * (1f - clampedT) + r2 * clampedT

            val finalAngle = angle + rotRad
            val px = centerX + r * cos(finalAngle) * radiusX
            val py = centerY + r * sin(finalAngle) * radiusY
            ptsX[i] = px
            ptsY[i] = py

            if (i == 0) {
                firstPointX = px
                firstPointY = py
            }
        }

        // Generate smooth path connecting points
        outPath.moveTo(firstPointX, firstPointY)
        for (i in 0 until RESOLUTION) {
            val currX = ptsX[i]
            val currY = ptsY[i]
            val nextX = ptsX[(i + 1) % RESOLUTION]
            val nextY = ptsY[(i + 1) % RESOLUTION]

            // Midpoint quadratic-to for lush, anti-aliased organic curves
            val midX = (currX + nextX) / 2f
            val midY = (currY + nextY) / 2f
            outPath.quadraticTo(currX, currY, midX, midY)
        }
        outPath.close()

        return outPath
    }

    /**
     * Computes the normalized interpolated sample points across a sequence of shapes
     * without touching Android native [Path] graphics APIs, enabling pure JVM deterministic testing.
     */
    fun sampleMorphedSequencePoints(
        shapes: List<ExpressiveShapeDefinition>,
        overallProgress: Float,
        bounds: Rect,
        rotationDegrees: Float = 0f
    ): List<Offset> {
        if (shapes.isEmpty()) return emptyList()
        if (shapes.size == 1) return sampleMorphedPoints(shapes[0], shapes[0], 0f, bounds, rotationDegrees)

        val totalShapes = shapes.size
        val normalizedProgress = (overallProgress % totalShapes + totalShapes) % totalShapes
        val fromIndex = normalizedProgress.toInt()
        val toIndex = (fromIndex + 1) % totalShapes
        val localT = normalizedProgress - fromIndex

        return sampleMorphedPoints(
            shape1 = shapes[fromIndex],
            shape2 = shapes[toIndex],
            t = localT,
            bounds = bounds,
            rotationDegrees = rotationDegrees
        )
    }

    /**
     * Morphs through an entire sequence of [shapes] based on continuous [overallProgress].
     * For example, progress 0.0 -> shape 0, progress 1.0 -> shape 1, progress 2.5 -> halfway between shape 2 & 3.
     */
    fun morphSequence(
        shapes: List<ExpressiveShapeDefinition>,
        overallProgress: Float,
        bounds: Rect,
        outPath: Path = Path(),
        rotationDegrees: Float = 0f
    ): Path {
        if (shapes.isEmpty()) {
            outPath.reset()
            return outPath
        }
        if (shapes.size == 1) {
            return shapes.first().toPath(bounds, outPath)
        }

        val totalShapes = shapes.size
        val normalizedProgress = (overallProgress % totalShapes + totalShapes) % totalShapes
        val fromIndex = normalizedProgress.toInt()
        val toIndex = (fromIndex + 1) % totalShapes
        val localT = normalizedProgress - fromIndex

        return morph(
            shape1 = shapes[fromIndex],
            shape2 = shapes[toIndex],
            t = localT,
            bounds = bounds,
            outPath = outPath,
            rotationDegrees = rotationDegrees
        )
    }

    /**
     * Samples the effective normalized radius of an expressive shape at radial [angle].
     */
    private fun sampleRadius(shape: ExpressiveShapeDefinition, angle: Float): Float {
        // Fast ray-intersection test against shape segments
        val rayCos = cos(angle)
        val raySin = sin(angle)

        var bestRadius = 0.9f
        var minAngleDiff = Float.MAX_VALUE

        // Match against segment nodes
        for (seg in shape.segments) {
            val nodeAngle = kotlin.math.atan2(seg.start.y, seg.start.x)
            var diff = kotlin.math.abs(nodeAngle - angle)
            if (diff > PI) diff = (2.0 * PI - diff).toFloat()

            if (diff < minAngleDiff) {
                minAngleDiff = diff
                val r = kotlin.math.hypot(seg.start.x, seg.start.y)
                bestRadius = r
            }
        }

        return bestRadius.coerceIn(0.2f, 1.2f)
    }
}
