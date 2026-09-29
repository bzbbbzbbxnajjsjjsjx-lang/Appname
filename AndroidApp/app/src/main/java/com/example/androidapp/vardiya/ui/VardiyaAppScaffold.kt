package com.example.androidapp.vardiya.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.vardiya.data.repository.LocalVardiyaRepository
import com.example.androidapp.vardiya.ui.navigation.AnalyticsNavKey
import com.example.androidapp.vardiya.ui.navigation.HistoryNavKey
import com.example.androidapp.vardiya.ui.navigation.HomeNavKey
import com.example.androidapp.vardiya.ui.navigation.SettingsNavKey
import com.example.androidapp.vardiya.ui.navigation.TOP_LEVEL_NAV_KEYS
import com.example.androidapp.vardiya.ui.navigation.VardiyaNavKey
import com.example.androidapp.vardiya.ui.navigation.VardiyaNavigationState
import com.example.androidapp.vardiya.ui.navigation.WindowWidthSizeClass
import com.example.androidapp.vardiya.ui.navigation.icon
import com.example.androidapp.vardiya.ui.navigation.rememberVardiyaNavigationState
import com.example.androidapp.vardiya.ui.screens.VardiyaAnalyticsScreen
import com.example.androidapp.vardiya.ui.screens.VardiyaHistoryScreen
import com.example.androidapp.vardiya.ui.screens.VardiyaSettingsScreen

/**
 * Modern Material 3 Expressive Adaptive Navigation Shell for Vardiya 3.0.
 *
 * Responsiveness:
 * - Compact screens (< 600dp): Material 3 Bottom NavigationBar
 * - Medium / Expanded screens (>= 600dp): Material 3 Leading NavigationRail
 *
 * Navigation 3 Architecture:
 * - Backed by rememberNavBackStack with HomeNavKey as root
 * - Centralized VardiyaNavigationState handles tab switching and back navigation
 * - Single shared VardiyaViewModel instance across all top-level destinations
 */
@Composable
fun VardiyaAppScaffold(
    modifier: Modifier = Modifier,
    viewModel: VardiyaViewModel = run {
        val context = LocalContext.current
        viewModel { VardiyaViewModel(LocalVardiyaRepository(context)) }
    },
    navState: VardiyaNavigationState = rememberVardiyaNavigationState(HomeNavKey),
    onNavigateToCalculator: (() -> Unit)? = null
) {
    // Intercept back navigation when on a secondary tab
    BackHandler(enabled = navState.backStack.size > 1) {
        navState.handleBack()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = WindowWidthSizeClass.fromWidth(maxWidth)

        if (sizeClass.isCompact) {
            // Compact layout: Bottom NavigationBar
            Scaffold(
                bottomBar = {
                    VardiyaNavigationBar(
                        currentDestination = navState.currentDestination,
                        onSelectDestination = { navState.navigateTo(it) }
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    VardiyaNavDisplay(
                        navState = navState,
                        viewModel = viewModel,
                        onNavigateToCalculator = onNavigateToCalculator
                    )
                }
            }
        } else {
            // Medium/Expanded layout: Leading NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                VardiyaNavigationRail(
                    currentDestination = navState.currentDestination,
                    onSelectDestination = { navState.navigateTo(it) }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    VardiyaNavDisplay(
                        navState = navState,
                        viewModel = viewModel,
                        onNavigateToCalculator = onNavigateToCalculator
                    )
                }
            }
        }
    }
}

/**
 * Navigation 3 Expressive Motion Transitions for Vardiya.
 *
 * Physics principles:
 * - Forward Navigation: Subtle 8% spatial offset paired with critically damped effects fade.
 *   Avoids disorienting full-screen carousels while clearly communicating hierarchy shift.
 * - Pop/Back Navigation: Symmetrical reverse 8% spatial offset paired with effects fade.
 * - Reduced Motion: Immediate zero-offset fade without spatial movement.
 */
object VardiyaNavTransitions {
    const val SubtleSpatialOffsetFactor = 0.08f

    fun createForwardTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform {
        return if (motionPreference == MotionPreference.REDUCED) {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            (fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { (it * SubtleSpatialOffsetFactor).toInt() }
                )) togetherWith
                (fadeOut(animationSpec = motionScheme.defaultEffectsSpec()) +
                    slideOutHorizontally(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        targetOffsetX = { (-it * SubtleSpatialOffsetFactor).toInt() }
                    ))
        }
    }

    fun createPopTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform {
        return if (motionPreference == MotionPreference.REDUCED) {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            (fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { (-it * SubtleSpatialOffsetFactor).toInt() }
                )) togetherWith
                (fadeOut(animationSpec = motionScheme.defaultEffectsSpec()) +
                    slideOutHorizontally(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        targetOffsetX = { (it * SubtleSpatialOffsetFactor).toInt() }
                    ))
        }
    }
}

/**
 * Navigation 3 Display container routing all Vardiya 3.0 destinations
 * with Material 3 Expressive screen transitions.
 */
@Composable
private fun VardiyaNavDisplay(
    navState: VardiyaNavigationState,
    viewModel: VardiyaViewModel,
    onNavigateToCalculator: (() -> Unit)?
) {
    val motionScheme = VardiyaTheme.motionScheme
    val motionPreference = VardiyaTheme.motionPreference

    NavDisplay(
        backStack = navState.backStack,
        onBack = { navState.handleBack() },
        transitionSpec = {
            VardiyaNavTransitions.createForwardTransition(motionScheme, motionPreference)
        },
        popTransitionSpec = {
            VardiyaNavTransitions.createPopTransition(motionScheme, motionPreference)
        },
        entryProvider = entryProvider {
            entry<HomeNavKey> {
                VardiyaScreen(
                    viewModel = viewModel,
                    onNavigateToCalculator = onNavigateToCalculator
                )
            }
            entry<HistoryNavKey> {
                VardiyaHistoryScreen(
                    viewModel = viewModel,
                    onBack = { navState.handleBack() }
                )
            }
            entry<AnalyticsNavKey> {
                VardiyaAnalyticsScreen(
                    viewModel = viewModel,
                    onBack = { navState.handleBack() }
                )
            }
            entry<SettingsNavKey> {
                VardiyaSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navState.handleBack() }
                )
            }
        }
    )
}

/**
 * Material 3 Expressive Bottom NavigationBar for Compact screens.
 */
@Composable
fun VardiyaNavigationBar(
    currentDestination: VardiyaNavKey,
    onSelectDestination: (VardiyaNavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        TOP_LEVEL_NAV_KEYS.forEach { destination ->
            val isSelected = destination == currentDestination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = destination.contentDescription
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = destination.contentDescription
                }
            )
        }
    }
}

/**
 * Material 3 Expressive NavigationRail for Medium/Expanded screens.
 */
@Composable
fun VardiyaNavigationRail(
    currentDestination: VardiyaNavKey,
    onSelectDestination: (VardiyaNavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        header = {
            Text(
                text = "Vardiya",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    ) {
        TOP_LEVEL_NAV_KEYS.forEach { destination ->
            val isSelected = destination == currentDestination
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = destination.contentDescription
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = destination.contentDescription
                }
            )
        }
    }
}
