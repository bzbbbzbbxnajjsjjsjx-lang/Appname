package com.example.androidapp.vardiya.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.androidapp.theme.VardiyaIcons
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Keys for Vardiya 3.0.
 * Compatible with Navigation 3 runtime and Kotlinx Serialization.
 */
sealed interface VardiyaNavKey : NavKey {
    val title: String
    val contentDescription: String
}

@Serializable
data object HomeNavKey : VardiyaNavKey {
    override val title: String = "Ana Sayfa"
    override val contentDescription: String = "Ana Sayfa sekmesi"
}

@Serializable
data object HistoryNavKey : VardiyaNavKey {
    override val title: String = "Geçmiş"
    override val contentDescription: String = "Geçmiş sekmesi"
}

@Serializable
data object AnalyticsNavKey : VardiyaNavKey {
    override val title: String = "Analitik"
    override val contentDescription: String = "Analitik sekmesi"
}

@Serializable
data object SettingsNavKey : VardiyaNavKey {
    override val title: String = "Ayarlar"
    override val contentDescription: String = "Ayarlar sekmesi"
}

/**
 * Maps each destination to its corresponding Material 3 icon.
 * Extension function keeps ImageVector out of Kotlinx Serialization scope.
 */
fun VardiyaNavKey.icon(): ImageVector = when (this) {
    HomeNavKey -> VardiyaIcons.Home
    HistoryNavKey -> VardiyaIcons.History
    AnalyticsNavKey -> VardiyaIcons.Analytics
    SettingsNavKey -> VardiyaIcons.Settings
}

/**
 * The four primary destinations in Vardiya 3.0 top-level navigation.
 */
val TOP_LEVEL_NAV_KEYS: List<VardiyaNavKey> = listOf(
    HomeNavKey,
    HistoryNavKey,
    AnalyticsNavKey,
    SettingsNavKey
)
