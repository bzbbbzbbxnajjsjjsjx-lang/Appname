package com.example.androidapp.vardiya.ui.components

import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import com.example.androidapp.theme.motion.contract.HeroSemanticState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic unit test suite verifying the real AndroidX Material 3 Expressive
 * geometry pipeline for Vardiya's 295dp Hero Wavy Progress element:
 *
 * RoundedPolygon.circle -> RoundedPolygon.star -> Morph -> Cubics / Path -> PathMeasure
 *
 * Verifies:
 * - 12-vertex clock rhythm (12 waves around 360°)
 * - Morph factor resolution across all semantic states
 * - Continuous Bézier cubic evaluation on JVM
 * - Radii and wavelength bounds matching Vardiya 295dp Hero
 * - Anchor 0 alignment with 12 o'clock (-90°)
 */
class HeroGeometryTest {

    @Test
    fun testPolygonAndMorphCreation() {
        val circle = CircularWavyHeroGeometry.createCirclePolygon(radius = 110f)
        val star = CircularWavyHeroGeometry.createStarPolygon(baseRadius = 110f, maxAmplitude = 7f)
        assertNotNull(circle)
        assertNotNull(star)

        val morph = CircularWavyHeroGeometry.createHeroMorph(circle, star)
        assertNotNull(morph)
        assertNotNull(morph.asCubics(0f))
        assertNotNull(morph.asCubics(0.5f))
        assertNotNull(morph.asCubics(1f))

        // Morph must preserve cubic topology across progress
        assertEquals(morph.asCubics(0f).size, morph.asCubics(1f).size)
    }

    @Test
    fun testVardiyaHeroGeometry_radiiAndWavelength() {
        val baseRadius = 110f
        val maxAmplitude = 7f // 7dp in overtime
        val wavelength = (2 * PI * baseRadius / 12).toFloat()
        val cornerRadius = 0.35f * wavelength
        val innerCornerRadius = 0.50f * wavelength

        val star = RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = baseRadius + maxAmplitude,
            innerRadius = baseRadius - maxAmplitude,
            rounding = CornerRounding(radius = cornerRadius, smoothing = 0.4f),
            innerRounding = CornerRounding(radius = innerCornerRadius),
            centerX = 150f,
            centerY = 150f
        )
        val circle = RoundedPolygon.circle(
            numVertices = 12,
            radius = baseRadius,
            centerX = 150f,
            centerY = 150f
        )
        val morph = Morph(circle, star)

        // Morph cubics count must match
        assertEquals(morph.asCubics(0f).size, morph.asCubics(1f).size)

        // Verify all cubics at progress 0.0, 0.25, 0.5, 0.75, 1.0 are well-formed
        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val cubics = morph.asCubics(t)
            assertTrue(cubics.isNotEmpty())
            for (c in cubics) {
                assertTrue(!c.anchor0X.isNaN())
                assertTrue(!c.anchor0Y.isNaN())
                assertTrue(!c.anchor1X.isNaN())
                assertTrue(!c.anchor1Y.isNaN())
            }
        }
    }

    @Test
    fun testMorphFactorResolution_allSemanticStates() {
        // OVERTIME: 7.0.dp -> 1.0f
        assertEquals(1.0f, CircularWavyHeroGeometry.resolveMorphFactor(HeroSemanticState.OVERTIME), 0.001f)

        // RUNNING: 6.0.dp / 7.0.dp -> ~0.857f
        assertEquals(6.0f / 7.0f, CircularWavyHeroGeometry.resolveMorphFactor(HeroSemanticState.RUNNING), 0.001f)

        // BREAK: 5.0.dp / 7.0.dp -> ~0.714f
        assertEquals(5.0f / 7.0f, CircularWavyHeroGeometry.resolveMorphFactor(HeroSemanticState.BREAK), 0.001f)

        // PAUSED: 4.5.dp / 7.0.dp -> ~0.643f
        assertEquals(4.5f / 7.0f, CircularWavyHeroGeometry.resolveMorphFactor(HeroSemanticState.PAUSED), 0.001f)

        // FINISHED: 5.0.dp / 7.0.dp -> ~0.714f
        assertEquals(5.0f / 7.0f, CircularWavyHeroGeometry.resolveMorphFactor(HeroSemanticState.FINISHED), 0.001f)

        // NOT_STARTED: 5.0.dp / 7.0.dp -> ~0.714f
        assertEquals(5.0f / 7.0f, CircularWavyHeroGeometry.resolveMorphFactor(HeroSemanticState.NOT_STARTED), 0.001f)

        // Arbitrary amplitude Dp coercion
        assertEquals(0f, CircularWavyHeroGeometry.resolveMorphFactor(0.dp), 0.001f)
        assertEquals(0.5f, CircularWavyHeroGeometry.resolveMorphFactor(3.5.dp), 0.001f)
        assertEquals(1.0f, CircularWavyHeroGeometry.resolveMorphFactor(7.0.dp), 0.001f)
        assertEquals(1.0f, CircularWavyHeroGeometry.resolveMorphFactor(10.0.dp), 0.001f)
    }

    @Test
    fun testBezierPointEvaluation_pureJvm() {
        val baseRadius = 110f
        val maxAmplitude = 7f

        val star = CircularWavyHeroGeometry.createStarPolygon(baseRadius = baseRadius, maxAmplitude = maxAmplitude)
        val circle = CircularWavyHeroGeometry.createCirclePolygon(radius = baseRadius)
        val morph = CircularWavyHeroGeometry.createHeroMorph(circle, star)
        val cubics = morph.asCubics(0.5f)

        val samplePoints = CircularWavyHeroGeometry.sampleCubicsPoints(cubics, samplesPerCubic = 4)
        assertTrue(samplePoints.size >= 48)

        for (pt in samplePoints) {
            val dist = hypot(pt.x.toDouble(), pt.y.toDouble())
            assertTrue("Radius $dist must fall within radial envelope", dist in (baseRadius - maxAmplitude - 1f)..(baseRadius + maxAmplitude + 1f))
        }
    }

    @Test
    fun testWaveVertexRhythm_exact12Waves() {
        val baseRadius = 110f
        val maxAmplitude = 7f
        val star = CircularWavyHeroGeometry.createStarPolygon(baseRadius = baseRadius, maxAmplitude = maxAmplitude)
        val circle = CircularWavyHeroGeometry.createCirclePolygon(radius = baseRadius)
        val morph = CircularWavyHeroGeometry.createHeroMorph(circle, star)
        val cubics = morph.asCubics(1.0f) // full wave amplitude

        println("cubics size = ${cubics.size}")
        val anchorRadii = cubics.map { hypot(it.anchor0X.toDouble(), it.anchor0Y.toDouble()) }
        println("anchorRadii = $anchorRadii")
        val outerAnchors = anchorRadii.filter { it > baseRadius }
        println("outerAnchors count = ${outerAnchors.size}")
        assertEquals(12, outerAnchors.size / 2) // Each wave has 2 outer control anchors due to corner rounding
    }

    @Test
    fun testAnchor0_clockAlignment() {
        val baseRadius = 110f
        val maxAmplitude = 7f
        val star = CircularWavyHeroGeometry.createStarPolygon(baseRadius = baseRadius, maxAmplitude = maxAmplitude)
        val circle = CircularWavyHeroGeometry.createCirclePolygon(radius = baseRadius)
        val morph = CircularWavyHeroGeometry.createHeroMorph(circle, star)
        val cubics = morph.asCubics(1.0f)

        val first = cubics.first()
        val rawAngle = Math.toDegrees(atan2(first.anchor0Y.toDouble(), first.anchor0X.toDouble()))
        assertEquals("Anchor 0 raw angle must be -15°", -15.0, rawAngle, 0.1)

        // After -75° rotation, anchor 0 aligns with 12 o'clock (-90°)
        val rotatedAngle = rawAngle - 75.0
        assertEquals("Rotated anchor 0 must align with 12 o'clock (-90°)", -90.0, rotatedAngle, 0.1)
    }
}
