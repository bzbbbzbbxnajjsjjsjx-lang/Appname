package com.example.androidapp.theme.motion

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive test suite verifying the Vardiya Motion System foundations:
 * - Expressive motion profile (spatial and effects spring specs, damping, stiffness)
 * - Standard motion profile (spatial and effects spring specs, damping, stiffness)
 * - Reduced motion profile (snap specs for accessibility)
 * - MotionPreference enum & resolver error handling
 * - Default CompositionLocal scheme contract
 */
class VardiyaMotionSchemeTest {

    // ========================================================================
    // 1. EXPRESSIVE MOTION PROFILE TESTS
    // ========================================================================

    @Test
    fun testExpressiveMotionScheme_spatialSpecs_haveCorrectPhysics() {
        val scheme = VardiyaMotionScheme.expressive()

        // Default Spatial (0.8f damping, 380f stiffness)
        val defaultSpatial = scheme.defaultSpatialSpec<Float>()
        assertTrue("defaultSpatial must be SpringSpec", defaultSpatial is SpringSpec<Float>)
        val defaultSpring = defaultSpatial as SpringSpec<Float>
        assertEquals(VardiyaMotionTokens.ExpressiveDefaultSpatialDamping, defaultSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.ExpressiveDefaultSpatialStiffness, defaultSpring.stiffness, 0.001f)

        // Fast Spatial (0.6f damping, 800f stiffness)
        val fastSpatial = scheme.fastSpatialSpec<Float>()
        assertTrue("fastSpatial must be SpringSpec", fastSpatial is SpringSpec<Float>)
        val fastSpring = fastSpatial as SpringSpec<Float>
        assertEquals(VardiyaMotionTokens.ExpressiveFastSpatialDamping, fastSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.ExpressiveFastSpatialStiffness, fastSpring.stiffness, 0.001f)

        // Slow Spatial (0.8f damping, 200f stiffness)
        val slowSpatial = scheme.slowSpatialSpec<Float>()
        assertTrue("slowSpatial must be SpringSpec", slowSpatial is SpringSpec<Float>)
        val slowSpring = slowSpatial as SpringSpec<Float>
        assertEquals(VardiyaMotionTokens.ExpressiveSlowSpatialDamping, slowSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.ExpressiveSlowSpatialStiffness, slowSpring.stiffness, 0.001f)
    }

    @Test
    fun testExpressiveMotionScheme_effectsSpecs_areCriticallyDamped() {
        val scheme = VardiyaMotionScheme.expressive()

        // Default Effects (1.0f damping, 1600f stiffness)
        val defaultEffects = scheme.defaultEffectsSpec<Float>()
        assertTrue("defaultEffects must be SpringSpec", defaultEffects is SpringSpec<Float>)
        val defaultSpring = defaultEffects as SpringSpec<Float>
        assertEquals(1.0f, defaultSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.ExpressiveDefaultEffectsStiffness, defaultSpring.stiffness, 0.001f)

        // Fast Effects (1.0f damping, 3800f stiffness)
        val fastEffects = scheme.fastEffectsSpec<Float>()
        assertTrue("fastEffects must be SpringSpec", fastEffects is SpringSpec<Float>)
        val fastSpring = fastEffects as SpringSpec<Float>
        assertEquals(1.0f, fastSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.ExpressiveFastEffectsStiffness, fastSpring.stiffness, 0.001f)

        // Slow Effects (1.0f damping, 800f stiffness)
        val slowEffects = scheme.slowEffectsSpec<Float>()
        assertTrue("slowEffects must be SpringSpec", slowEffects is SpringSpec<Float>)
        val slowSpring = slowEffects as SpringSpec<Float>
        assertEquals(1.0f, slowSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.ExpressiveSlowEffectsStiffness, slowSpring.stiffness, 0.001f)
    }

    // ========================================================================
    // 2. STANDARD MOTION PROFILE TESTS
    // ========================================================================

    @Test
    fun testStandardMotionScheme_spatialSpecs_haveCorrectPhysics() {
        val scheme = VardiyaMotionScheme.standard()

        // Default Spatial (0.9f damping, 700f stiffness)
        val defaultSpatial = scheme.defaultSpatialSpec<Float>()
        assertTrue("defaultSpatial must be SpringSpec", defaultSpatial is SpringSpec<Float>)
        val defaultSpring = defaultSpatial as SpringSpec<Float>
        assertEquals(VardiyaMotionTokens.StandardDefaultSpatialDamping, defaultSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.StandardDefaultSpatialStiffness, defaultSpring.stiffness, 0.001f)

        // Fast Spatial (0.9f damping, 1400f stiffness)
        val fastSpatial = scheme.fastSpatialSpec<Float>()
        assertTrue("fastSpatial must be SpringSpec", fastSpatial is SpringSpec<Float>)
        val fastSpring = fastSpatial as SpringSpec<Float>
        assertEquals(VardiyaMotionTokens.StandardFastSpatialDamping, fastSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.StandardFastSpatialStiffness, fastSpring.stiffness, 0.001f)

        // Slow Spatial (0.9f damping, 300f stiffness)
        val slowSpatial = scheme.slowSpatialSpec<Float>()
        assertTrue("slowSpatial must be SpringSpec", slowSpatial is SpringSpec<Float>)
        val slowSpring = slowSpatial as SpringSpec<Float>
        assertEquals(VardiyaMotionTokens.StandardSlowSpatialDamping, slowSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.StandardSlowSpatialStiffness, slowSpring.stiffness, 0.001f)
    }

    @Test
    fun testStandardMotionScheme_effectsSpecs_areCriticallyDamped() {
        val scheme = VardiyaMotionScheme.standard()

        // Default Effects (1.0f damping, 1600f stiffness)
        val defaultEffects = scheme.defaultEffectsSpec<Float>()
        assertTrue("defaultEffects must be SpringSpec", defaultEffects is SpringSpec<Float>)
        val defaultSpring = defaultEffects as SpringSpec<Float>
        assertEquals(1.0f, defaultSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.StandardDefaultEffectsStiffness, defaultSpring.stiffness, 0.001f)

        // Fast Effects (1.0f damping, 3800f stiffness)
        val fastEffects = scheme.fastEffectsSpec<Float>()
        assertTrue("fastEffects must be SpringSpec", fastEffects is SpringSpec<Float>)
        val fastSpring = fastEffects as SpringSpec<Float>
        assertEquals(1.0f, fastSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.StandardFastEffectsStiffness, fastSpring.stiffness, 0.001f)

        // Slow Effects (1.0f damping, 800f stiffness)
        val slowEffects = scheme.slowEffectsSpec<Float>()
        assertTrue("slowEffects must be SpringSpec", slowEffects is SpringSpec<Float>)
        val slowSpring = slowEffects as SpringSpec<Float>
        assertEquals(1.0f, slowSpring.dampingRatio, 0.001f)
        assertEquals(VardiyaMotionTokens.StandardSlowEffectsStiffness, slowSpring.stiffness, 0.001f)
    }

    // ========================================================================
    // 3. REDUCED MOTION PROFILE TESTS
    // ========================================================================

    @Test
    fun testReducedMotionScheme_allSpecsAreSnap() {
        val scheme = VardiyaMotionScheme.reducedMotion()

        assertTrue("defaultSpatial must be SnapSpec", scheme.defaultSpatialSpec<Float>() is SnapSpec<Float>)
        assertTrue("fastSpatial must be SnapSpec", scheme.fastSpatialSpec<Float>() is SnapSpec<Float>)
        assertTrue("slowSpatial must be SnapSpec", scheme.slowSpatialSpec<Float>() is SnapSpec<Float>)
        assertTrue("defaultEffects must be SnapSpec", scheme.defaultEffectsSpec<Float>() is SnapSpec<Float>)
        assertTrue("fastEffects must be SnapSpec", scheme.fastEffectsSpec<Float>() is SnapSpec<Float>)
        assertTrue("slowEffects must be SnapSpec", scheme.slowEffectsSpec<Float>() is SnapSpec<Float>)
    }

    // ========================================================================
    // 4. MOTION PREFERENCE & RESOLVER TESTS
    // ========================================================================

    @Test
    fun testMotionPreferenceEnum_valuesExist() {
        assertEquals(2, MotionPreference.values().size)
        assertEquals(MotionPreference.NORMAL, MotionPreference.valueOf("NORMAL"))
        assertEquals(MotionPreference.REDUCED, MotionPreference.valueOf("REDUCED"))
    }

    @Test
    fun testCustomMotionPreferenceResolver_canOverridePreference() {
        var preferenceToReturn = MotionPreference.NORMAL
        val resolver = MotionPreferenceResolver { preferenceToReturn }
        val dummyContext = android.content.ContextWrapper(null)

        assertEquals(MotionPreference.NORMAL, resolver.resolve(dummyContext))

        preferenceToReturn = MotionPreference.REDUCED
        assertEquals(MotionPreference.REDUCED, resolver.resolve(dummyContext))
    }

    @Test
    fun testSystemMotionPreferenceResolver_defaultsGracefullyOnFailure() {
        val dummyContext = android.content.ContextWrapper(null)
        val resolved = SystemMotionPreferenceResolver.resolve(dummyContext)
        assertEquals(MotionPreference.NORMAL, resolved)
    }

    // ========================================================================
    // 5. CACHED INSTANCES (GC REDUCTION)
    // ========================================================================

    @Test
    fun testMotionScheme_retainsSpecInstancesAcrossInvocations() {
        val expressive = VardiyaMotionScheme.expressive()
        val spec1 = expressive.defaultSpatialSpec<Float>()
        val spec2 = expressive.defaultSpatialSpec<Float>()

        // Instance equality check: Reuses same spec instance, eliminating memory allocations in recomposition
        assertTrue("Specs must be identical instances for zero-allocation performance", spec1 === spec2)
    }

    // ========================================================================
    // 6. COMPOSITION LOCAL DEFAULT
    // ========================================================================

    @Test
    fun testLocalVardiyaMotionScheme_defaultNotNull() {
        assertNotNull(LocalVardiyaMotionScheme)
        assertNotNull(LocalMotionPreference)
    }
}
