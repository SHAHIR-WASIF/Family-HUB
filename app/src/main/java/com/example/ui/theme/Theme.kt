package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = OnSagePrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = OnSagePrimaryContainer,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaSecondaryContainer,
    onSecondaryContainer = OnTerracottaSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = OnAmberTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = OnAmberTertiaryContainer,
    background = ParchmentBackground,
    onBackground = OnParchmentBackground,
    surface = ParchmentSurface,
    onSurface = OnParchmentSurface,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = OnParchmentSurfaceVariant,
    surfaceContainerLowest = ParchmentSurfaceContainerLowest,
    surfaceContainerLow = ParchmentSurfaceContainerLow,
    surfaceContainer = ParchmentSurfaceContainer,
    surfaceContainerHigh = ParchmentSurfaceContainerHigh,
    surfaceContainerHighest = ParchmentSurfaceContainerHighest,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = OnSagePrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = OnSagePrimaryContainerDark,
    secondary = TerracottaSecondaryDark,
    onSecondary = OnTerracottaSecondaryDark,
    secondaryContainer = TerracottaSecondaryContainerDark,
    onSecondaryContainer = OnTerracottaSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = OnAmberTertiaryContainerDark,
    background = ObsidianBackground,
    onBackground = OnObsidianBackground,
    surface = ObsidianSurface,
    onSurface = OnObsidianSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = OnObsidianSurfaceVariant,
    surfaceContainerLowest = ObsidianSurfaceContainerLowest,
    surfaceContainerLow = ObsidianSurfaceContainerLow,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceContainerHigh,
    surfaceContainerHighest = ObsidianSurfaceContainerHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

val FamilyHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FamilyHubTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FamilyHubTypography,
        shapes = FamilyHubShapes,
        content = content
    )
}
