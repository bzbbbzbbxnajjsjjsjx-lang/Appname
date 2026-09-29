package com.example.androidapp.theme.motion

/**
 * Motion design tokens matching Google Material 3 Expressive motion specifications.
 *
 * Source: AndroidX Compose Material 3 (ExpressiveMotionTokens & StandardMotionTokens).
 *
 * Physics principles:
 * - Spatial specs: For shape, bounds, size, position, and radius. Uses gentle underdamping (bouncy/energetic)
 *   to create a tactile physical presence.
 * - Effects specs: For color, alpha, elevation, and opacity. Uses critical damping (dampingRatio = 1.0f)
 *   to eliminate visual bouncing or oscillating color artifacts.
 */
object VardiyaMotionTokens {

    // ========================================================================
    // EXPRESSIVE MOTION PROFILE
    // Recommended for hero interactions, primary containers, and state changes.
    // ========================================================================

    // Expressive Spatial Tokens
    const val ExpressiveDefaultSpatialDamping = 0.8f
    const val ExpressiveDefaultSpatialStiffness = 380.0f

    const val ExpressiveFastSpatialDamping = 0.6f
    const val ExpressiveFastSpatialStiffness = 800.0f

    const val ExpressiveSlowSpatialDamping = 0.8f
    const val ExpressiveSlowSpatialStiffness = 200.0f

    // Expressive Effects Tokens
    const val ExpressiveDefaultEffectsDamping = 1.0f
    const val ExpressiveDefaultEffectsStiffness = 1600.0f

    const val ExpressiveFastEffectsDamping = 1.0f
    const val ExpressiveFastEffectsStiffness = 3800.0f

    const val ExpressiveSlowEffectsDamping = 1.0f
    const val ExpressiveSlowEffectsStiffness = 800.0f

    // ========================================================================
    // STANDARD MOTION PROFILE
    // Recommended for utilitarian UI, recurring secondary interactions, and data displays.
    // ========================================================================

    // Standard Spatial Tokens
    const val StandardDefaultSpatialDamping = 0.9f
    const val StandardDefaultSpatialStiffness = 700.0f

    const val StandardFastSpatialDamping = 0.9f
    const val StandardFastSpatialStiffness = 1400.0f

    const val StandardSlowSpatialDamping = 0.9f
    const val StandardSlowSpatialStiffness = 300.0f

    // Standard Effects Tokens
    const val StandardDefaultEffectsDamping = 1.0f
    const val StandardDefaultEffectsStiffness = 1600.0f

    const val StandardFastEffectsDamping = 1.0f
    const val StandardFastEffectsStiffness = 3800.0f

    const val StandardSlowEffectsDamping = 1.0f
    const val StandardSlowEffectsStiffness = 800.0f
}
