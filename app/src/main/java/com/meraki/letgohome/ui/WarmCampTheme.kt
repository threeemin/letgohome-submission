package com.meraki.letgohome.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val CampCanvas = Color(0xFFF6F3F2)
internal val CampSand = Color(0xFFE4DCD3)
internal val CampCard = Color.White
internal val CampBlue = Color(0xFF002C5F)
internal val CampGold = Color(0xFFA36B4F)

// A separate state color keeps warnings recognizable on the light surfaces.
internal val CampWarning = Color(0xFF882E1C)
internal val CampWarningSurface = Color(0xFFF9E6DF)

internal val WarmCampColors = lightColorScheme(
    primary = CampBlue,
    onPrimary = CampCard,
    primaryContainer = CampSand,
    onPrimaryContainer = CampBlue,
    secondary = CampBlue,
    onSecondary = CampCard,
    secondaryContainer = CampSand,
    onSecondaryContainer = CampBlue,
    background = CampCanvas,
    onBackground = CampBlue,
    surface = CampCard,
    onSurface = CampBlue,
    onSurfaceVariant = CampBlue,
    surfaceVariant = CampSand,
    surfaceContainer = CampCanvas,
    surfaceContainerHigh = CampSand,
    outline = Color(0xFF75695E),
    outlineVariant = CampSand,
    error = CampWarning,
    onError = CampCard,
    errorContainer = CampWarningSurface,
    onErrorContainer = CampWarning,
)

internal val WarmCampShapes = Shapes(
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)
