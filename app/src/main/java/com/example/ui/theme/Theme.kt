package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect

private val LightColorScheme = lightColorScheme(
    primary = DozumPrimary,
    onPrimary = DozumOnPrimary,
    primaryContainer = DozumPrimaryContainer,
    onPrimaryContainer = DozumOnPrimaryContainer,
    secondary = DozumSecondary,
    onSecondary = Color.White,
    secondaryContainer = DozumSecondaryContainer,
    onSecondaryContainer = DozumOnSecondaryContainer,
    tertiary = DozumTertiary,
    onTertiary = DozumOnTertiary,
    tertiaryContainer = DozumTertiaryContainer,
    onTertiaryContainer = DozumTertiary,
    background = DozumBackground,
    onBackground = DozumOnBackground,
    surface = Color.White,
    onSurface = DozumOnSurface,
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = DozumOnSurfaceVariant,
    outline = DozumOutline,
    outlineVariant = DozumOutlineVariant,
    error = DozumError,
    onError = DozumOnError,
    errorContainer = DozumErrorContainer,
    onErrorContainer = DozumOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FDEC8),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF89F5E7),
    secondary = Color(0xFF6BD8CB),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004F46),
    onSecondaryContainer = Color(0xFF71F8E4),
    tertiary = Color(0xFF4EDE9E),
    onTertiary = Color(0xFF003923),
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFF6FFBBE),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun DozumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
