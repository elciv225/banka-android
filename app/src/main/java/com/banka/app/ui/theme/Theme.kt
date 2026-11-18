package com.banka.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme: ColorScheme = expressiveLightColorScheme().copy(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9FF2CC),
    onPrimaryContainer = Color(0xFF003826),
    secondary = Color(0xFF4C5FFA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1FF),
    onSecondaryContainer = Color(0xFF00105C),
    tertiary = Color(0xFFB3261E),
    background = Color(0xFFF6FBF4),
    surface = Color(0xFFF6FBF4),
    surfaceVariant = Color(0xFFDEE5DA),
    onSurface = Color(0xFF171D19),
    onSurfaceVariant = Color(0xFF414941),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF5BD99A),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005236),
    onPrimaryContainer = Color(0xFF9FF2CC),
    secondary = Color(0xFFBCC2FF),
    onSecondary = Color(0xFF00105C),
    secondaryContainer = Color(0xFF3041C6),
    onSecondaryContainer = Color(0xFFE0E1FF),
    tertiary = Color(0xFFFFB4AB),
    background = Color(0xFF0F1511),
    surface = Color(0xFF0F1511),
    surfaceVariant = Color(0xFF414941),
    onSurface = Color(0xFFDFE4DC),
    onSurfaceVariant = Color(0xFFC1C9BF),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BankaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
