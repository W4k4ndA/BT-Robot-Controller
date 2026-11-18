package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RoboticsDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = BrushedTitanium,
    onPrimaryContainer = NeonCyan,
    secondary = NeonEmerald,
    onSecondary = ObsidianVoid,
    secondaryContainer = GunmetalChassis,
    onSecondaryContainer = NeonEmerald,
    tertiary = NeonMagenta,
    onTertiary = ObsidianVoid,
    tertiaryContainer = AlloyHighlight,
    onTertiaryContainer = NeonMagenta,
    error = NeonRed,
    onError = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = CarbonBase,
    onSurface = TextPrimary,
    surfaceVariant = GunmetalChassis,
    onSurfaceVariant = TextSecondary,
    outline = SteelBevel,
    outlineVariant = AlloyHighlight
)

@Composable
fun RoverLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoboticsDarkColorScheme,
        typography = RoverTypography,
        content = content
    )
}
