package com.example.androidapp.vardiya.ui.navigation

import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Unit Test Suite for Vardiya 3.0 Navigation 3 & Adaptive Shell.
 *
 * Verifies:
 * 1. Initial Home destination
 * 2. History navigation
 * 3. Analytics navigation
 * 4. Settings navigation
 * 5. Selected destination state consistency
 * 6. Back navigation from secondary tabs to Home
 * 7. Back navigation from root Home (no-op / exit contract)
 * 8. Tab switching bounded stack guarantees (no stack bloat)
 * 9. Compact window size classification (< 600dp)
 * 10. Medium/Expanded window size classification (>= 600dp)
 * 11. Accessibility attributes (labels, content descriptions, icons)
 * 12. State preservation and serialization readiness
 */
class VardiyaNavigationTest {

    // ------------------------------------------------------------------------
    // 1. Initial Destination
    // ------------------------------------------------------------------------
    @Test
    fun testInitialDestination_isHomeNavKey() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        assertEquals(HomeNavKey, navState.currentDestination)
        assertEquals(1, navState.backStack.size)
        assertEquals(HomeNavKey, navState.backStack.first())
    }

    // ------------------------------------------------------------------------
    // 2. History Navigation
    // ------------------------------------------------------------------------
    @Test
    fun testNavigateToHistory_updatesDestinationAndMaintainsHomeRoot() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        navState.navigateTo(HistoryNavKey)

        assertEquals(HistoryNavKey, navState.currentDestination)
        assertEquals(2, navState.backStack.size)
        assertEquals(HomeNavKey, navState.backStack[0])
        assertEquals(HistoryNavKey, navState.backStack[1])
    }

    // ------------------------------------------------------------------------
    // 3. Analytics Navigation
    // ------------------------------------------------------------------------
    @Test
    fun testNavigateToAnalytics_updatesDestination() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        navState.navigateTo(AnalyticsNavKey)

        assertEquals(AnalyticsNavKey, navState.currentDestination)
        assertEquals(2, navState.backStack.size)
        assertEquals(HomeNavKey, navState.backStack[0])
        assertEquals(AnalyticsNavKey, navState.backStack[1])
    }

    // ------------------------------------------------------------------------
    // 4. Settings Navigation
    // ------------------------------------------------------------------------
    @Test
    fun testNavigateToSettings_updatesDestination() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        navState.navigateTo(SettingsNavKey)

        assertEquals(SettingsNavKey, navState.currentDestination)
        assertEquals(2, navState.backStack.size)
        assertEquals(HomeNavKey, navState.backStack[0])
        assertEquals(SettingsNavKey, navState.backStack[1])
    }

    // ------------------------------------------------------------------------
    // 5. Selected Destination State Consistency
    // ------------------------------------------------------------------------
    @Test
    fun testSelectedDestinationState_isAccuratelyReported() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        assertEquals(4, TOP_LEVEL_NAV_KEYS.size)
        assertTrue(TOP_LEVEL_NAV_KEYS.contains(HomeNavKey))
        assertTrue(TOP_LEVEL_NAV_KEYS.contains(HistoryNavKey))
        assertTrue(TOP_LEVEL_NAV_KEYS.contains(AnalyticsNavKey))
        assertTrue(TOP_LEVEL_NAV_KEYS.contains(SettingsNavKey))

        TOP_LEVEL_NAV_KEYS.forEach { destination ->
            navState.navigateTo(destination)
            assertEquals(destination, navState.currentDestination)
        }
    }

    // ------------------------------------------------------------------------
    // 6. Back Navigation from Sub-tab
    // ------------------------------------------------------------------------
    @Test
    fun testBackNavigation_fromSubTabReturnsToHome() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        navState.navigateTo(HistoryNavKey)
        assertEquals(HistoryNavKey, navState.currentDestination)

        val handled = navState.handleBack()
        assertTrue(handled)
        assertEquals(HomeNavKey, navState.currentDestination)
        assertEquals(1, navState.backStack.size)
        assertEquals(HomeNavKey, navState.backStack.first())
    }

    // ------------------------------------------------------------------------
    // 7. Back Navigation from Root Home
    // ------------------------------------------------------------------------
    @Test
    fun testBackNavigation_fromHomeReturnsFalse() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        assertEquals(HomeNavKey, navState.currentDestination)
        val handled = navState.handleBack()

        assertFalse("Back at root Home should not pop the root destination", handled)
        assertEquals(HomeNavKey, navState.currentDestination)
        assertEquals(1, navState.backStack.size)
    }

    // ------------------------------------------------------------------------
    // 8. Tab Switching Bounded Stack
    // ------------------------------------------------------------------------
    @Test
    fun testTabSwitching_maintainsBoundedStackWithoutMemoryLeak() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        // Rapidly switch across all tabs multiple times
        for (i in 1..5) {
            navState.navigateTo(HistoryNavKey)
            assertEquals(2, navState.backStack.size)

            navState.navigateTo(AnalyticsNavKey)
            assertEquals(2, navState.backStack.size)

            navState.navigateTo(SettingsNavKey)
            assertEquals(2, navState.backStack.size)

            navState.navigateTo(HomeNavKey)
            assertEquals(1, navState.backStack.size)
        }

        // Final state: only HomeNavKey
        assertEquals(HomeNavKey, navState.currentDestination)
        assertEquals(1, navState.backStack.size)
    }

    @Test
    fun testSameTabClick_isNoOp() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        navState.navigateTo(HistoryNavKey)
        assertEquals(2, navState.backStack.size)

        // Click History again
        navState.navigateTo(HistoryNavKey)
        assertEquals(2, navState.backStack.size)
        assertEquals(HistoryNavKey, navState.currentDestination)
    }

    // ------------------------------------------------------------------------
    // 9. Compact Navigation Shell (< 600dp)
    // ------------------------------------------------------------------------
    @Test
    fun testWindowWidthSizeClass_compactThreshold() {
        val sizes = listOf(0.dp, 320.dp, 390.dp, 412.dp, 599.dp, 599.9.dp)
        sizes.forEach { width ->
            val sizeClass = WindowWidthSizeClass.fromWidth(width)
            assertEquals("Width $width should be COMPACT", WindowWidthSizeClass.COMPACT, sizeClass)
            assertTrue("Width $width should report isCompact == true", sizeClass.isCompact)
            assertFalse("Width $width should report isMediumOrExpanded == false", sizeClass.isMediumOrExpanded)
        }
    }

    // ------------------------------------------------------------------------
    // 10. Expanded Navigation Shell (>= 600dp)
    // ------------------------------------------------------------------------
    @Test
    fun testWindowWidthSizeClass_mediumAndExpandedThresholds() {
        val mediumSizes = listOf(600.dp, 720.dp, 800.dp, 839.dp)
        mediumSizes.forEach { width ->
            val sizeClass = WindowWidthSizeClass.fromWidth(width)
            assertEquals("Width $width should be MEDIUM", WindowWidthSizeClass.MEDIUM, sizeClass)
            assertFalse("Width $width should report isCompact == false", sizeClass.isCompact)
            assertTrue("Width $width should report isMediumOrExpanded == true", sizeClass.isMediumOrExpanded)
        }

        val expandedSizes = listOf(840.dp, 1024.dp, 1280.dp, 1920.dp)
        expandedSizes.forEach { width ->
            val sizeClass = WindowWidthSizeClass.fromWidth(width)
            assertEquals("Width $width should be EXPANDED", WindowWidthSizeClass.EXPANDED, sizeClass)
            assertFalse("Width $width should report isCompact == false", sizeClass.isCompact)
            assertTrue("Width $width should report isMediumOrExpanded == true", sizeClass.isMediumOrExpanded)
        }
    }

    // ------------------------------------------------------------------------
    // 11. Accessibility & Visual Metadata
    // ------------------------------------------------------------------------
    @Test
    fun testNavKeysAccessibility_allKeysHaveValidMetadataAndIcons() {
        TOP_LEVEL_NAV_KEYS.forEach { key ->
            assertTrue("Key ${key::class.simpleName} must have non-empty title", key.title.isNotBlank())
            assertTrue("Key ${key::class.simpleName} must have non-empty contentDescription", key.contentDescription.isNotBlank())
            assertNotNull("Key ${key::class.simpleName} must have valid ImageVector icon", key.icon())
        }
    }

    // ------------------------------------------------------------------------
    // 12. Child Sub-route Push & Back Handling
    // ------------------------------------------------------------------------
    @Test
    fun testPushChildRoute_increasesStackAndPopsCleanly() {
        val backStack = mutableListOf<NavKey>(HomeNavKey)
        val navState = VardiyaNavigationState(backStack)

        navState.navigateTo(SettingsNavKey)
        assertEquals(2, navState.backStack.size)

        // Simulate pushing a child detail route
        navState.push(AnalyticsNavKey)
        assertEquals(3, navState.backStack.size)
        assertEquals(AnalyticsNavKey, navState.currentDestination)

        // First back pops to Settings
        val back1 = navState.handleBack()
        assertTrue(back1)
        assertEquals(SettingsNavKey, navState.currentDestination)
        assertEquals(2, navState.backStack.size)

        // Second back pops to Home
        val back2 = navState.handleBack()
        assertTrue(back2)
        assertEquals(HomeNavKey, navState.currentDestination)
        assertEquals(1, navState.backStack.size)
    }
}
