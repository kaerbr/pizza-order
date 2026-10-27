package io.github.kaerbr.pizzaorder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// "Forno 12" design (design/index.html): one token set per palette × brightness,
// mapped onto Material roles so every screen (incl. Settings) picks it up.
private fun scheme(
    dark: Boolean,
    bg: Long, surface: Long, surface2: Long, ink: Long, muted: Long, line: Long,
    accent: Long, accentInk: Long, accentSoft: Long,
    danger: Long, dangerSoft: Long, readyBg: Long, readyInk: Long,
): ColorScheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = Color(accent),
    onPrimary = Color(accentInk),
    primaryContainer = Color(accentSoft),
    onPrimaryContainer = Color(ink),
    secondary = Color(accent),
    onSecondary = Color(accentInk),
    secondaryContainer = Color(accentSoft),   // selected chips + nav indicator
    onSecondaryContainer = Color(ink),
    tertiaryContainer = Color(readyBg),
    onTertiaryContainer = Color(readyInk),
    background = Color(bg),
    onBackground = Color(ink),
    surface = Color(surface),
    onSurface = Color(ink),
    surfaceVariant = Color(surface2),
    onSurfaceVariant = Color(muted),
    surfaceContainerLowest = Color(surface),
    surfaceContainerLow = Color(surface),
    surfaceContainer = Color(surface),
    surfaceContainerHigh = Color(surface2),
    surfaceContainerHighest = Color(surface2),
    outline = Color(line),
    outlineVariant = Color(line),
    error = Color(danger),
    onError = Color(accentInk),
    errorContainer = Color(dangerSoft),
    onErrorContainer = Color(danger),
)

enum class Palette(val label: String, val description: String, val light: ColorScheme, val dark: ColorScheme) {
    Forno(
        "Forno", "Terrakotta, Olive, warmes Papier",
        scheme(false, 0xFFEFE7DB, 0xFFFAF6EF, 0xFFF4EDE3, 0xFF2A211C, 0xFF6B5849, 0xFFDCCDBD, 0xFFA63F22, 0xFFFFF7F1, 0xFFF3DCD1, 0xFF982D1C, 0xFFF7E2DB, 0xFFDBE9D4, 0xFF2C5230),
        scheme(true, 0xFF171210, 0xFF211A17, 0xFF2A211D, 0xFFF3EAE0, 0xFFBCA998, 0xFF3A2E27, 0xFFE0764A, 0xFF1C110C, 0xFF3A241B, 0xFFF09A86, 0xFF3A211B, 0xFF1E3324, 0xFFA9D6A4),
    ),
    Basilico(
        "Basilico", "Salbei, Creme, Gartengrün",
        scheme(false, 0xFFE9EDE2, 0xFFF7F8F1, 0xFFEFF2E8, 0xFF202A1F, 0xFF56654F, 0xFFD3D9C9, 0xFF3E5B36, 0xFFF7F8F1, 0xFFDEE7D2, 0xFF8D3323, 0xFFF4E2DA, 0xFFD6E6D0, 0xFF27512B),
        scheme(true, 0xFF10150F, 0xFF181F17, 0xFF20281E, 0xFFE9EFE2, 0xFFA5B49F, 0xFF2C362B, 0xFF9CC07C, 0xFF141C11, 0xFF26331F, 0xFFEB9C87, 0xFF36221C, 0xFF1C3021, 0xFFA5D2A1),
    ),
    Notte(
        "Notte", "Stein, Anthrazit, Glut-Bernstein",
        scheme(false, 0xFFE9E7E2, 0xFFF6F5F1, 0xFFEEEDE8, 0xFF1E1C19, 0xFF5C574F, 0xFFD5D1CA, 0xFF7E4612, 0xFFFFF6EA, 0xFFF1E1CD, 0xFF942F1D, 0xFFF5E1DA, 0xFFDBE8D5, 0xFF2C522E),
        scheme(true, 0xFF121213, 0xFF1C1C1E, 0xFF262628, 0xFFF1EFEB, 0xFFA9A49B, 0xFF33322F, 0xFFE8A25E, 0xFF1A130C, 0xFF33261A, 0xFFEF9B85, 0xFF38231D, 0xFF1F3323, 0xFFA9D6A4),
    ),
}

// ponytail: system serif stands in for Iowan/Palatino; bundle a font in res/font if it matters
private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Serif),
    )
}

@Composable
fun PizzaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: Palette = Palette.Forno,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) palette.dark else palette.light,
        typography = AppTypography,
        content = content,
    )
}
