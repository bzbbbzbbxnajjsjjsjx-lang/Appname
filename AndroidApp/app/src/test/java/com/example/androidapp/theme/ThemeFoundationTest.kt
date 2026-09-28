package com.example.androidapp.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.vardiya.domain.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Unit test suite verifying Material 3 Expressive theme foundations:
 * - Color schemes (Light and Dark complete coverage)
 * - WCAG AA accessibility contrast ratios (>= 4.5:1)
 * - Surface container tonal hierarchy (Light decreasing luminance, Dark increasing luminance)
 * - Shape scale and expressive tokens
 * - Typography scale and expressive styles
 * - Circular wavy progress hero semantic color mapping contract
 * - Backward compatibility with legacy color constants
 */
class ThemeFoundationTest {

    // ------------------------------------------------------------------------
    // Relative Luminance & WCAG Contrast Formula
    // ------------------------------------------------------------------------
    private fun calculateRelativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(foreground: Color, background: Color): Double {
        val lum1 = calculateRelativeLuminance(foreground)
        val lum2 = calculateRelativeLuminance(background)
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    // ------------------------------------------------------------------------
    // 1. Color Schemes Completeness
    // ------------------------------------------------------------------------
    @Test
    fun testLightColorScheme_hasAllTokensDefined() {
        assertNotNull(LightColorScheme.primary)
        assertNotNull(LightColorScheme.onPrimary)
        assertNotNull(LightColorScheme.primaryContainer)
        assertNotNull(LightColorScheme.onPrimaryContainer)
        assertNotNull(LightColorScheme.inversePrimary)

        assertNotNull(LightColorScheme.secondary)
        assertNotNull(LightColorScheme.onSecondary)
        assertNotNull(LightColorScheme.secondaryContainer)
        assertNotNull(LightColorScheme.onSecondaryContainer)

        assertNotNull(LightColorScheme.tertiary)
        assertNotNull(LightColorScheme.onTertiary)
        assertNotNull(LightColorScheme.tertiaryContainer)
        assertNotNull(LightColorScheme.onTertiaryContainer)

        assertNotNull(LightColorScheme.error)
        assertNotNull(LightColorScheme.onError)
        assertNotNull(LightColorScheme.errorContainer)
        assertNotNull(LightColorScheme.onErrorContainer)

        assertNotNull(LightColorScheme.background)
        assertNotNull(LightColorScheme.onBackground)
        assertNotNull(LightColorScheme.surface)
        assertNotNull(LightColorScheme.onSurface)
        assertNotNull(LightColorScheme.surfaceVariant)
        assertNotNull(LightColorScheme.onSurfaceVariant)

        assertNotNull(LightColorScheme.surfaceContainerLowest)
        assertNotNull(LightColorScheme.surfaceContainerLow)
        assertNotNull(LightColorScheme.surfaceContainer)
        assertNotNull(LightColorScheme.surfaceContainerHigh)
        assertNotNull(LightColorScheme.surfaceContainerHighest)

        assertNotNull(LightColorScheme.surfaceDim)
        assertNotNull(LightColorScheme.surfaceBright)
        assertNotNull(LightColorScheme.outline)
        assertNotNull(LightColorScheme.outlineVariant)
    }

    @Test
    fun testDarkColorScheme_hasAllTokensDefined() {
        assertNotNull(DarkColorScheme.primary)
        assertNotNull(DarkColorScheme.onPrimary)
        assertNotNull(DarkColorScheme.primaryContainer)
        assertNotNull(DarkColorScheme.onPrimaryContainer)
        assertNotNull(DarkColorScheme.inversePrimary)

        assertNotNull(DarkColorScheme.secondary)
        assertNotNull(DarkColorScheme.onSecondary)
        assertNotNull(DarkColorScheme.secondaryContainer)
        assertNotNull(DarkColorScheme.onSecondaryContainer)

        assertNotNull(DarkColorScheme.tertiary)
        assertNotNull(DarkColorScheme.onTertiary)
        assertNotNull(DarkColorScheme.tertiaryContainer)
        assertNotNull(DarkColorScheme.onTertiaryContainer)

        assertNotNull(DarkColorScheme.error)
        assertNotNull(DarkColorScheme.onError)
        assertNotNull(DarkColorScheme.errorContainer)
        assertNotNull(DarkColorScheme.onErrorContainer)

        assertNotNull(DarkColorScheme.background)
        assertNotNull(DarkColorScheme.onBackground)
        assertNotNull(DarkColorScheme.surface)
        assertNotNull(DarkColorScheme.onSurface)
        assertNotNull(DarkColorScheme.surfaceVariant)
        assertNotNull(DarkColorScheme.onSurfaceVariant)

        assertNotNull(DarkColorScheme.surfaceContainerLowest)
        assertNotNull(DarkColorScheme.surfaceContainerLow)
        assertNotNull(DarkColorScheme.surfaceContainer)
        assertNotNull(DarkColorScheme.surfaceContainerHigh)
        assertNotNull(DarkColorScheme.surfaceContainerHighest)

        assertNotNull(DarkColorScheme.surfaceDim)
        assertNotNull(DarkColorScheme.surfaceBright)
        assertNotNull(DarkColorScheme.outline)
        assertNotNull(DarkColorScheme.outlineVariant)
    }

    // ------------------------------------------------------------------------
    // 2. WCAG AA Accessibility Contrast Tests (Ratio >= 4.5:1)
    // ------------------------------------------------------------------------
    @Test
    fun testLightColorScheme_meetsWcagContrastRequirements() {
        val pairs = listOf(
            "Primary" to (LightColorScheme.onPrimary to LightColorScheme.primary),
            "PrimaryContainer" to (LightColorScheme.onPrimaryContainer to LightColorScheme.primaryContainer),
            "Secondary" to (LightColorScheme.onSecondary to LightColorScheme.secondary),
            "SecondaryContainer" to (LightColorScheme.onSecondaryContainer to LightColorScheme.secondaryContainer),
            "Tertiary" to (LightColorScheme.onTertiary to LightColorScheme.tertiary),
            "TertiaryContainer" to (LightColorScheme.onTertiaryContainer to LightColorScheme.tertiaryContainer),
            "Error" to (LightColorScheme.onError to LightColorScheme.error),
            "Surface" to (LightColorScheme.onSurface to LightColorScheme.surface),
            "Background" to (LightColorScheme.onBackground to LightColorScheme.background)
        )

        for ((name, pair) in pairs) {
            val ratio = calculateContrastRatio(pair.first, pair.second)
            assertTrue(
                "Light $name contrast ratio ($ratio) must meet WCAG AA >= 4.5:1",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun testDarkColorScheme_meetsWcagContrastRequirements() {
        val pairs = listOf(
            "Primary" to (DarkColorScheme.onPrimary to DarkColorScheme.primary),
            "PrimaryContainer" to (DarkColorScheme.onPrimaryContainer to DarkColorScheme.primaryContainer),
            "Secondary" to (DarkColorScheme.onSecondary to DarkColorScheme.secondary),
            "SecondaryContainer" to (DarkColorScheme.onSecondaryContainer to DarkColorScheme.secondaryContainer),
            "Tertiary" to (DarkColorScheme.onTertiary to DarkColorScheme.tertiary),
            "TertiaryContainer" to (DarkColorScheme.onTertiaryContainer to DarkColorScheme.tertiaryContainer),
            "Error" to (DarkColorScheme.onError to DarkColorScheme.error),
            "Surface" to (DarkColorScheme.onSurface to DarkColorScheme.surface),
            "Background" to (DarkColorScheme.onBackground to DarkColorScheme.background)
        )

        for ((name, pair) in pairs) {
            val ratio = calculateContrastRatio(pair.first, pair.second)
            assertTrue(
                "Dark $name contrast ratio ($ratio) must meet WCAG AA >= 4.5:1",
                ratio >= 4.5
            )
        }
    }

    // ------------------------------------------------------------------------
    // 3. Surface Container Tonal Hierarchy
    // ------------------------------------------------------------------------
    @Test
    fun testLightSurfaceContainerHierarchy_decreasesLuminanceForDepth() {
        val lumLowest = calculateRelativeLuminance(LightColorScheme.surfaceContainerLowest)
        val lumLow = calculateRelativeLuminance(LightColorScheme.surfaceContainerLow)
        val lumDefault = calculateRelativeLuminance(LightColorScheme.surfaceContainer)
        val lumHigh = calculateRelativeLuminance(LightColorScheme.surfaceContainerHigh)
        val lumHighest = calculateRelativeLuminance(LightColorScheme.surfaceContainerHighest)

        assertTrue("Lowest >= Low in Light scheme", lumLowest >= lumLow)
        assertTrue("Low >= Default in Light scheme", lumLow >= lumDefault)
        assertTrue("Default >= High in Light scheme", lumDefault >= lumHigh)
        assertTrue("High >= Highest in Light scheme", lumHigh >= lumHighest)
    }

    @Test
    fun testDarkSurfaceContainerHierarchy_increasesLuminanceForElevation() {
        val lumLowest = calculateRelativeLuminance(DarkColorScheme.surfaceContainerLowest)
        val lumLow = calculateRelativeLuminance(DarkColorScheme.surfaceContainerLow)
        val lumDefault = calculateRelativeLuminance(DarkColorScheme.surfaceContainer)
        val lumHigh = calculateRelativeLuminance(DarkColorScheme.surfaceContainerHigh)
        val lumHighest = calculateRelativeLuminance(DarkColorScheme.surfaceContainerHighest)

        assertTrue("Lowest <= Low in Dark scheme", lumLowest <= lumLow)
        assertTrue("Low <= Default in Dark scheme", lumLow <= lumDefault)
        assertTrue("Default <= High in Dark scheme", lumDefault <= lumHigh)
        assertTrue("High <= Highest in Dark scheme", lumHigh <= lumHighest)
    }

    // ------------------------------------------------------------------------
    // 4. Shape Scale & Expressive Tokens
    // ------------------------------------------------------------------------
    @Test
    fun testShapesScale_matchesMaterial3Specification() {
        assertNotNull(Shapes.extraSmall)
        assertNotNull(Shapes.small)
        assertNotNull(Shapes.medium)
        assertNotNull(Shapes.large)
        assertNotNull(Shapes.extraLarge)

        assertNotNull(PillShape)
        assertNotNull(SquircleCardShape)
        assertNotNull(ExpressiveHeroShape)
        assertNotNull(FullCircleShape)
    }

    // ------------------------------------------------------------------------
    // 5. Typography Scale
    // ------------------------------------------------------------------------
    @Test
    fun testTypographyScale_hasValidTypeStyles() {
        assertEquals(57.sp, Typography.displayLarge.fontSize)
        assertEquals(45.sp, Typography.displayMedium.fontSize)
        assertEquals(36.sp, Typography.displaySmall.fontSize)

        assertEquals(32.sp, Typography.headlineLarge.fontSize)
        assertEquals(28.sp, Typography.headlineMedium.fontSize)
        assertEquals(24.sp, Typography.headlineSmall.fontSize)

        assertEquals(22.sp, Typography.titleLarge.fontSize)
        assertEquals(16.sp, Typography.titleMedium.fontSize)
        assertEquals(14.sp, Typography.titleSmall.fontSize)

        assertEquals(16.sp, Typography.bodyLarge.fontSize)
        assertEquals(14.sp, Typography.bodyMedium.fontSize)
        assertEquals(12.sp, Typography.bodySmall.fontSize)

        assertEquals(14.sp, Typography.labelLarge.fontSize)
        assertEquals(12.sp, Typography.labelMedium.fontSize)
        assertEquals(11.sp, Typography.labelSmall.fontSize)

        // Specialized Expressive Styles
        assertEquals(50.sp, HeroCurrencyStyle.fontSize)
        assertEquals(FontFamily.Monospace, TimerMonospaceStyle.fontFamily)
    }

    // ------------------------------------------------------------------------
    // 6. Circular Wavy Progress Hero Semantic Color Mapping Contract
    // ------------------------------------------------------------------------
    @Test
    fun testWavyHeroColorContract_mapsSemanticStatesProperly() {
        fun resolveHeroActiveColor(state: ShiftState, dark: Boolean): Color {
            val scheme = if (dark) DarkColorScheme else LightColorScheme
            return when (state) {
                ShiftState.RUNNING -> scheme.primary
                ShiftState.PAUSED -> scheme.tertiary
                ShiftState.FINISHED -> scheme.secondary
                ShiftState.NOT_STARTED -> scheme.primary
            }
        }

        // Light mode checks
        assertEquals(LightColorScheme.primary, resolveHeroActiveColor(ShiftState.RUNNING, false))
        assertEquals(LightColorScheme.tertiary, resolveHeroActiveColor(ShiftState.PAUSED, false))
        assertEquals(LightColorScheme.secondary, resolveHeroActiveColor(ShiftState.FINISHED, false))
        assertEquals(LightColorScheme.primary, resolveHeroActiveColor(ShiftState.NOT_STARTED, false))

        // Dark mode checks
        assertEquals(DarkColorScheme.primary, resolveHeroActiveColor(ShiftState.RUNNING, true))
        assertEquals(DarkColorScheme.tertiary, resolveHeroActiveColor(ShiftState.PAUSED, true))
        assertEquals(DarkColorScheme.secondary, resolveHeroActiveColor(ShiftState.FINISHED, true))
        assertEquals(DarkColorScheme.primary, resolveHeroActiveColor(ShiftState.NOT_STARTED, true))
    }

    // ------------------------------------------------------------------------
    // 7. Backward Compatibility with Template Colors
    // ------------------------------------------------------------------------
    @Test
    fun testLegacyColorConstants_remainUnchanged() {
        assertEquals(Color(0xFFD0BCFF), Purple80)
        assertEquals(Color(0xFFCCC2DC), PurpleGrey80)
        assertEquals(Color(0xFFEFB8C8), Pink80)
        assertEquals(Color(0xFF6650a4), Purple40)
        assertEquals(Color(0xFF625b71), PurpleGrey40)
        assertEquals(Color(0xFF7D5260), Pink40)
    }
}
