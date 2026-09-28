package com.example.androidapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive Color Palette for Vardiya.
 * Fully compatible with Material Design 3 and Google HCT Color System.
 * Ensures WCAG AA compliance (contrast ratio >= 4.5:1) for all foreground/background pairings.
 */

// Legacy template aliases for backward compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ----------------------------------------------------------------------------
// Light Theme Tokens (Material 3 Expressive Baseline)
// ----------------------------------------------------------------------------
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)
val InversePrimaryLight = Color(0xFFD0BCFF)

val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val BackgroundLight = Color(0xFFFEF7FF)
val OnBackgroundLight = Color(0xFF1D1B20)

val SurfaceLight = Color(0xFFFEF7FF)
val OnSurfaceLight = Color(0xFF1D1B20)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)

// Material 3 Expressive Surface Container hierarchy (Light mode: decreasing luminance)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F2FA)
val SurfaceContainerLight = Color(0xFFF3EDF7)
val SurfaceContainerHighLight = Color(0xFFECE6F0)
val SurfaceContainerHighestLight = Color(0xFFE6E0E9)

val SurfaceDimLight = Color(0xFFDED8E1)
val SurfaceBrightLight = Color(0xFFFEF7FF)

val InverseSurfaceLight = Color(0xFF322F35)
val InverseOnSurfaceLight = Color(0xFFF5EFF7)

val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)
val ScrimLight = Color(0xFF000000)

// ----------------------------------------------------------------------------
// Dark Theme Tokens (Material 3 Expressive Baseline)
// ----------------------------------------------------------------------------
val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)
val InversePrimaryDark = Color(0xFF6750A4)

val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val BackgroundDark = Color(0xFF141218)
val OnBackgroundDark = Color(0xFFE6E0E9)

val SurfaceDark = Color(0xFF141218)
val OnSurfaceDark = Color(0xFFE6E0E9)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

// Material 3 Expressive Surface Container hierarchy (Dark mode: increasing luminance)
val SurfaceContainerLowestDark = Color(0xFF0F0D13)
val SurfaceContainerLowDark = Color(0xFF1D1B20)
val SurfaceContainerDark = Color(0xFF211F26)
val SurfaceContainerHighDark = Color(0xFF2B2930)
val SurfaceContainerHighestDark = Color(0xFF36343B)

val SurfaceDimDark = Color(0xFF141218)
val SurfaceBrightDark = Color(0xFF3B383E)

val InverseSurfaceDark = Color(0xFFE6E0E9)
val InverseOnSurfaceDark = Color(0xFF322F35)

val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)
val ScrimDark = Color(0xFF000000)
