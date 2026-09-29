package com.example.androidapp.theme.shape

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.theme.motion.contract.ButtonSemanticState
import com.example.androidapp.theme.motion.contract.DefaultVardiyaButtonMotionContract
import com.example.androidapp.theme.motion.contract.DefaultVardiyaHeroMotionContract
import com.example.androidapp.theme.motion.contract.DefaultVardiyaSelectionMotionContract
import com.example.androidapp.theme.motion.contract.HeroSemanticState
import com.example.androidapp.theme.motion.contract.SelectionSemanticState
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.ui.components.CircularWavyHeroMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic unit test suite verifying:
 * 1. Material 3 Expressive Shape geometries (14 canonical shapes)
 * 2. Parametric Shape Morphing engine (interpolation, continuity, bounds)
 * 3. Indeterminate sequence alignment with Google upstream
 * 4. Hero container volumetric scale spring resolution
 * 5. Tactile press and selection contracts
 */
class VardiyaExpressiveShapeTest {

    // ========================================================================
    // 1. CANONICAL SHAPE GEOMETRY AUDIT
    // ========================================================================

    @Test
    fun testAllCanonicalShapes_areDefinedAndValid() {
        val allShapes = listOf(
            VardiyaExpressiveShapes.Circle,
            VardiyaExpressiveShapes.Oval,
            VardiyaExpressiveShapes.Pill,
            VardiyaExpressiveShapes.Triangle,
            VardiyaExpressiveShapes.Diamond,
            VardiyaExpressiveShapes.Clamshell,
            VardiyaExpressiveShapes.Pentagon,
            VardiyaExpressiveShapes.Gem,
            VardiyaExpressiveShapes.Sunny,
            VardiyaExpressiveShapes.VerySunny,
            VardiyaExpressiveShapes.Cookie4,
            VardiyaExpressiveShapes.Cookie9,
            VardiyaExpressiveShapes.Burst,
            VardiyaExpressiveShapes.SoftBurst
        )

        assertEquals("Must support 14 canonical expressive shapes", 14, allShapes.size)

        for (shape in allShapes) {
            assertNotNull(shape.type)
            assertTrue("Shape ${shape.type} must contain segments", shape.segments.isNotEmpty())

            for (seg in shape.segments) {
                // Assert coordinates are normalized roughly within [-1.2, 1.2]
                assertTrue("Start x in range", seg.start.x in -1.25f..1.25f)
                assertTrue("Start y in range", seg.start.y in -1.25f..1.25f)
                assertTrue("End x in range", seg.end.x in -1.25f..1.25f)
                assertTrue("End y in range", seg.end.y in -1.25f..1.25f)
            }
        }
    }

    // ========================================================================
    // 2. INDETERMINATE SEQUENCE UPSTREAM ALIGNMENT
    // ========================================================================

    @Test
    fun testIndeterminateSequence_matchesGoogleUpstreamMaterialShapes() {
        val seq = VardiyaExpressiveShapes.IndeterminateSequence
        assertEquals(
            "Google LoadingIndicator sequence uses 7 morph shapes",
            7,
            seq.size
        )

        val expectedTypes = listOf(
            VardiyaExpressiveShapeType.SOFT_BURST,
            VardiyaExpressiveShapeType.COOKIE_9,
            VardiyaExpressiveShapeType.PENTAGON,
            VardiyaExpressiveShapeType.PILL,
            VardiyaExpressiveShapeType.SUNNY,
            VardiyaExpressiveShapeType.COOKIE_4,
            VardiyaExpressiveShapeType.OVAL
        )

        for (i in expectedTypes.indices) {
            assertEquals(
                "Sequence index $i must match Google upstream shape",
                expectedTypes[i],
                seq[i].type
            )
        }
    }

    // ========================================================================
    // 3. SHAPE MORPHING CONTINUITY & BOUNDS
    // ========================================================================

    @Test
    fun testShapeMorph_producesValidPointsAtAllFractions() {
        val bounds = Rect(0f, 0f, 100f, 100f)

        val fractions = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        for (f in fractions) {
            val points = VardiyaShapeMorph.sampleMorphedPoints(
                shape1 = VardiyaExpressiveShapes.SoftBurst,
                shape2 = VardiyaExpressiveShapes.Cookie9,
                t = f,
                bounds = bounds,
                rotationDegrees = 45f
            )
            assertEquals("Must sample 72 high-fidelity radial points", 72, points.size)
            for (p in points) {
                assertFalse("Point must not be NaN", p.x.isNaN() || p.y.isNaN())
                assertFalse("Point must not be Infinite", p.x.isInfinite() || p.y.isInfinite())
            }
        }
    }

    @Test
    fun testMorphSequence_handlesFullCycleSeamlessly() {
        val bounds = Rect(0f, 0f, 220f, 220f)

        // Test over multi-cycle range [0.0 to 14.0]
        var progress = 0.0f
        while (progress <= 14.0f) {
            val points = VardiyaShapeMorph.sampleMorphedSequencePoints(
                shapes = VardiyaExpressiveShapes.IndeterminateSequence,
                overallProgress = progress,
                bounds = bounds,
                rotationDegrees = progress * 15f
            )
            assertEquals(72, points.size)
            progress += 0.5f
        }
    }

    // ========================================================================
    // 4. HERO CONTAINER VOLUMETRIC SCALE SPRING RESOLUTION
    // ========================================================================

    @Test
    fun testHeroContainerScale_matchesVolumetricPhysicalHierarchy() {
        // NOT_STARTED: 0.96f resting volumetric state
        assertEquals(
            0.96f,
            DefaultVardiyaHeroMotionContract.resolveContainerScale(HeroSemanticState.NOT_STARTED),
            0.001f
        )

        // RUNNING: 1.00f active nominal state
        assertEquals(
            1.00f,
            DefaultVardiyaHeroMotionContract.resolveContainerScale(HeroSemanticState.RUNNING),
            0.001f
        )

        // OVERTIME: 1.03f energetic peak state
        assertEquals(
            1.03f,
            DefaultVardiyaHeroMotionContract.resolveContainerScale(HeroSemanticState.OVERTIME),
            0.001f
        )

        // PAUSED: 0.97f suspended state
        assertEquals(
            0.97f,
            DefaultVardiyaHeroMotionContract.resolveContainerScale(HeroSemanticState.PAUSED),
            0.001f
        )

        // BREAK: 0.98f relaxed state
        assertEquals(
            0.98f,
            DefaultVardiyaHeroMotionContract.resolveContainerScale(HeroSemanticState.BREAK),
            0.001f
        )

        // FINISHED: 1.00f completed state
        assertEquals(
            1.00f,
            DefaultVardiyaHeroMotionContract.resolveContainerScale(HeroSemanticState.FINISHED),
            0.001f
        )
    }

    @Test
    fun testCircularWavyHeroMotion_delegatesContainerScaleCorrectly() {
        val scaleRunning = CircularWavyHeroMotion.resolveContainerScale(
            shiftState = ShiftState.RUNNING,
            isOvertimeActive = false,
            isBreakActive = false
        )
        assertEquals(1.00f, scaleRunning, 0.001f)

        val scaleOvertime = CircularWavyHeroMotion.resolveContainerScale(
            shiftState = ShiftState.RUNNING,
            isOvertimeActive = true,
            isBreakActive = false
        )
        assertEquals(1.03f, scaleOvertime, 0.001f)

        val scaleNotStarted = CircularWavyHeroMotion.resolveContainerScale(
            shiftState = ShiftState.NOT_STARTED,
            isOvertimeActive = false,
            isBreakActive = false
        )
        assertEquals(0.96f, scaleNotStarted, 0.001f)
    }

    // ========================================================================
    // 5. TACTILE BUTTON & SELECTION CONTRACTS
    // ========================================================================

    @Test
    fun testButtonContract_pressedScaleDepression() {
        val buttonContract = DefaultVardiyaButtonMotionContract
        assertEquals(0.94f, buttonContract.resolvePressedScale(), 0.001f)
        assertEquals(0.94f, buttonContract.resolveScale(ButtonSemanticState.PRESSED), 0.001f)
        assertEquals(1.00f, buttonContract.resolveScale(ButtonSemanticState.RESTING), 0.001f)
        assertEquals(1.00f, buttonContract.resolveScale(ButtonSemanticState.DISABLED), 0.001f)
    }

    @Test
    fun testSelectionContract_borderWidthHighlight() {
        val selectionContract = DefaultVardiyaSelectionMotionContract
        assertEquals(2.dp, selectionContract.resolveBorderWidth(SelectionSemanticState.SELECTED))
        assertEquals(0.dp, selectionContract.resolveBorderWidth(SelectionSemanticState.UNSELECTED))
    }
}
