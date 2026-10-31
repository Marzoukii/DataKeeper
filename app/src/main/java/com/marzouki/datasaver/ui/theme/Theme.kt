package com.marzouki.datasaver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Le secondaire reprend la menthe : chips, onglets et indicateurs restent dans la palette Wi-Fi.
internal val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightPrimary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightPrimaryContainer,
    onSecondaryContainer = LightOnPrimaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightPrimary,
    surfaceBright = LightSurface,
    surfaceDim = LightSurfaceVariant,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

internal val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkPrimary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkPrimaryContainer,
    onSecondaryContainer = DarkOnPrimaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    surfaceBright = DarkSurfaceVariant,
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

/** Couleurs hors du schéma Material 3. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    /** Fond des cartes en verre dépoli : translucide, les halos du fond passent au travers. */
    val glass: Color,
    /** Liseré clair du verre. */
    val glassBorder: Color,
    /** Halos colorés peints derrière le contenu. */
    val glowStrength: Float,
)

private val LightExtended = ExtendedColors(
    success = LightSuccess,
    successContainer = LightSuccessContainer,
    glass = Color.White.copy(alpha = 0.70f),
    glassBorder = Color.White.copy(alpha = 0.85f),
    glowStrength = 0.30f,
)

private val DarkExtended = ExtendedColors(
    success = DarkSuccess,
    successContainer = DarkSuccessContainer,
    glass = DarkSurface.copy(alpha = 0.72f),
    glassBorder = Color.White.copy(alpha = 0.10f),
    glowStrength = 0.22f,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

/** Alias sémantiques : on parle de Wi-Fi et de mobile, jamais de « primary » ou « tertiary ». */
object DataColors {
    val wifi: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val mobile: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
    val success: Color
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current.success
    val successContainer: Color
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current.successContainer
    val today: Color
        @Composable @ReadOnlyComposable get() = Brand.Saffron
    val todayTint: Color
        @Composable @ReadOnlyComposable get() = Brand.SaffronTint
}

/** Pas de couleurs dynamiques (Android 12+) : la menthe, le corail et le safran portent un sens. */
@Composable
fun DataKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extendedColors = if (darkTheme) DarkExtended else LightExtended
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            // De droite à gauche, c'est-à-dire en arabe : pas d'espacement des lettres (voir RtlTypography).
            typography = if (LocalLayoutDirection.current == LayoutDirection.Rtl) RtlTypography else Typography,
            shapes = DataKeeperShapes,
            content = content,
        )
    }
}
