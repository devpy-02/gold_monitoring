package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = CharcoalBackground,
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = OnGoldContainerDark,
    secondary = SilverPrimary,
    onSecondary = CharcoalBackground,
    secondaryContainer = SilverContainerDark,
    onSecondaryContainer = OnSilverContainerDark,
    tertiary = GoldLight,
    onTertiary = CharcoalBackground,
    background = CharcoalBackground,
    onBackground = CharcoalOnSurface,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant,
    outline = CharcoalBorder
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = WarmLightSurface,
    primaryContainer = GoldContainerLight,
    onPrimaryContainer = OnGoldContainerLight,
    secondary = SilverDark,
    onSecondary = WarmLightSurface,
    secondaryContainer = SilverContainerLight,
    onSecondaryContainer = OnSilverContainerLight,
    tertiary = GoldPrimary,
    onTertiary = WarmLightSurface,
    background = WarmLightBackground,
    onBackground = WarmLightOnSurface,
    surface = WarmLightSurface,
    onSurface = WarmLightOnSurface,
    surfaceVariant = WarmLightSurfaceVariant,
    onSurfaceVariant = WarmLightOnSurfaceVariant,
    outline = WarmLightBorder
)

@Composable
fun ZarNegarTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Mandatory RTL support for Persian UI across the entire application
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
