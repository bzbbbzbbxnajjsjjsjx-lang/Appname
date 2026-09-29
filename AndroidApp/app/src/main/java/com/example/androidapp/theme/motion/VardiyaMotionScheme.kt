package com.example.androidapp.theme.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Semantic motion scheme providing [FiniteAnimationSpec] definitions for Vardiya.
 *
 * Modeled after Material 3 Expressive motion principles:
 * - Spatial specs: For shape, bounds, size, radius, offset, scale, and layout animations.
 *   Uses gentle underdamping to create an organic, tactile physical presence.
 * - Effects specs: For color, alpha, elevation, and opacity transitions.
 *   Uses critical damping (dampingRatio = 1.0f) to eliminate oscillation artifacts.
 *
 * UI components query semantic categories:
 * `motion.defaultSpatialSpec<Float>()`
 * `motion.fastEffectsSpec<Color>()`
 * rather than hard-coding raw numbers, durations, or damping values.
 */
@Immutable
interface VardiyaMotionScheme {
    /**
     * Default spatial motion spec. Designed for general shape morphing, bounds changes,
     * container transitions, and circular hero progress animation.
     */
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T>

    /**
     * Fast spatial motion spec. Designed for responsive tactile UI feedback such as button
     * pill morphing, quick card collapse, and small spatial adjustments.
     */
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T>

    /**
     * Slow spatial motion spec. Designed for large, graceful transitions such as full-screen
     * container expansion or hero ring intro animation.
     */
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T>

    /**
     * Default effects motion spec. Critically damped (no bounce) for color schemes,
     * surface container transitions, and content opacity.
     */
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T>

    /**
     * Fast effects motion spec. Critically damped for snappy feedback such as chip selection,
     * switch color changes, and hover/focus states.
     */
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T>

    /**
     * Slow effects motion spec. Critically damped for ambient background transitions,
     * glow fades, and relaxed secondary color changes.
     */
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T>

    companion object {
        /**
         * Returns an Expressive motion scheme configured with official Material 3
         * Expressive tokens (spatial damping: 0.8f/0.6f, effects damping: 1.0f).
         */
        fun expressive(): VardiyaMotionScheme = ExpressiveMotionSchemeImpl

        /**
         * Returns a Standard motion scheme configured with utilitarian Material 3
         * Standard tokens (spatial damping: 0.9f, effects damping: 1.0f).
         */
        fun standard(): VardiyaMotionScheme = StandardMotionSchemeImpl

        /**
         * Returns a Reduced motion scheme where spatial movements and effects snap
         * immediately to respect user accessibility preferences.
         */
        fun reducedMotion(): VardiyaMotionScheme = ReducedMotionSchemeImpl
    }
}

/**
 * Expressive motion profile implementation caching reusable spring specs to eliminate
 * garbage collection overhead on every query.
 */
@Suppress("UNCHECKED_CAST")
private object ExpressiveMotionSchemeImpl : VardiyaMotionScheme {
    private val defaultSpatial = spring<Any>(
        dampingRatio = VardiyaMotionTokens.ExpressiveDefaultSpatialDamping,
        stiffness = VardiyaMotionTokens.ExpressiveDefaultSpatialStiffness,
    )
    private val fastSpatial = spring<Any>(
        dampingRatio = VardiyaMotionTokens.ExpressiveFastSpatialDamping,
        stiffness = VardiyaMotionTokens.ExpressiveFastSpatialStiffness,
    )
    private val slowSpatial = spring<Any>(
        dampingRatio = VardiyaMotionTokens.ExpressiveSlowSpatialDamping,
        stiffness = VardiyaMotionTokens.ExpressiveSlowSpatialStiffness,
    )
    private val defaultEffects = spring<Any>(
        dampingRatio = VardiyaMotionTokens.ExpressiveDefaultEffectsDamping,
        stiffness = VardiyaMotionTokens.ExpressiveDefaultEffectsStiffness,
    )
    private val fastEffects = spring<Any>(
        dampingRatio = VardiyaMotionTokens.ExpressiveFastEffectsDamping,
        stiffness = VardiyaMotionTokens.ExpressiveFastEffectsStiffness,
    )
    private val slowEffects = spring<Any>(
        dampingRatio = VardiyaMotionTokens.ExpressiveSlowEffectsDamping,
        stiffness = VardiyaMotionTokens.ExpressiveSlowEffectsStiffness,
    )

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = defaultSpatial as FiniteAnimationSpec<T>
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = fastSpatial as FiniteAnimationSpec<T>
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = slowSpatial as FiniteAnimationSpec<T>
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = defaultEffects as FiniteAnimationSpec<T>
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = fastEffects as FiniteAnimationSpec<T>
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = slowEffects as FiniteAnimationSpec<T>
}

/**
 * Standard motion profile implementation caching reusable spring specs for utilitarian UI.
 */
@Suppress("UNCHECKED_CAST")
private object StandardMotionSchemeImpl : VardiyaMotionScheme {
    private val defaultSpatial = spring<Any>(
        dampingRatio = VardiyaMotionTokens.StandardDefaultSpatialDamping,
        stiffness = VardiyaMotionTokens.StandardDefaultSpatialStiffness,
    )
    private val fastSpatial = spring<Any>(
        dampingRatio = VardiyaMotionTokens.StandardFastSpatialDamping,
        stiffness = VardiyaMotionTokens.StandardFastSpatialStiffness,
    )
    private val slowSpatial = spring<Any>(
        dampingRatio = VardiyaMotionTokens.StandardSlowSpatialDamping,
        stiffness = VardiyaMotionTokens.StandardSlowSpatialStiffness,
    )
    private val defaultEffects = spring<Any>(
        dampingRatio = VardiyaMotionTokens.StandardDefaultEffectsDamping,
        stiffness = VardiyaMotionTokens.StandardDefaultEffectsStiffness,
    )
    private val fastEffects = spring<Any>(
        dampingRatio = VardiyaMotionTokens.StandardFastEffectsDamping,
        stiffness = VardiyaMotionTokens.StandardFastEffectsStiffness,
    )
    private val slowEffects = spring<Any>(
        dampingRatio = VardiyaMotionTokens.StandardSlowEffectsDamping,
        stiffness = VardiyaMotionTokens.StandardSlowEffectsStiffness,
    )

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = defaultSpatial as FiniteAnimationSpec<T>
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = fastSpatial as FiniteAnimationSpec<T>
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = slowSpatial as FiniteAnimationSpec<T>
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = defaultEffects as FiniteAnimationSpec<T>
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = fastEffects as FiniteAnimationSpec<T>
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = slowEffects as FiniteAnimationSpec<T>
}

/**
 * Reduced motion profile providing zero-delay snap specs to eliminate motion disorientation
 * when accessibility reduced-motion is requested.
 */
@Suppress("UNCHECKED_CAST")
private object ReducedMotionSchemeImpl : VardiyaMotionScheme {
    private val snapSpec = snap<Any>()

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snapSpec as FiniteAnimationSpec<T>
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snapSpec as FiniteAnimationSpec<T>
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snapSpec as FiniteAnimationSpec<T>
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snapSpec as FiniteAnimationSpec<T>
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snapSpec as FiniteAnimationSpec<T>
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snapSpec as FiniteAnimationSpec<T>
}

/**
 * CompositionLocal providing the current [VardiyaMotionScheme].
 * Defaults to the expressive profile.
 */
val LocalVardiyaMotionScheme = staticCompositionLocalOf<VardiyaMotionScheme> {
    VardiyaMotionScheme.expressive()
}

/**
 * Convenient object accessor matching Compose idiomatic styling:
 * `VardiyaTheme.motionScheme.defaultSpatialSpec()`
 */
object VardiyaTheme {
    val motionScheme: VardiyaMotionScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalVardiyaMotionScheme.current

    val motionPreference: MotionPreference
        @Composable
        @ReadOnlyComposable
        get() = LocalMotionPreference.current
}
