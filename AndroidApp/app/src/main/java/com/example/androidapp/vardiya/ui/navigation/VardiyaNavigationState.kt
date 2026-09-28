package com.example.androidapp.vardiya.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Standard Material 3 Window Width Size Classes.
 * Breakpoint boundaries:
 * - Compact: < 600dp (standard portrait phones)
 * - Medium: 600dp ..< 840dp (foldables unfolded, small tablets)
 * - Expanded: >= 840dp (large tablets, desktop, horizontal landscape)
 */
enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    val isCompact: Boolean
        get() = this == COMPACT

    val isMediumOrExpanded: Boolean
        get() = this == MEDIUM || this == EXPANDED

    companion object {
        val COMPACT_MAX_WIDTH: Dp = 600.dp
        val MEDIUM_MAX_WIDTH: Dp = 840.dp

        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < COMPACT_MAX_WIDTH -> COMPACT
            width < MEDIUM_MAX_WIDTH -> MEDIUM
            else -> EXPANDED
        }
    }
}

/**
 * Centralized Navigation State Manager for Vardiya 3.0.
 * Operates on Navigation 3's back stack list.
 *
 * Guarantees:
 * 1. Single source of truth for the active destination.
 * 2. HomeNavKey always acts as the root destination.
 * 3. Tab switching retains a bounded backstack ([HomeNavKey] or [HomeNavKey, TabKey]).
 * 4. Back navigation pops back to Home before app exit.
 * 5. State survives configuration changes and process recreation via rememberNavBackStack.
 */
@Stable
class VardiyaNavigationState(
    val backStack: MutableList<NavKey>
) {
    val currentDestination: VardiyaNavKey
        get() = (backStack.lastOrNull() as? VardiyaNavKey) ?: HomeNavKey

    /**
     * Navigates to a top-level destination.
     * Prevents duplicate instances of the same tab on top of the stack.
     */
    fun navigateTo(destination: VardiyaNavKey) {
        if (currentDestination == destination) return

        if (destination == HomeNavKey) {
            // Return to root Home destination
            while (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        } else {
            // Keep Home as base root, replace current non-home tab
            while (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
            backStack.add(destination)
        }
    }

    /**
     * Intercepts back navigation.
     * Returns true if a destination was popped, false if already at root.
     */
    fun handleBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    /**
     * Pushes a child destination (for future sub-routes or detail scenes).
     */
    fun push(key: NavKey) {
        backStack.add(key)
    }
}

/**
 * Creates and remembers a VardiyaNavigationState backed by rememberNavBackStack.
 */
@Composable
fun rememberVardiyaNavigationState(
    startDestination: VardiyaNavKey = HomeNavKey
): VardiyaNavigationState {
    val backStack = rememberNavBackStack(startDestination)
    return remember(backStack) {
        VardiyaNavigationState(backStack)
    }
}
