package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CleanLightColorScheme = lightColorScheme(
    primary = CleanPrimary,
    onPrimary = CleanOnPrimary,
    primaryContainer = CleanPrimaryContainer,
    onPrimaryContainer = CleanOnPrimaryContainer,
    secondary = CleanSecondary,
    onSecondary = CleanOnSecondary,
    secondaryContainer = CleanSecondaryContainer,
    onSecondaryContainer = CleanOnSecondaryContainer,
    background = CleanBackground,
    onBackground = CleanOnBackground,
    surface = CleanSurface,
    onSurface = CleanOnSurface,
    surfaceVariant = CleanSurfaceVariant,
    onSurfaceVariant = CleanOnSurfaceVariant,
    outline = CleanOutline,
    outlineVariant = CleanOutlineVariant,
    error = CleanError
)

private val CleanDarkColorScheme = darkColorScheme(
    primary = CleanSecondary,
    onPrimary = CleanOnPrimary,
    primaryContainer = CleanOnSecondaryContainer,
    onPrimaryContainer = CleanPrimaryContainer,
    secondary = CleanPrimary,
    onSecondary = CleanOnSecondary,
    secondaryContainer = CleanOnBackground,
    onSecondaryContainer = CleanSurfaceVariant,
    background = CleanOnBackground,
    onBackground = CleanBackground,
    surface = CleanOnSurface,
    onSurface = CleanBackground,
    surfaceVariant = CleanOnSurface,
    onSurfaceVariant = CleanOutline,
    outline = CleanOutlineVariant,
    outlineVariant = CleanOnBackground,
    error = CleanError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to ensure our Clean Minimalism theme takes full effect!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CleanDarkColorScheme else CleanLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
