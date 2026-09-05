package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Boss LifeOS Palette
val ObsidianBg = Color(0xFF0B1120)
val SlateSurface = Color(0xFF0F172A)
val SlateCard = Color(0xFF1E293B)
val SlateCardElevated = Color(0xFF283548)
val SlateBorder = Color(0xFF334155)

val ElectricSky = Color(0xFF38BDF8)
val ElectricSkyVariant = Color(0xFF0284C7)
val AmberGold = Color(0xFFF59E0B)
val AmberGoldDark = Color(0xFFB45309)
val EmeraldVerified = Color(0xFF10B981)
val CrimsonUrgent = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val DarkColorSchemeColors = androidx.compose.material3.darkColorScheme(
    primary = ElectricSky,
    onPrimary = ObsidianBg,
    primaryContainer = SlateCardElevated,
    onPrimaryContainer = ElectricSky,
    secondary = AmberGold,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0xFF2D2415),
    onSecondaryContainer = AmberGold,
    tertiary = EmeraldVerified,
    onTertiary = ObsidianBg,
    error = CrimsonUrgent,
    onError = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder
)
