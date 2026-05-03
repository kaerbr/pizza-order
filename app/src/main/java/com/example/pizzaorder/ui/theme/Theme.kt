package com.example.pizzaorder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Neapolitana / Pompeii frescoes: dark fired-clay primary, olive-stone
// secondary, sun-bleached parchment background.
private val LightScheme = lightColorScheme(
    primary             = Color(0xFF8B4A2B),  // fired clay — dark brown, faint red echo
    onPrimary           = Color(0xFFFFF6EE),
    primaryContainer    = Color(0xFFEFD3BD),  // unfired clay / dough
    onPrimaryContainer  = Color(0xFF2E1503),
    secondary           = Color(0xFF4D5C4D),  // olive-stone — quiet, warm-leaning gray-green
    onSecondary         = Color(0xFFEAEEE7),
    secondaryContainer  = Color(0xFFD2D8CD),  // weathered stone
    onSecondaryContainer= Color(0xFF22302A),
    tertiary            = Color(0xFFB8902F),  // amalfi-lemon zest, used sparingly
    onTertiary          = Color(0xFFFFF6EE),
    background          = Color(0xFFFAF6EC),  // sun-bleached Naples plaster
    onBackground        = Color(0xFF1F1A14),
    surface             = Color(0xFFFAF6EC),
    onSurface           = Color(0xFF1F1A14),
    surfaceVariant      = Color(0xFFF1E8D4),  // lightly toasted crust
    onSurfaceVariant    = Color(0xFF5A5040),
    outline             = Color(0xFF8E8467),  // weathered wood / stone edge
    error               = Color(0xFF6E2A19),  // burnt-edge brown, even quieter than primary
    onError             = Color(0xFFFFF6EE),
)

private val DarkScheme = darkColorScheme(
    primary             = Color(0xFFE0A57F),  // glowing kiln clay
    onPrimary           = Color(0xFF3F1A06),
    primaryContainer    = Color(0xFF5A2D14),  // deep ember
    onPrimaryContainer  = Color(0xFFF2D5BD),
    secondary           = Color(0xFFB6C2B2),  // moonlit olive-stone
    onSecondary         = Color(0xFF22302A),
    secondaryContainer  = Color(0xFF364437),
    onSecondaryContainer= Color(0xFFCDD5C8),
    tertiary            = Color(0xFFE8C66F),
    onTertiary          = Color(0xFF3D2C00),
    background          = Color(0xFF1A1814),  // Vesuvius basalt at night
    onBackground        = Color(0xFFEFE7D6),
    surface             = Color(0xFF1A1814),
    onSurface           = Color(0xFFEFE7D6),
    surfaceVariant      = Color(0xFF3F3A2E),
    onSurfaceVariant    = Color(0xFFBFB39A),
    outline             = Color(0xFF89806A),
    error               = Color(0xFFFFB4A6),
    onError             = Color(0xFF5A0E07),
)

/**
 * Dynamic color (Material You) on Android 12+ unless explicitly disabled.
 * Falls back to our defined warm palette below on older devices.
 */
@Composable
fun PizzaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,   // false = always use our warm palette
    content: @Composable () -> Unit,
) {
    val scheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else      -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}