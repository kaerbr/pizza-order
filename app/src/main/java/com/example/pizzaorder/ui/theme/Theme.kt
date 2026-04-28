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

// Wood-fired-oven warm: tomato red as primary, soft cream as background.
private val LightScheme = lightColorScheme(
    primary           = Color(0xFFB23A3A),
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410001),
    secondary         = Color(0xFF775656),
    onSecondary       = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    background        = Color(0xFFFFF8F6),
    onBackground      = Color(0xFF201A19),
    surface           = Color(0xFFFFF8F6),
    onSurface         = Color(0xFF201A19),
    surfaceVariant    = Color(0xFFF5DDDA),
    onSurfaceVariant  = Color(0xFF534341),
    outline           = Color(0xFF857371),
    error             = Color(0xFFBA1A1A),
)

private val DarkScheme = darkColorScheme(
    primary           = Color(0xFFFFB3AC),
    onPrimary         = Color(0xFF680010),
    primaryContainer  = Color(0xFF8E2025),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary         = Color(0xFFE6BDBC),
    background        = Color(0xFF201A19),
    onBackground      = Color(0xFFEDE0DE),
    surface           = Color(0xFF201A19),
    onSurface         = Color(0xFFEDE0DE),
    surfaceVariant    = Color(0xFF534341),
    onSurfaceVariant  = Color(0xFFD8C2BF),
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
