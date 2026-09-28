package com.example.androidapp.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shape scale for Vardiya.
 * Standard scale follows official Material Design 3 specifications:
 * - extraSmall: 4dp (menus, snackbars, small chips)
 * - small: 8dp (chips, text fields)
 * - medium: 12dp (cards, dialogs)
 * - large: 16dp (standard cards, floating sheets, list containers)
 * - extraLarge: 28dp (large containers, prominent modal dialogs)
 *
 * Expressive Shape Tokens:
 * - PillShape: 50% rounded (action buttons, status badges)
 * - SquircleCardShape: 24dp (fluid expressive cards)
 * - ExpressiveHeroShape: 28dp (hero progress background and large displays)
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val PillShape = RoundedCornerShape(percent = 50)
val SquircleCardShape = RoundedCornerShape(24.dp)
val ExpressiveHeroShape = RoundedCornerShape(28.dp)
val FullCircleShape = CircleShape
