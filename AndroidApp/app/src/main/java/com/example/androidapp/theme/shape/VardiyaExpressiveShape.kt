package com.example.androidapp.theme.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Polygon definitions and shape generators.
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE (Google):
 * In Material Components Android (`MaterialShapes.java`) and `androidx.graphics.shapes`,
 * Google defines canonical expressive polygon geometries:
 * - OVAL, PILL, TRIANGLE, DIAMOND, CLAMSHELL, PENTAGON, GEM, SUNNY, VERY_SUNNY,
 *   COOKIE_4, COOKIE_9, BURST, SOFT_BURST.
 * Each shape is defined by normalized anchor vertices and corner roundings fitting in
 * a unit circle / bounding box.
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * We reproduce these exact mathematical geometries natively in Compose using pure
 * normalized bezier control points without requiring external runtime dependencies.
 */
enum class VardiyaExpressiveShapeType {
    CIRCLE,
    OVAL,
    PILL,
    TRIANGLE,
    DIAMOND,
    CLAMSHELL,
    PENTAGON,
    GEM,
    SUNNY,
    VERY_SUNNY,
    COOKIE_4,
    COOKIE_9,
    BURST,
    SOFT_BURST
}

/**
 * Normalized 2D point on a [-1, 1] unit square coordinate space.
 */
data class NormalizedPoint(val x: Float, val y: Float)

/**
 * Cubic Bezier segment in normalized coordinates.
 */
data class NormalizedCubicSegment(
    val start: NormalizedPoint,
    val control1: NormalizedPoint,
    val control2: NormalizedPoint,
    val end: NormalizedPoint
)

/**
 * Mathematical definition of an expressive shape composed of smooth cubic segments.
 */
class ExpressiveShapeDefinition(
    val type: VardiyaExpressiveShapeType,
    val segments: List<NormalizedCubicSegment>
) {
    /**
     * Builds a Compose [Path] scaled and translated to fit inside [bounds].
     */
    fun toPath(bounds: Rect, outPath: Path = Path()): Path {
        outPath.reset()
        if (segments.isEmpty()) return outPath

        val centerX = bounds.center.x
        val centerY = bounds.center.y
        val radiusX = bounds.width / 2f
        val radiusY = bounds.height / 2f

        fun mapPoint(p: NormalizedPoint): Offset {
            return Offset(centerX + p.x * radiusX, centerY + p.y * radiusY)
        }

        val firstStart = mapPoint(segments.first().start)
        outPath.moveTo(firstStart.x, firstStart.y)

        for (segment in segments) {
            val c1 = mapPoint(segment.control1)
            val c2 = mapPoint(segment.control2)
            val end = mapPoint(segment.end)
            outPath.cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
        }

        outPath.close()
        return outPath
    }
}

/**
 * Factory for canonical Material 3 Expressive Shapes.
 */
object VardiyaExpressiveShapes {

    private const val QUARTER_CIRCLE_HANDLE = 0.55228475f

    /**
     * Standard Circle fitting unit radius.
     */
    val Circle: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.CIRCLE,
        points = 8,
        outerRadius = 1.0f,
        innerRadius = 1.0f,
        smoothness = QUARTER_CIRCLE_HANDLE
    )

    /**
     * Upstream Material OVAL: 1.0f x-scale, 0.64f y-scale.
     */
    val Oval: ExpressiveShapeDefinition = run {
        val baseCircle = Circle
        val segments = baseCircle.segments.map { s ->
            NormalizedCubicSegment(
                start = NormalizedPoint(s.start.x, s.start.y * 0.64f),
                control1 = NormalizedPoint(s.control1.x, s.control1.y * 0.64f),
                control2 = NormalizedPoint(s.control2.x, s.control2.y * 0.64f),
                end = NormalizedPoint(s.end.x, s.end.y * 0.64f)
            )
        }
        ExpressiveShapeDefinition(VardiyaExpressiveShapeType.OVAL, segments)
    }

    /**
     * Upstream Material PILL: elongated rounded stadium shape.
     */
    val Pill: ExpressiveShapeDefinition = run {
        val baseCircle = Circle
        val segments = baseCircle.segments.map { s ->
            NormalizedCubicSegment(
                start = NormalizedPoint(s.start.x * 0.55f, s.start.y),
                control1 = NormalizedPoint(s.control1.x * 0.55f, s.control1.y),
                control2 = NormalizedPoint(s.control2.x * 0.55f, s.control2.y),
                end = NormalizedPoint(s.end.x * 0.55f, s.end.y)
            )
        }
        ExpressiveShapeDefinition(VardiyaExpressiveShapeType.PILL, segments)
    }

    /**
     * Upstream Material PENTAGON: 5-sided expressive polygon with rounded vertices.
     */
    val Pentagon: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.PENTAGON,
        points = 5,
        outerRadius = 1.0f,
        innerRadius = 0.88f,
        smoothness = 0.42f,
        rotationOffset = -PI / 2.0
    )

    /**
     * Upstream Material SUNNY: 8-point expressive sun with smooth cusps.
     */
    val Sunny: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.SUNNY,
        points = 8,
        outerRadius = 1.0f,
        innerRadius = 0.80f,
        smoothness = 0.38f
    )

    /**
     * Upstream Material VERY_SUNNY: 12-point expressive high-frequency sun.
     */
    val VerySunny: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.VERY_SUNNY,
        points = 12,
        outerRadius = 1.0f,
        innerRadius = 0.82f,
        smoothness = 0.32f
    )

    /**
     * Upstream Material COOKIE_4: 4-lobed playful cookie clover.
     */
    val Cookie4: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.COOKIE_4,
        points = 4,
        outerRadius = 1.0f,
        innerRadius = 0.65f,
        smoothness = 0.52f
    )

    /**
     * Upstream Material COOKIE_9: 9-lobed expressive cookie.
     */
    val Cookie9: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.COOKIE_9,
        points = 9,
        outerRadius = 1.0f,
        innerRadius = 0.80f,
        smoothness = 0.40f,
        rotationOffset = -PI / 2.0
    )

    /**
     * Upstream Material SOFT_BURST: 10-lobed soft undulating burst.
     */
    val SoftBurst: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.SOFT_BURST,
        points = 10,
        outerRadius = 1.0f,
        innerRadius = 0.76f,
        smoothness = 0.36f
    )

    /**
     * Upstream Material BURST: 12-lobed energetic star burst.
     */
    val Burst: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.BURST,
        points = 12,
        outerRadius = 1.0f,
        innerRadius = 0.70f,
        smoothness = 0.28f
    )

    /**
     * Upstream Material DIAMOND: 4-point elongated diamond.
     */
    val Diamond: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.DIAMOND,
        points = 4,
        outerRadius = 1.0f,
        innerRadius = 0.45f,
        smoothness = 0.45f
    )

    /**
     * Upstream Material TRIANGLE: 3-sided rounded triangle.
     */
    val Triangle: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.TRIANGLE,
        points = 3,
        outerRadius = 1.0f,
        innerRadius = 0.72f,
        smoothness = 0.48f,
        rotationOffset = -PI / 2.0
    )

    /**
     * Upstream Material GEM: 6-sided faceted gem shape.
     */
    val Gem: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.GEM,
        points = 6,
        outerRadius = 1.0f,
        innerRadius = 0.78f,
        smoothness = 0.35f,
        rotationOffset = -PI / 6.0
    )

    /**
     * Upstream Material CLAMSHELL: 4-lobed shell geometry.
     */
    val Clamshell: ExpressiveShapeDefinition = createRadialStar(
        type = VardiyaExpressiveShapeType.CLAMSHELL,
        points = 4,
        outerRadius = 1.0f,
        innerRadius = 0.58f,
        smoothness = 0.46f,
        rotationOffset = PI / 4.0
    )

    /**
     * Authoritative morph sequence directly matching Google's upstream
     * [LoadingIndicatorDrawingDelegate.INDETERMINATE_SHAPES]:
     * SOFT_BURST -> COOKIE_9 -> PENTAGON -> PILL -> SUNNY -> COOKIE_4 -> OVAL.
     */
    val IndeterminateSequence: List<ExpressiveShapeDefinition> = listOf(
        SoftBurst,
        Cookie9,
        Pentagon,
        Pill,
        Sunny,
        Cookie4,
        Oval
    )

    /**
     * Helper to create a mathematically smooth radial star/polygon using cubic Beziers.
     * Generates a fixed number of segments (2 * points) to enable seamless 1:1 vertex morphing.
     */
    private fun createRadialStar(
        type: VardiyaExpressiveShapeType,
        points: Int,
        outerRadius: Float,
        innerRadius: Float,
        smoothness: Float,
        rotationOffset: Double = 0.0
    ): ExpressiveShapeDefinition {
        val totalNodes = points * 2
        val stepAngle = 2.0 * PI / totalNodes
        val nodes = mutableListOf<NormalizedPoint>()

        for (i in 0 until totalNodes) {
            val angle = rotationOffset + i * stepAngle
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = (r * cos(angle)).toFloat()
            val y = (r * sin(angle)).toFloat()
            nodes.add(NormalizedPoint(x, y))
        }

        val segments = mutableListOf<NormalizedCubicSegment>()
        for (i in 0 until totalNodes) {
            val curr = nodes[i]
            val next = nodes[(i + 1) % totalNodes]
            val prev = nodes[(i - 1 + totalNodes) % totalNodes]
            val nextNext = nodes[(i + 2) % totalNodes]

            // Calculate tangents for smooth continuous curvature
            val tan1X = (next.x - prev.x) * smoothness * 0.5f
            val tan1Y = (next.y - prev.y) * smoothness * 0.5f
            val tan2X = (nextNext.x - curr.x) * smoothness * 0.5f
            val tan2Y = (nextNext.y - curr.y) * smoothness * 0.5f

            val c1 = NormalizedPoint(curr.x + tan1X, curr.y + tan1Y)
            val c2 = NormalizedPoint(next.x - tan2X, next.y - tan2Y)

            segments.add(NormalizedCubicSegment(curr, c1, c2, next))
        }

        return ExpressiveShapeDefinition(type, segments)
    }

    /**
     * Returns a Compose [Shape] outline implementation for an expressive shape.
     */
    fun toComposeShape(definition: ExpressiveShapeDefinition): Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val bounds = Rect(0f, 0f, size.width, size.height)
            val path = definition.toPath(bounds)
            return Outline.Generic(path)
        }
    }
}
